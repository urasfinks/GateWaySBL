package ru.jamsys.sbl.jpa.service;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.jamsys.sbl.Util;
import ru.jamsys.sbl.jpa.dto.*;
import ru.jamsys.sbl.jpa.repo.*;
import ru.jamsys.sbl.message.Message;
import ru.jamsys.sbl.message.MessageImpl;
import ru.jamsys.sbl.web.GreetingClient;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TaskService {

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

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    public Message exec() {

        Message ret = null;
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        List<TaskDTO> listTask = taskRepo.getAlready(timestamp);
        //Util.logConsole(Thread.currentThread(), "::execOneTask count: " + listTask.size());
        if (listTask.size() > 0) {
            TaskDTO task = taskRepo.test(listTask.get(0).getId());
            if (task != null && task.getStatus() == 0) {
                Util.logConsole(Thread.currentThread(), "::execOneTask work: " + task);
                try {
                    task.setResult(""); //Если взяли в работу, то от предыдущего раза очистим результат
                    execTask(task);
                } catch (Exception e) {
                    Integer retry = task.getRetry();
                    if (retry != null) {
                        task.setRetry(++retry);
                    }
                    retry = task.getRetry();
                    if (retry != null && retry < 5) {
                        task.setStatus(0);
                    } else {
                        task.setStatus(-1);
                    }
                    task.setResult(Util.stackTraceToString(e));
                }
                task.setDateUpdate(new Timestamp(System.currentTimeMillis()));
                TaskDTO savedTask = taskRepo.save(task);
                //Util.logConsole(Thread.currentThread(), "::execOneTask saved task: " + savedTask);
                ret = new MessageImpl();
            }
        }
        //Util.logConsole(Thread.currentThread(), "::execOneTask finish");
        return ret;
    }

    @SuppressWarnings("unchecked")
    @Transactional
    public void execTask(TaskDTO task) {
        Map<String, Object> parsed = new Gson().fromJson(task.getTask(), Map.class);
        if (parsed.get("action") != null) {
            String action = (String) parsed.get("action");
            if (action.equals("CreateVirtualServer")) {
                actionCreateVirtualServer(task, parsed);
            } else {
                task.setStatus(-1);
                task.setResult("Action not define");
            }
        }
    }

    private int getNextPortRouter(long idRouter) {
        List<VirtualServerDTO> portRouter = virtualServerRepo.getPortRouter(idRouter);
        int maxPortRouter = portRouter.size() > 0 ? portRouter.get(0).getPortRouter() : 22001;
        return ++maxPortRouter;
    }

    private int getNextPortServer(long idSrv) {
        List<VirtualServerDTO> portRouter = virtualServerRepo.getPortServer(idSrv);
        int maxPortRouter = portRouter.size() > 0 ? portRouter.get(0).getPortLocal() : 22001;
        return ++maxPortRouter;
    }

    private ServerDTO getFreeServer() {
        List<ServerDTO> alreadyServer = serverRepo.getAlready();
        if (alreadyServer.size() > 0) {
            ServerDTO srv = alreadyServer.get(0);
            srv.setStatus(1);
            serverRepo.save(srv);
            return srv;
        }
        return null;
    }

    private void status(String level, Long idTask, String data) {
        if (idTask != null) {
            TaskStatusDTO status = new TaskStatusDTO();
            status.setLevel("INFO");
            status.setData(data);
            status.setIdTask(idTask);
            System.out.println(status);
            taskStatusRepo.save(status);
        } else {
            Util.logConsole(Thread.currentThread(), "[" + level + "] " + data);
        }
    }

    @Transactional
    public void actionCreateVirtualServer(TaskDTO task, Map<String, Object> parsed) {

        if (parsed.containsKey("iso")) {
            //Получить доступный сервер, на который можно начать установку
            ServerDTO freeServer = getFreeServer();

            if (freeServer != null) {

                long idRouter = freeServer.getIdRouter();
                RouterDTO routerDTO = routerRepo.findById(idRouter).orElse(null);

                task.setLinkIdSrv(freeServer.getId());

                int portRouter = getNextPortRouter(idRouter);
                int portServer = getNextPortServer(freeServer.getId());


                String user = Util.genUser();
                String password = Util.genPassword();

                Util.logConsole(Thread.currentThread(), "PortRouter: " + portRouter + "; PortServer: " + portServer);

                boolean next = true;

                if (next) {
                    if (portRouter > 22100) {
                        status("ERROR", task.getId(), "Max port forwarding on router " + idRouter);
                        next = false;
                    }
                }

                VirtualServerDTO virtualServerDTO = null;
                if (next) {
                    try {
                        virtualServerDTO = new VirtualServerDTO();
                        virtualServerDTO.setIdSrv(freeServer.getId());
                        virtualServerDTO.setIdClient(task.getIdClient());
                        virtualServerDTO.setIso((String) parsed.get("iso"));
                        virtualServerDTO.setPortLocal(portServer);
                        virtualServerDTO.setPortRouter(portRouter);
                        virtualServerDTO.setLogin(user);
                        virtualServerDTO.setPassword(password);
                        virtualServerDTO.setIdTask(task.getId());

                        virtualServerRepo.save(virtualServerDTO);

                        task.setLinkIdVSrv(virtualServerDTO.getId());

                    } catch (Exception e) {
                        status("ERROR", task.getId(), "AddPortForwarding response: " + Util.stackTraceToString(e));
                        next = false;
                    }
                }

                if (next) {
                    try { //VirtualBoxController

                        Map<String, Object> createVmJson = new HashMap<>();
                        createVmJson.put("id", virtualServerDTO.getId());
                        createVmJson.put("portLocal", virtualServerDTO.getPortLocal());
                        createVmJson.put("iso", virtualServerDTO.getIso());
                        createVmJson.put("login", virtualServerDTO.getLogin());
                        createVmJson.put("password", virtualServerDTO.getPassword());
                        createVmJson.put("idTask", virtualServerDTO.getIdTask());
                        createVmJson.put("ipRouter", routerDTO.getIp());
                        createVmJson.put("internetPortRouter", portRouter);
                        createVmJson.put("localPortRouter", portServer);
                        createVmJson.put("nameRuleRouter", "RDP_" + virtualServerDTO.getIso() + virtualServerDTO.getId());

                        String r = greetingClient.nettyRequestPost(
                                "http://" + freeServer.getIp() + ":3000",
                                "/CreateVM",
                                Util.jsonObjectToString(createVmJson),
                                5
                        ).block();

                        Util.logConsole(Thread.currentThread(), "VirtualBoxController response: " + r);
                    } catch (Exception e) {
                        status("ERROR", task.getId(), "VirtualBoxController response: " + Util.stackTraceToString(e));
                        next = false;
                    }
                }

                if (!next) {
                    if (virtualServerDTO != null) {
                        virtualServerDTO.setStatus(-1);
                        virtualServerRepo.save(virtualServerDTO);
                    }
                    //Возвращаем сервер как доступный
                    freeServer.setStatus(0);
                    serverRepo.save(freeServer);
                }

                task.setStatus(1);

            } else {
                Util.logConsole(Thread.currentThread(), "Not found free server or router");
                task.setResult("Not found free server or router");
                //Нет сервера, просто вперёд передвигаем исполнение
                task.setDateExecute(new Timestamp(System.currentTimeMillis() + 60000));
            }
        } else {
            Util.logConsole(Thread.currentThread(), "Field iso undefined");
            task.setResult("Field iso undefined");
        }
    }

}
