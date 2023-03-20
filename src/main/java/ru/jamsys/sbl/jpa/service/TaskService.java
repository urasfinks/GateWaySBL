package ru.jamsys.sbl.jpa.service;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import ru.jamsys.sbl.SblApplication;
import ru.jamsys.sbl.Util;
import ru.jamsys.sbl.WrapJsonToObject;
import ru.jamsys.sbl.jpa.dto.*;
import ru.jamsys.sbl.jpa.repo.*;
import ru.jamsys.sbl.message.Message;
import ru.jamsys.sbl.message.MessageImpl;
import ru.jamsys.sbl.web.GreetingClient;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class TaskService {

    @PersistenceContext
    private EntityManager em;

    protected <T> T saveWithoutCache(CrudRepository<T, Long> crudRepository, T entity) {
        return SblApplication.saveWithoutCache(em, crudRepository, entity);
    }

    GreetingClient greetingClient;
    VirtualServerRepo virtualServerRepo;
    TaskRepo taskRepo;
    ServerRepo serverRepo;
    RouterRepo routerRepo;
    TaskStatusRepo taskStatusRepo;

    @Autowired
    public void setTaskStatusRepo(TaskStatusRepo taskStatusRepo) {
        this.taskStatusRepo = taskStatusRepo;
    }

    @Autowired
    public void setRouterRepo(RouterRepo routerRepo) {
        this.routerRepo = routerRepo;
    }

    @Autowired
    public void setGreetingClient(GreetingClient greetingClient) {
        this.greetingClient = greetingClient;
    }

    @Autowired
    public void setServerRepo(ServerRepo serverRepo) {
        this.serverRepo = serverRepo;
    }

    @Autowired
    public void setTaskRepo(TaskRepo taskRepo) {
        this.taskRepo = taskRepo;
    }

    @Autowired
    public void setVirtualServerRepo(VirtualServerRepo virtualServerRepo) {
        this.virtualServerRepo = virtualServerRepo;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Message exec() {
        Message ret = null;
        synchronized (SblApplication.class) {
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            List<TaskDTO> listTask = taskRepo.getAlready(timestamp);
            if (listTask.size() > 0) {
                TaskDTO task = taskRepo.lock(listTask.get(0).getId());
                if (task != null && task.getStatus() == 0) {
                    Util.logConsole(Thread.currentThread(), "::execOneTask start: " + task.getId());

                    Integer retry = task.getRetry();
                    if (retry == null) {
                        retry = 0;
                    }
                    if (retry < task.getRetryMax()) {
                        try {
                            task.setResult("Start exec: " + LocalDateTime.now()); //Если взяли в работу, то от предыдущего раза очистим результат
                            execTask(task);
                        } catch (Exception e) {
                            e.printStackTrace();
                            task.incRetry();
                            status("ERROR", task, Util.stackTraceToString(e));
                        }
                    } else {
                        task.setStatus(-1);
                        status("ERROR", task, "Max retry");
                    }

                    task.setDateUpdate(new Timestamp(System.currentTimeMillis()));
                    saveWithoutCache(taskRepo, task);
                    ret = new MessageImpl();
                }
            }

            List<VirtualServerDTO> removedVSrv = virtualServerRepo.getRemove(timestamp);
            for (VirtualServerDTO virtualServerDTO : removedVSrv) {

                virtualServerDTO.setStatus(-2);
                saveWithoutCache(virtualServerRepo, virtualServerDTO);
                System.out.println("NEED REMOVE " + virtualServerDTO);
                TaskDTO removeTask = new TaskDTO();
                Map<String, Object> dataRemoveTask = new HashMap<>();
                dataRemoveTask.put("action", "ControlVM");
                dataRemoveTask.put("command", "remove");
                dataRemoveTask.put("name", virtualServerDTO.getIso() + "_" + virtualServerDTO.getId());

                removeTask.setIdClient(virtualServerDTO.getIdClient());
                removeTask.setLinkIdVSrv(virtualServerDTO.getId());
                removeTask.setLinkIdSrv(virtualServerDTO.getIdSrv());

                removeTask.setTask(Util.jsonObjectToString(dataRemoveTask));
                System.out.println(removeTask);
                saveWithoutCache(taskRepo, removeTask);
            }

        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    public void execTask(TaskDTO task) {
        Map<String, Object> parsed = new Gson().fromJson(task.getTask(), Map.class);
        if (parsed.get("action") != null) {
            String action = (String) parsed.get("action");
            if (action.equals("CreateVM")) {
                actionCreateVM(task, parsed);
            } else if (action.equals("ControlVM")) {
                actionControlVM(task, parsed);
            } else {
                task.setStatus(-1);
                status("ERROR", task, "Action not define");
            }
        }
    }

    public void actionControlVM(TaskDTO task, Map<String, Object> parsed) {
        boolean next = true;

        if (next) {
            if (!parsed.containsKey("name")) {
                taskError(task, "Field name undefined");
                next = false;
            }
        }

        if (next) {
            if (!parsed.containsKey("command")) {
                taskError(task, "Field command undefined");
                next = false;
            }
        }

        String name = null;

        VirtualServerDTO virtualServerDTO = null;
        if (next) {
            name = (String) parsed.get("name");
            String[] partsOfName = name.split("_");

            virtualServerDTO = virtualServerRepo.findById(Long.parseLong(partsOfName[partsOfName.length - 1])).orElse(null);
            if (virtualServerDTO == null) {
                taskError(task, "VirtualServer not found by name: " + name);
                next = false;
            }
        }

        ServerDTO serverDTO = null;

        if (next) {
            serverDTO = serverRepo.findById(virtualServerDTO.getIdSrv()).orElse(null);
            if (serverDTO == null) {
                taskError(task, "Server not found by VM name: " + name);
                next = false;
            }
        }

        if (next) {
            if (serverDTO.getStatus() != 0) {
                taskFuture(task, "Server busy; Status: " + serverDTO.getStatus());
                next = false;
            }
        }

        if (next) {
            if (serverDTO.getPingStatus() != 1) {
                taskFuture(task, "Server not available; Status: " + serverDTO.getStatus());
                next = false;
            }
        }

        boolean lockServer = false;
        boolean serverBusy = false;

        if (next) {
            lockServer(serverDTO, task);
            lockServer = true;
            task.setLinkIdVSrv(virtualServerDTO.getId());
            task.setLinkIdSrv(serverDTO.getId());

            try { //VirtualBoxController
                Map<String, Object> jsonRequest = new HashMap<>();
                jsonRequest.put("command", parsed.get("command"));
                jsonRequest.put("idTask", task.getId());
                jsonRequest.put("name", name);

                String r = greetingClient.nettyRequestPost(
                        "http://" + serverDTO.getIp() + ":" + serverDTO.getPort(),
                        "/ControlVM",
                        Util.jsonObjectToString(jsonRequest),
                        5
                ).block();
                status("RESPONSE", task, "VirtualBoxController response: " + r);
                Map<String, Object> parsedResp = new Gson().fromJson(r, Map.class);
                if (parsedResp.containsKey("status") && parsedResp.get("status").equals("OK")) {

                } else {
                    serverBusy = true;
                    throw new Exception("Status response not OK");
                }
            } catch (Exception e) {
                //Да, сломались, попробуем ещё
                taskFuture(task, "VirtualBoxController request exception: " + Util.stackTraceToString(e));
                next = false;
            }

            if (next) {
                if (parsed.get("command").equals("remove")) {
                    //virtualServerDTO.setStatus(-2); //Этот статус автоматом проставится PingService как сервера не окажется на машине
                    //saveWithoutCache(virtualServerRepo, virtualServerDTO);
                }
                taskComplete(task);
            }

            if (next == false && lockServer == true) {
                status("INFO", task, "ServerDTO Возвращаю статус серверу 0, потому что ошибки исполнения таски");
                serverDTO.setStatus(serverBusy ? 1 : 0);
                Util.logConsole(Thread.currentThread(), "Set status = " + serverDTO.getStatus() + "; idVSrv = " + serverDTO.getId() + "Task: " + task.toString());

                saveWithoutCache(serverRepo, serverDTO);
            }
        }
    }

    public void actionCreateVM(TaskDTO task, Map<String, Object> parsed) {
        boolean next = true;
        if (next) {
            if (!parsed.containsKey("iso")) {
                taskError(task, "Field iso undefined");
                next = false;
            }
        }

        ServerDTO freeServer = null;
        boolean lockServer = false;

        if (next) {
            freeServer = getFreeServerAndLock(task);
            if (freeServer == null) {
                taskFuture(task, "No free server");
                next = false;
            } else {
                lockServer = true;
            }
        }

        RouterDTO routerDTO = null;
        Long idRouter = null;

        if (next) {
            idRouter = freeServer.getIdRouter();
            if (idRouter == null) {
                taskError(task, "Router ID is null by Server: " + freeServer.getId());
                next = false;
            }
        }

        if (next) {
            routerDTO = routerRepo.findById(idRouter).orElse(null);
            if (routerDTO == null) {
                taskError(task, "Router not found by Server: " + freeServer.getId());
                next = false;
            }
        }

        Integer portRouter = null;
        Integer portServer = null;
        String user = null;
        String password = null;

        if (next) {
            task.setLinkIdSrv(freeServer.getId());

            portRouter = getNextPortRouter(idRouter);
            portServer = getNextPortServer(freeServer.getId());

            user = Util.genUser();
            password = Util.genPassword();

            status("INFO", task, "PortRouter: " + portRouter + "; PortServer: " + portServer);
            if (portRouter > 22100) {
                taskError(task, "Max port forwarding on router " + idRouter);
                next = false;
            }
        }

        VirtualServerDTO virtualServerDTO = null;
        boolean serverBusy = false;

        if (next) {

            virtualServerDTO = new VirtualServerDTO();
            virtualServerDTO.setIdSrv(freeServer.getId());
            virtualServerDTO.setIdClient(task.getIdClient());
            virtualServerDTO.setIso((String) parsed.get("iso"));
            virtualServerDTO.setPortLocal(portServer);
            virtualServerDTO.setPortRouter(portRouter);
            virtualServerDTO.setLogin(user);
            virtualServerDTO.setPassword(password);
            virtualServerDTO.setIdTask(task.getId());
            virtualServerDTO.setRdpInfo(freeServer.getIp() + ":" + portRouter);

            if (parsed.containsKey("dateRemove")) {
                String inDate = (String) parsed.get("dateRemove");
                String dateTemplate = "dd.MM.yyyy hh:mm:ss";
                try {
                    DateFormat df = new SimpleDateFormat(dateTemplate);
                    Timestamp ts = new Timestamp(df.parse(inDate).getTime());
                    virtualServerDTO.setDateRemove(ts);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            //System.out.println(virtualServerDTO);
            //next = false;

            saveWithoutCache(virtualServerRepo, virtualServerDTO);

            task.setLinkIdVSrv(virtualServerDTO.getId());


            try { //VirtualBoxController

                Map<String, Object> jsonRequest = new HashMap<>();
                jsonRequest.put("id", virtualServerDTO.getId());
                jsonRequest.put("portLocal", virtualServerDTO.getPortLocal());
                jsonRequest.put("iso", virtualServerDTO.getIso());
                jsonRequest.put("login", virtualServerDTO.getLogin());
                jsonRequest.put("password", virtualServerDTO.getPassword());
                jsonRequest.put("idTask", virtualServerDTO.getIdTask());
                jsonRequest.put("ipRouter", routerDTO.getIp());
                jsonRequest.put("internetPortRouter", portRouter);
                jsonRequest.put("localPortRouter", portServer);
                jsonRequest.put("nameRuleRouter", "RDP_" + virtualServerDTO.getIso() + virtualServerDTO.getId());

                String r = greetingClient.nettyRequestPost(
                        "http://" + freeServer.getIp() + ":" + freeServer.getPort(),
                        "/CreateVM",
                        Util.jsonObjectToString(jsonRequest),
                        5
                ).block();

                status("RESPONSE", task, "VirtualBoxController response: " + r);

                Map<String, Object> parsedResp = new Gson().fromJson(r, Map.class);
                if (parsedResp.containsKey("status") && parsedResp.get("status").equals("OK")) {

                } else {
                    serverBusy = true;
                    throw new Exception("Status response not OK");
                }
            } catch (Exception e) {
                taskFuture(task, "VirtualBoxController request exception: " + Util.stackTraceToString(e));
                virtualServerDTO.setStatus(-2); //Ошибка создания сервера, просто пометим его как удалённый, но на самом деле, сервер просто мог не дособраться, но будет работать
                virtualServerDTO.setResponse("При создании сервера произошла ошибка, будет создаваться новый сервер");
                saveWithoutCache(virtualServerRepo, virtualServerDTO);
                next = false;
            }
        }

        if (next) {
            taskComplete(task);
        }

        if (next == false && lockServer == true) {
            status("INFO", task, "ServerDTO Возвращаю статус серверу 0, потому что ошибки исполнения таски");
            freeServer.setStatus(serverBusy ? 1 : 0);
            Util.logConsole(Thread.currentThread(), "Set status = " + freeServer.getStatus() + "; idVSrv = " + freeServer.getId() + "Task: " + task.toString());

            saveWithoutCache(serverRepo, freeServer);
        }

    }

    private int getNextPortRouter(long idRouter) {
        List<VirtualServerDTO> portRouter = virtualServerRepo.getPortRouter(idRouter);
        List<Integer> list = IntStream.range(22001, 22100).boxed().collect(Collectors.toList());
        for (VirtualServerDTO virtualServerDTO : portRouter) {
            list.remove(virtualServerDTO.getPortRouter());
        }
        return list.get(0);
    }

    private int getNextPortServer(long idSrv) {
        List<VirtualServerDTO> portRouter = virtualServerRepo.getPortServer(idSrv);
        int maxPortRouter = portRouter.size() > 0 ? portRouter.get(0).getPortLocal() : 22001;
        return ++maxPortRouter;
    }

    private ServerDTO getFreeServerAndLock(TaskDTO task) {
        try {
            List<ServerDTO> alreadyServer = serverRepo.getAlready();
            WrapJsonToObject<Map> mapWrapJsonToObject = Util.jsonToObject(task.getTask(), Map.class);
            String preferNameSrv = null;
            if (mapWrapJsonToObject.getException() != null) {
                Map object = mapWrapJsonToObject.getObject();
                if (object.containsKey("preferNameSrv")) {
                    preferNameSrv = (String) object.get("preferNameSrv");
                }
            }
            if (alreadyServer.size() > 0) {
                if (preferNameSrv != null) {
                    for (ServerDTO curSrv : alreadyServer) {
                        if (curSrv.getName().equals(preferNameSrv)) {
                            lockServer(curSrv, task);
                            return curSrv;
                        }
                    }
                }
                ServerDTO srv = alreadyServer.get(0);
                lockServer(srv, task);
                return srv;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void lockServer(ServerDTO srv, TaskDTO task) {
        srv.setStatus(1);
        srv.setIdTask(task.getId());
        srv.setLockDate(new Timestamp(System.currentTimeMillis()));
        saveWithoutCache(serverRepo, srv);
        status("INFO", task, "ServerDTO Блокирую сервер lockServer; idSrv: " + srv.getId());
    }

    public void status(String level, TaskDTO task, String data) {
        if (task != null) {
            task.setResult(data);
            TaskStatusDTO status = new TaskStatusDTO();
            status.setLevel(level);
            status.setData(data);
            status.setIdTask(task.getId());
            saveWithoutCache(taskStatusRepo, status);
        } else {
            Util.logConsole(Thread.currentThread(), "[" + level + "] " + data);
        }
    }

    private void taskComplete(TaskDTO task) {
        taskUpdate(task, 1, "Ok");
    }

    private void taskFuture(TaskDTO task, String description) {
        task.setDateExecute(new Timestamp(System.currentTimeMillis() + 10000));
        taskUpdate(task, 0, description);
    }

    private void taskError(TaskDTO task, String description) {
        taskUpdate(task, -1, description);
    }

    private void taskUpdate(TaskDTO task, int status, String description) {
        //Util.logConsole(Thread.currentThread(), "Status: " + status + "; Description: " + description);
        task.setStatus(status);
        switch (status) {
            case 0:
            case 1:
                status("INFO", task, description);
                break;
            case -1:
                status("ERROR", task, description);
                break;
            default:
                status("UNDEFINED", task, description);
                break;
        }
    }

}
