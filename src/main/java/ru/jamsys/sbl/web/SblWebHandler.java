package ru.jamsys.sbl.web;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
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
import java.util.HashMap;
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

    @Transactional
    protected <T> T saveWithoutCache(CrudRepository<T, Long> crudRepository, T entity) {
        //Это самое больше зло, с чем я встречался
        T ret = crudRepository.save(entity);
        try {
            em.flush();
        } catch (Exception e) {
        }
        return ret;
    }

    ClientRepo clientRepo;
    ServerRepo serverRepo;
    VirtualServerRepo virtualServerRepo;
    VirtualServerStatusRepo virtualServerStatusRepo;
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
    public Mono<ServerResponse> getApi(ServerRequest serverRequest) {
        return ServerResponse.ok().body(BodyInserters.fromValue("Hello world"));
    }

    @NonNull
    public Mono<ServerResponse> getServer(ServerRequest serverRequest) {
        return ServerResponse.ok().body(Flux.fromIterable(serverRepo.findAll()), ServerDTO.class);
    }

    @NonNull
    public Mono<ServerResponse> getVirtualServer(ServerRequest serverRequest) {
        return ServerResponse.ok().body(Flux.fromIterable(virtualServerRepo.findAll()), VirtualServerDTO.class);
    }

    @NonNull
    public Mono<ServerResponse> getVirtualServerStatus(ServerRequest serverRequest) {
        return ServerResponse.ok().body(Flux.fromIterable(taskStatusRepo.findAll()), TaskStatusDTO.class);
    }

    @NonNull
    public Mono<ServerResponse> getTaskStatus(ServerRequest serverRequest) {
        return ServerResponse.ok().body(Flux.fromIterable(virtualServerStatusRepo.findAll()), ClientDTO.class);
    }

    private <T> Mono<ServerResponse> postHandler(ServerRequest serverRequest, CrudRepository crudRepository, Class<T> classType) {
        return postHandler(serverRequest, crudRepository, classType, null);
    }

    private <T> Mono<ServerResponse> postHandler(ServerRequest serverRequest, CrudRepository crudRepository, Class<T> classType, BiConsumer<WrapJsonToObject<T>, String> handler) {
        cmpStatistic.incShareStatistic("WebRequest");
        Mono<String> bodyData = serverRequest.bodyToMono(String.class);

        return bodyData.flatMap(body -> {
            JsonResponse jRet = new JsonResponse();
            if (body != null && !body.isEmpty()) {
                try {
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

    @NonNull
    public Mono<ServerResponse> postClient(ServerRequest serverRequest) {
        return postHandler(serverRequest, clientRepo, ClientDTO.class);
    }

    @NonNull
    public Mono<ServerResponse> postServer(ServerRequest serverRequest) {
        return postHandler(serverRequest, serverRepo, ServerDTO.class);
    }

    @NonNull
    public Mono<ServerResponse> postTask(ServerRequest serverRequest) {
        return postHandler(serverRequest, taskRepo, TaskDTO.class, (obj, body) -> obj.getObject().setTask(body));
    }

    @NonNull
    public Mono<ServerResponse> postVirtualServer(ServerRequest serverRequest) {
        return postHandler(serverRequest, virtualServerRepo, VirtualServerDTO.class);
    }

    @NonNull
    public Mono<ServerResponse> postVirtualServerStatus(ServerRequest serverRequest) {
        return postHandler(serverRequest, virtualServerStatusRepo, VirtualServerStatusDTO.class);
    }

    @NonNull
    public Mono<ServerResponse> postTaskStatus(ServerRequest serverRequest) {
        return postHandler(serverRequest, taskStatusRepo, TaskStatusDTO.class);
    }

    public Mono<ServerResponse> patchHandler(ServerRequest serverRequest, BiConsumer<String, JsonResponse> consumer) {
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

    @NonNull
    public Mono<ServerResponse> patchTaskComplete(ServerRequest serverRequest) {
        return patchHandler(serverRequest, (body, jRet) -> {
            //Util.logConsole(Thread.currentThread(), "::patchTaskComplete " + body);
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
                            jRet.set(HttpStatus.EXPECTATION_FAILED, "Not found task: " + body);
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

                taskService.status("ROLLBACK", task, "Task failed. Remove VM: "+(virtualServerDTO.getIso() + "_" + task.getLinkIdVSrv())+" and restore task status = 0");

                task.incRetry();
                task.setStatus(0); //Пойдём в перенакат
            }
        }

    }


}