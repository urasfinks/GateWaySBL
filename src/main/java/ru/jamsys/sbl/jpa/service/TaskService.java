package ru.jamsys.sbl.jpa.service;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.jamsys.sbl.Util;
import ru.jamsys.sbl.UtilRouter;
import ru.jamsys.sbl.jpa.dto.*;
import ru.jamsys.sbl.jpa.repo.*;
import ru.jamsys.sbl.message.Message;
import ru.jamsys.sbl.message.MessageImpl;
import ru.jamsys.sbl.web.GreetingClient;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@Service
public class TaskService {

    GreetingClient greetingClient;
    VirtualServerRepo virtualServerRepo;
    TaskRepo taskRepo;
    ServerRepo serverRepo;
    RouterRepo routerRepo;
    VirtualServerStatusRepo virtualServerStatusRepo;

    @Autowired
    public void setVirtualServerStatusRepo(VirtualServerStatusRepo virtualServerStatusRepo) {
        this.virtualServerStatusRepo = virtualServerStatusRepo;
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
    public Message execOneTask() {

        Message ret = null;
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        List<TaskDTO> listTask = taskRepo.getAlready(timestamp);
        //Util.logConsole(Thread.currentThread(), "::execOneTask count: " + listTask.size());
        if (listTask.size() > 0) {
            TaskDTO task = taskRepo.test(listTask.get(0).getId());
            if (task != null && task.getStatus() == 0) {
                Util.logConsole(Thread.currentThread(), "::execOneTask work: " + task);
                try {
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
                Util.logConsole(Thread.currentThread(), "::execOneTask saved task: " + savedTask);
                ret = new MessageImpl();
            }
        }
        Util.logConsole(Thread.currentThread(), "::execOneTask finish");
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

    private void status(String level, Long idVSrv, String data) {
        if (idVSrv != null) {
            VirtualServerStatusDTO status = new VirtualServerStatusDTO();
            status.setLevel("INFO");
            status.setData(data);
            status.setIdVSrv(idVSrv);
            System.out.println(status);
            virtualServerStatusRepo.save(status);
        } else {
            Util.logConsole(Thread.currentThread(), "[" + level + "] " + data);
        }
    }

    @Transactional
    public void actionCreateVirtualServer(TaskDTO task, Map<String, Object> parsed) {
        long idRouter = 1L;
        if (parsed.containsKey("iso")) {
            //Получить доступный сервер, на который можно начать установку
            ServerDTO freeServer = getFreeServer();
            RouterDTO routerDTO = routerRepo.findById(idRouter).orElse(null);
            if (freeServer != null && routerDTO != null) {
                int portRouter = getNextPortRouter(idRouter);
                int portServer = getNextPortServer(freeServer.getId());
                String user = Util.genUser();
                String password = Util.genPassword();

                Util.logConsole(Thread.currentThread(), "PortRouter: " + portRouter + "; PortServer: " + portServer);

                boolean next = true;

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
                        virtualServerDTO.setIdRouter(idRouter);

                        virtualServerRepo.save(virtualServerDTO);
                    } catch (Exception e) {
                        status("ERROR", null, "AddPortForwarding response: " + Util.stackTraceToString(e));
                        next = false;
                    }
                }
                if (next) {
                    try {
                        //Add port forwarding
                        String resp = UtilRouter.addPortForwarding(
                                routerDTO.getIp(),
                                "RDP_" + virtualServerDTO.getIso() + virtualServerDTO.getId(),
                                portRouter + "",
                                freeServer.getIp(),
                                portServer + ""
                        );

                        status("INFO", virtualServerDTO.getId(), "AddPortForwarding response: " + resp);
                    } catch (Exception e) {
                        status("ERROR", virtualServerDTO.getId(), "AddPortForwarding response: " + Util.stackTraceToString(e));
                        next = false;
                    }
                }
                if (next) {
                    try {
                        String r = greetingClient.nettyRequest(
                                "http://localhost:3000",
                                "",
                                Util.jsonObjectToString(virtualServerDTO),
                                5
                        ).block();

                        Util.logConsole(Thread.currentThread(), "VirtualBoxController response: " + r);
                        Map<String, Object> rp = new Gson().fromJson(r, Map.class);
                        //{ "id":1, "others":{ "max_rules":64 }, "error_code":"34800" }
                        String errorCode = (String) rp.get("error_code");
                        if (!errorCode.equals("0")) {
                            next = false;
                        }
                    } catch (Exception e) {
                        status("ERROR", virtualServerDTO.getId(), "VirtualBoxController response: " + Util.stackTraceToString(e));
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
                Util.logConsole(Thread.currentThread(), "No free server or router");
                task.setResult("No free server or router");
                //Нет сервера, просто вперёд передвигаем исполнение
                task.setDateExecute(new Timestamp(System.currentTimeMillis() + 60000));
            }
        } else {
            Util.logConsole(Thread.currentThread(), "Field iso undefined");
            task.setResult("Field iso undefined");
        }
    }

}
