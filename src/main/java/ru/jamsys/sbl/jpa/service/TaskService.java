package ru.jamsys.sbl.jpa.service;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.jamsys.sbl.Util;
import ru.jamsys.sbl.jpa.dto.ServerDTO;
import ru.jamsys.sbl.jpa.dto.TaskDTO;
import ru.jamsys.sbl.jpa.dto.VirtualServerDTO;
import ru.jamsys.sbl.jpa.repo.ServerRepo;
import ru.jamsys.sbl.jpa.repo.TaskRepo;
import ru.jamsys.sbl.jpa.repo.VirtualServerRepo;
import ru.jamsys.sbl.message.Message;
import ru.jamsys.sbl.message.MessageImpl;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@Service
public class TaskService {

    VirtualServerRepo virtualServerRepo;
    TaskRepo taskRepo;
    ServerRepo serverRepo;

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
    public Message execOneTask() {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        List<TaskDTO> listTask = taskRepo.getAlready(timestamp);
        //Util.logConsole(Thread.currentThread(), "::execOneTask count: "+listTask.size());
        if (listTask.size() > 0) {
            TaskDTO task = taskRepo.findOneForUpdate(listTask.get(0).getId());
            if (task != null) {
                //Util.logConsole(Thread.currentThread(), task.toString());
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
                taskRepo.save(task);
                return new MessageImpl();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private void execTask(TaskDTO task) {
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
        int maxPortRouter = portRouter.size() > 0 ? portRouter.get(0).getPortRouter() : 2020;
        return ++maxPortRouter;
    }

    private int getNextPortServer(long idSrv) {
        List<VirtualServerDTO> portRouter = virtualServerRepo.getPortServer(idSrv);
        int maxPortRouter = portRouter.size() > 0 ? portRouter.get(0).getPortLocal() : 2020;
        return ++maxPortRouter;
    }

    private Long getFreeServer() {
        List<ServerDTO> alreadyServer = serverRepo.getAlready();
        if (alreadyServer.size() > 0) {
            return alreadyServer.get(0).getId();
        }
        return null;
    }

    private void actionCreateVirtualServer(TaskDTO task, Map<String, Object> parsed) {
        long idRouter = 1L;
        if (parsed.containsKey("iso")) {
            //Получить доступный сервер, на который можно начать установку
            Long freeServer = getFreeServer();
            if (freeServer != null) {
                int portRouter = getNextPortRouter(idRouter);
                int portServer = getNextPortServer(freeServer);
                String user = Util.genUser();
                String password = Util.genPassword();

                Util.logConsole(Thread.currentThread(), "PortRouter: " + portRouter + "; PortServer: " + portServer);

                VirtualServerDTO virtualServerDTO = new VirtualServerDTO();
                virtualServerDTO.setIdSrv(freeServer);
                virtualServerDTO.setIdClient(task.getIdClient());
                virtualServerDTO.setIso((String) parsed.get("iso"));
                virtualServerDTO.setPortLocal(portServer);
                virtualServerDTO.setPortRouter(portRouter);
                virtualServerDTO.setLogin(user);
                virtualServerDTO.setPassword(password);
                virtualServerDTO.setIdRouter(idRouter);

                virtualServerRepo.save(virtualServerDTO);

                task.setStatus(1);

            } else {
                task.setResult("No free server");
            }
        } else {
            task.setResult("Field iso undefined");
        }
    }

}
