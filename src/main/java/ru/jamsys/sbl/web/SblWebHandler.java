package ru.jamsys.sbl.web;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.jamsys.sbl.SblApplication;
import ru.jamsys.sbl.Util;
import ru.jamsys.sbl.WrapJsonToObject;
import ru.jamsys.sbl.component.CmpStatistic;
import ru.jamsys.sbl.jpa.dto.*;
import ru.jamsys.sbl.jpa.repo.*;
import ru.jamsys.sbl.jpa.service.TaskService;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

@Component
public class SblWebHandler {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    public void setTaskService(TaskService taskService) {
        this.taskService = taskService;
    }

    TaskService taskService;

    protected <T> T saveWithoutCache(CrudRepository<T, Long> crudRepository, T entity) {
        return SblApplication.saveWithoutCache(em, crudRepository, entity);
    }

    ClientRepo clientRepo;
    ServerRepo serverRepo;
    VirtualServerRepo virtualServerRepo;
    BillidRepo billidRepo;
    PromoRepo promoRepo;
    VirtualServerStatusRepo virtualServerStatusRepo;
    ActionsRepo actionsRepo;
    DeleteTimeRepo deleteTimeRepo;
    TaskStatusRepo taskStatusRepo;
    TaskRepo taskRepo;
    CmpStatistic cmpStatistic;

    @Autowired
    public void setTaskStatusRepo(TaskStatusRepo taskStatusRepo) {
        this.taskStatusRepo = taskStatusRepo;
    }

    @Autowired
    public void setTaskRepo(TaskRepo taskRepo) {
        this.taskRepo = taskRepo;
    }

    @Autowired
    public void setCmpStatistic(CmpStatistic cmpStatistic) {
        this.cmpStatistic = cmpStatistic;
    }

    @Autowired
    public void setClientRepo(ClientRepo clientRepo) {
        this.clientRepo = clientRepo;
    }

    @Autowired
    public void setBillidRepo(BillidRepo billidRepo) {
        this.billidRepo = billidRepo;
    }

    @Autowired
    public void setPromoRepo(PromoRepo promoRepo) {
        this.promoRepo = promoRepo;
    }

    @Autowired
    public void setActionsRepo(ActionsRepo actionsRepo) {
        this.actionsRepo = actionsRepo;
    }

    @Autowired
    public void setDeleteTimeRepo(DeleteTimeRepo deleteTimeRepo) {
        this.deleteTimeRepo = deleteTimeRepo;
    }

    @Autowired
    public void setServerRepo(ServerRepo serverRepo) {
        this.serverRepo = serverRepo;
    }

    @Autowired
    public void setVirtualServerRepo(VirtualServerRepo virtualServerRepo) {
        this.virtualServerRepo = virtualServerRepo;
    }

    @Autowired
    public void setVirtualServerStatusRepo(VirtualServerStatusRepo virtualServerStatusRepo) {
        this.virtualServerStatusRepo = virtualServerStatusRepo;
    }

    @NonNull
    public Mono<ServerResponse> healthCheck(ServerRequest request) {
        return ServerResponse.ok().contentType(MediaType.TEXT_PLAIN)
                .body(BodyInserters.fromValue(new JsonResponse().toString()));
    }

    @NonNull
    public Mono<ServerResponse> getClient(ServerRequest serverRequest) {
        return ServerResponse.ok().body(Flux.fromIterable(clientRepo.findAll()), ClientDTO.class);
    }

    @NonNull
    public Mono<ServerResponse> getBillid(ServerRequest serverRequest) {
        return ServerResponse.ok().body(Flux.fromIterable(billidRepo.findAll()), BillidDTO.class);
    }

    @NonNull
    public Mono<ServerResponse> getActions(ServerRequest serverRequest) {
        return ServerResponse.ok().body(Flux.fromIterable(actionsRepo.findAll()), ActionsDTO.class);
    }

    @NonNull
    public Mono<ServerResponse> getDeleteTime(ServerRequest serverRequest) {
        return ServerResponse.ok().body(Flux.fromIterable(deleteTimeRepo.findAll()), DeleteTimeDTO.class);
    }

    @NonNull
    public Mono<ServerResponse> getApi(ServerRequest serverRequest) {
        if (!checkDeny(serverRequest)) {
            return accessDeny();
        }
        cmpStatistic.incShareStatistic("WebRequestApi");
        Mono<String> bodyData = serverRequest.bodyToMono(String.class);

        return bodyData.flatMap(body -> {
            System.out.println("Request: " + body);
            JsonResponse jRet = new JsonResponse();
            if (body != null && !body.isEmpty()) {
                try {
                    //jRet.addData("idClient", 453);
                    jRet.addData("idClient", 92602);
                } catch (Exception e) {
                    jRet.set(HttpStatus.EXPECTATION_FAILED, e.toString());
                    e.printStackTrace();
                }
            } else {
                jRet.set(HttpStatus.EXPECTATION_FAILED, "Empty request");
            }
            if (!jRet.status.equals(HttpStatus.OK)) {
                cmpStatistic.incShareStatistic("WebError");
            }
            System.out.println("Response: " + jRet.toString());
            return ServerResponse.status(jRet.status).body(BodyInserters.fromValue(jRet.toString()));
        });
    }

    @NonNull
    public Mono<ServerResponse> getServer(ServerRequest serverRequest) {
        if (!checkDeny(serverRequest)) {
            return accessDeny();
        }
        return ServerResponse.ok().body(Flux.fromIterable(serverRepo.findAll()), ServerDTO.class);
    }

    @NonNull
    public Mono<ServerResponse> getVirtualServer(ServerRequest serverRequest) {
        if (!checkDeny(serverRequest)) {
            return accessDeny();
        }
        return ServerResponse.ok().body(Flux.fromIterable(virtualServerRepo.findAll()), VirtualServerDTO.class);
    }

    @NonNull
    public Mono<ServerResponse> getTaskByIdClient(ServerRequest serverRequest) {
        if (!checkDeny(serverRequest)) {
            return accessDeny();
        }
        Long idClient = Long.parseLong(serverRequest.pathVariable("id"));
        return ServerResponse.ok().body(Flux.fromIterable(taskRepo.findAllByIdClient(idClient)), TaskDTO.class);
    }

    @NonNull
    public Mono<ServerResponse> getBillidByIdClient(ServerRequest serverRequest) {
        if (!checkDeny(serverRequest)) {
            return accessDeny();
        }
        Long idClient = Long.parseLong(serverRequest.pathVariable("id"));
        return ServerResponse.ok().body(Flux.fromIterable(billidRepo.findAllByIdClient(idClient)), BillidDTO.class);
    }

    public Mono<ServerResponse> getPromoCode(ServerRequest serverRequest) {
        if (!checkDeny(serverRequest)) {
            return accessDeny();
        }
        String promo = serverRequest.pathVariable("id");
        return ServerResponse.ok().body(Flux.fromIterable(promoRepo.findAllByPromo(promo)), PromoDTO.class);
    }

    @NonNull
    public Mono<ServerResponse> getActionsByIdClient(ServerRequest serverRequest) {
        if (!checkDeny(serverRequest)) {
            return accessDeny();
        }
        Long idClient = Long.parseLong(serverRequest.pathVariable("id"));
        return ServerResponse.ok().body(Flux.fromIterable(actionsRepo.findAllByIdClient(idClient)), ActionsDTO.class);
    }

    @NonNull
    public Mono<ServerResponse> getDeleteTimeByIdVirtualServer(ServerRequest serverRequest) {
        if (!checkDeny(serverRequest)) {
            return accessDeny();
        }
        Long idSrv = Long.parseLong(serverRequest.pathVariable("id"));
        return ServerResponse.ok().body(Flux.fromIterable(deleteTimeRepo.findAllByIdClient(idSrv)), ActionsDTO.class);
    }

    @NonNull
    public Mono<ServerResponse> getVirtualServerByIdClient(ServerRequest serverRequest) {
        if (!checkDeny(serverRequest)) {
            return accessDeny();
        }
        Long idClient = Long.parseLong(serverRequest.pathVariable("id"));
        return ServerResponse.ok().body(Flux.fromIterable(virtualServerRepo.findAllByIdClient(idClient)), VirtualServerDTO.class);
    }

    @NonNull
    public Mono<ServerResponse> getVirtualServerStatus(ServerRequest serverRequest) {
        if (!checkDeny(serverRequest)) {
            return accessDeny();
        }
        return ServerResponse.ok().body(Flux.fromIterable(taskStatusRepo.findAll()), TaskStatusDTO.class);
    }

    @NonNull
    public Mono<ServerResponse> getTaskStatus(ServerRequest serverRequest) {
        if (!checkDeny(serverRequest)) {
            return accessDeny();
        }
        return ServerResponse.ok().body(Flux.fromIterable(virtualServerStatusRepo.findAll()), ClientDTO.class);
    }

    @NonNull
    public Mono<ServerResponse> postClient(ServerRequest serverRequest) {
        if (!checkDeny(serverRequest)) {
            return accessDeny();
        }
        return postHandler(serverRequest, clientRepo, ClientDTO.class);
    }

    @NonNull
    public Mono<ServerResponse> postActions(ServerRequest serverRequest) {
        if (!checkDeny(serverRequest)) {
            return accessDeny();
        }
        return postHandler(serverRequest, actionsRepo, ActionsDTO.class);
    }

    @NonNull
    public Mono<ServerResponse> postDeleteTime(ServerRequest serverRequest) {
        if (!checkDeny(serverRequest)) {
            return accessDeny();
        }
        return postHandler(serverRequest, deleteTimeRepo, DeleteTimeDTO.class);
    }

    @NonNull
    public Mono<ServerResponse> postBillid(ServerRequest serverRequest) {
        if (!checkDeny(serverRequest)) {
            return accessDeny();
        }
        return postHandler(serverRequest, billidRepo, BillidDTO.class);
    }

    public Mono<ServerResponse> postPromo(ServerRequest serverRequest) {
        if (!checkDeny(serverRequest)) {
            return accessDeny();
        }
        return postHandler(serverRequest, promoRepo, PromoDTO.class);
    }

    @NonNull
    public Mono<ServerResponse> postServer(ServerRequest serverRequest) {
        if (!checkDeny(serverRequest)) {
            return accessDeny();
        }
        return postHandler(serverRequest, serverRepo, ServerDTO.class);
    }

    @NonNull
    public Mono<ServerResponse> postTask(ServerRequest serverRequest) {
        if (!checkDeny(serverRequest)) {
            return accessDeny();
        }
        return postHandler(serverRequest, taskRepo, TaskDTO.class, (obj, body) -> obj.getObject().setTask(body));
    }

    @NonNull
    public Mono<ServerResponse> postVirtualServer(ServerRequest serverRequest) {
        if (!checkDeny(serverRequest)) {
            return accessDeny();
        }
        return postHandler(serverRequest, virtualServerRepo, VirtualServerDTO.class);
    }

    @NonNull
    public Mono<ServerResponse> postVirtualServerStatus(ServerRequest serverRequest) {
        if (!checkDeny(serverRequest)) {
            return accessDeny();
        }
        return postHandler(serverRequest, virtualServerStatusRepo, VirtualServerStatusDTO.class);
    }

    @NonNull
    public Mono<ServerResponse> postTaskStatus(ServerRequest serverRequest) {
        if (!checkDeny(serverRequest)) {
            return accessDeny();
        }
        return postHandler(serverRequest, taskStatusRepo, TaskStatusDTO.class);
    }

    public Mono<ServerResponse> getVdsAvailable(ServerRequest serverRequest) {
        if (!checkDeny(serverRequest)) {
            return accessDeny();
        }
        return ServerResponse.ok().body(BodyInserters.fromValue(SblApplication.getAvgVSrvAvailable(serverRepo, "check")));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Mono<ServerResponse> patchHandler(ServerRequest serverRequest, BiConsumer<String, JsonResponse> consumer) {
        if (!checkDeny(serverRequest)) {
            return accessDeny();
        }
        cmpStatistic.incShareStatistic("WebRequest");
        Mono<String> bodyData = serverRequest.bodyToMono(String.class);
        return bodyData.flatMap(body -> {
            JsonResponse jRet = new JsonResponse();
            if (body != null && !body.isEmpty()) {
                try {
                    synchronized (SblApplication.class) {
                        consumer.accept(body, jRet);
                    }
                } catch (Exception e) {
                    jRet.set(HttpStatus.EXPECTATION_FAILED, e.toString());
                    e.printStackTrace();
                }
            } else {
                jRet.set(HttpStatus.EXPECTATION_FAILED, "Empty request");
            }
            if (!jRet.status.equals(HttpStatus.OK)) {
                cmpStatistic.incShareStatistic("WebError");
            }
            return ServerResponse.status(jRet.status).body(BodyInserters.fromValue(jRet.toString()));
        });
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @NonNull
    public Mono<ServerResponse> patchVirtualServer(ServerRequest serverRequest) {
        if (!checkDeny(serverRequest)) {
            return accessDeny();
        }
        System.out.println("Request");
        Long idVSrv = Long.parseLong(serverRequest.pathVariable("id"));
        return patchHandler(serverRequest, (body, jRet) -> {
            Util.logConsole(Thread.currentThread(), "::patchTaskComplete " + body);
            synchronized (SblApplication.class) {
                //virtualServerRepo.
                Map<String, Object> req = null;
                boolean next = true;
                if (next) {
                    try {
                        req = new Gson().fromJson(body, Map.class);
                    } catch (Exception e) {
                        jRet.set(HttpStatus.EXPECTATION_FAILED, "Json parsing fail: " + body);
                        next = false;
                    }
                }
                VirtualServerDTO virtualServerDTO = null;
                if (next) {
                    try {
                        virtualServerDTO = virtualServerRepo.findById(idVSrv).orElse(null);
                        if (virtualServerDTO == null) {
                            jRet.set(HttpStatus.OK, "Not found VirtualServer " + idVSrv + ": " + body);
                            next = false;
                        }
                    } catch (Exception e) {
                        jRet.set(HttpStatus.EXPECTATION_FAILED, "Exception get task: " + e);
                        next = false;
                    }
                }
                if (next) {
                    if (!req.containsKey("dateRemove")) {
                        jRet.set(HttpStatus.EXPECTATION_FAILED, "Field dateRemove not found in json: " + body);
                        next = false;
                    }
                }
                if (next) {
                    String inDate = (String) req.get("dateRemove");
                    String dateTemplate = "dd.MM.yyyy hh:mm:ss";
                    try {
                        DateFormat df = new SimpleDateFormat(dateTemplate);
                        Timestamp ts = new Timestamp(df.parse(inDate).getTime());
                        virtualServerDTO.setDateRemove(ts);
                    } catch (Exception e) {
                        jRet.set(HttpStatus.EXPECTATION_FAILED, "Field parse dateRemove: " + inDate + " by template: " + dateTemplate);
                        next = false;
                    }
                }
                if (next) {
                    saveWithoutCache(virtualServerRepo, virtualServerDTO);
                }
                //jRet.set(HttpStatus.EXPECTATION_FAILED, "idVSrv: " + idVSrv + " body: " + body);
                if (!next) {
                    Util.logConsole(Thread.currentThread(), jRet.toString());
                }
            }
        });
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @NonNull
    public Mono<ServerResponse> patchTaskComplete(ServerRequest serverRequest) {
        if (!checkDeny(serverRequest)) {
            return accessDeny();
        }
        return patchHandler(serverRequest, (body, jRet) -> {
            Util.logConsole(Thread.currentThread(), "::patchTaskComplete " + body);
            synchronized (SblApplication.class) {
                Map<String, Object> req = null;
                boolean next = true;
                if (next) {
                    try {
                        req = new Gson().fromJson(body, Map.class);
                    } catch (Exception e) {
                        jRet.set(HttpStatus.EXPECTATION_FAILED, "Json parsing fail: " + body);
                        next = false;
                    }
                }

                if (next) {
                    if (!req.containsKey("idTask")) {
                        jRet.set(HttpStatus.EXPECTATION_FAILED, "Field idTask not found in json: " + body);
                        next = false;
                    }
                }

                if (next) {
                    if (!req.containsKey("statusTask")) {
                        jRet.set(HttpStatus.EXPECTATION_FAILED, "Field statusTask not found in json: " + body);
                        next = false;
                    }
                }

                TaskDTO task = null;
                if (next) {
                    try {
                        Double x = (Double) req.get("idTask");
                        task = taskRepo.findById(x.longValue()).orElse(null);
                        if (task == null) {
                            //Наткнулся на неочень приятную историю, когда удалил таску, и логично что мы её не можем найти
                            //VirtualBoxController впал в бесконечнй докат, поэтому я верну OK
                            jRet.set(HttpStatus.OK, "Not found task: " + body);
                            next = false;
                        }
                    } catch (Exception e) {
                        jRet.set(HttpStatus.EXPECTATION_FAILED, "Exception get task: " + e);
                        next = false;
                    }
                }

                if (next) {
                    if (task.getLinkIdSrv() == null) {
                        jRet.set(HttpStatus.EXPECTATION_FAILED, "Server by Task is null");
                        next = false;
                    }
                }

                ServerDTO serverDTO = null;
                if (next) {
                    try {
                        serverDTO = serverRepo.findById(task.getLinkIdSrv()).orElse(null);
                        if (serverDTO == null) {
                            jRet.set(HttpStatus.EXPECTATION_FAILED, "Server by Task not found");
                            next = false;
                        }
                    } catch (Exception e) {
                        jRet.set(HttpStatus.EXPECTATION_FAILED, "Exception get server: " + e);
                        next = false;
                    }
                }

                if (next) {
                    try {
                        //Util.logConsole(Thread.currentThread(), "ServerDTO TaskComplete разблокирую сервер");
                        taskService.status("INFO", task, "ServerDTO Возвращаю статус серверу 0, так как сервер закончил задачу");
                        Util.logConsole(Thread.currentThread(), "Set status = 0; idVSrv = " + serverDTO.getId() + "Task: " + task);
                        serverDTO.setStatus(0); //Сам URL говорит что это конечная итарация VirtualBoxController, переводим сервер в режим готовности на обработку тасков
                        saveWithoutCache(serverRepo, serverDTO);
                    } catch (Exception e) {
                        jRet.set(HttpStatus.EXPECTATION_FAILED, "Set status Server exception: " + e);
                        next = false;
                    }
                }
                if (next) {
                    try {
                        Double x2 = (Double) req.get("statusTask");
                        task.setStatus(x2.intValue());
                        if (task.getStatus() < 0) {
                            postAnalyzeError(task); //Анализ что это была таска установки сервера, для удаления и retry
                        }
                        saveWithoutCache(taskRepo, task);
                    } catch (Exception e) {
                        jRet.set(HttpStatus.EXPECTATION_FAILED, "Set status Task exception: " + e);
                        next = false;
                    }
                }

                if (task != null) {
                    taskService.status("PATCH_REQUEST_TASK_COMPLETE", task, body);
                    taskService.status("PATCH_ANSWER_TASK_COMPLETE", task, jRet.toString());
                } else {
                    Util.logConsole(Thread.currentThread(), jRet.toString());
                }

            }

        });
    }

    private void postAnalyzeError(TaskDTO task) {
        Map<String, Object> parsed = new Gson().fromJson(task.getTask(), Map.class);
        /*
         * 1) Создание VM
         *    Если пришёл отбой мы должны
         *       1.1) Создать задачу на удаление сервера
         *       1.2) Обновить таску retry + 1
         * */
        if (parsed.containsKey("action")) {
            String action = (String) parsed.get("action");
            if (action.equals("CreateVM")) {
                VirtualServerDTO virtualServerDTO = virtualServerRepo.findById(task.getLinkIdVSrv()).orElse(null);
                TaskDTO removeTask = task.childTask();

                Map<String, Object> dataRemoveTask = new HashMap<>();
                dataRemoveTask.put("action", "ControlVM");
                dataRemoveTask.put("command", "remove");
                dataRemoveTask.put("name", virtualServerDTO.getIso() + "_" + task.getLinkIdVSrv());

                removeTask.setTask(Util.jsonObjectToString(dataRemoveTask));

                saveWithoutCache(taskRepo, removeTask);

                taskService.status("ROLLBACK", task, "Task failed. Remove VM: " + (virtualServerDTO.getIso() + "_" + task.getLinkIdVSrv()) + " and restore task status = 0");

                task.incRetry();
                task.setStatus(0); //Пойдём в перенакат
            }
        }
    }

    private <T> Mono<ServerResponse> postHandler(ServerRequest serverRequest, CrudRepository crudRepository, Class<T> classType, BiConsumer<WrapJsonToObject<T>, String> handler) {
        cmpStatistic.incShareStatistic("WebRequest");
        Mono<String> bodyData = serverRequest.bodyToMono(String.class);

        return bodyData.flatMap(body -> {
            JsonResponse jRet = new JsonResponse();
            if (body != null && !body.isEmpty()) {
                try {
                    WrapJsonToObject<Map> test = Util.jsonToObject(body, Map.class);
                    if (test.getObject().containsKey("action")) {
                        String act = (String) test.getObject().get("action");
                        if (act.equals("CreateVM")) {

                            try {
                                Util.telegramSend(SblApplication.getAvgVSrvAvailable(serverRepo, "CreateVM"));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            int countAvailable = serverRepo.getAvailable().size();
                            if (countAvailable == 0) {
                                throw new Exception("No servers available");
                            }
                        }
                    }
                    WrapJsonToObject<T> wrapJsonToObject = handler != null ?
                            Util.jsonToObjectOverflowProperties(body, classType)
                            : Util.jsonToObject(body, classType);
                    if (wrapJsonToObject.getException() == null) {
                        synchronized (SblApplication.class) {
                            if (handler != null) {
                                handler.accept(wrapJsonToObject, body);
                            }
                            T o = wrapJsonToObject.getObject();
                            saveWithoutCache(crudRepository, o);
                            String[] split = o.getClass().getName().split("\\.");
                            jRet.addData(split[split.length - 1].replace("DTO", ""), o);
                        }
                    } else {
                        jRet.set(HttpStatus.EXPECTATION_FAILED, wrapJsonToObject.getException().toString());
                    }
                } catch (Exception e) {
                    jRet.set(HttpStatus.EXPECTATION_FAILED, e.toString());
                    e.printStackTrace();
                }
            } else {
                jRet.set(HttpStatus.EXPECTATION_FAILED, "Empty request");
            }
            if (!jRet.status.equals(HttpStatus.OK)) {
                cmpStatistic.incShareStatistic("WebError");
            }
            return ServerResponse.status(jRet.status).body(BodyInserters.fromValue(jRet.toString()));
        });
    }

    private <T> Mono<ServerResponse> postHandler(ServerRequest serverRequest, CrudRepository crudRepository, Class<T> classType) {
        return postHandler(serverRequest, crudRepository, classType, null);
    }

    private boolean checkDeny(ServerRequest serverRequest) {
        Iterable<ServerDTO> all = serverRepo.findAll();
        List<String> ips = new ArrayList<>();
        for (ServerDTO serverDTO : all) {
            ips.add("/" + serverDTO.getIp());
        }
        ips.add("/127.0.0.1");
        String requestIp = serverRequest.remoteAddress().get().getAddress().toString();
        if (!ips.contains(requestIp)) {
            Util.telegramSend("FORBIDDEN for ip: " + requestIp + "; path: " + serverRequest.path());
        }
        return ips.contains(requestIp);
    }

    private Mono<ServerResponse> accessDeny() {
        return ServerResponse.status(HttpStatus.FORBIDDEN).body(BodyInserters.fromValue("{\"status\":\"FORBIDDEN\"}"));
    }

}