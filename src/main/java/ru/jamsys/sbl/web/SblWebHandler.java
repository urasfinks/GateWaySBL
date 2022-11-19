package ru.jamsys.sbl.web;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.jamsys.sbl.Util;
import ru.jamsys.sbl.WrapJsonToObject;
import ru.jamsys.sbl.component.CmpStatistic;
import ru.jamsys.sbl.jpa.dto.*;
import ru.jamsys.sbl.jpa.repo.*;

import java.util.Map;
import java.util.function.BiConsumer;

@Component
public class SblWebHandler {

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
        cmpStatistic.incShareStatistic("WebRequestPost");
        Mono<String> bodyData = serverRequest.bodyToMono(String.class);

        return bodyData.flatMap(body -> {
            JsonResponse jRet = new JsonResponse();
            if (body != null && !body.isEmpty()) {
                try {
                    WrapJsonToObject<T> wrapJsonToObject = handler != null ?
                            Util.jsonToObjectOverflowProperties(body, classType)
                            : Util.jsonToObject(body, classType);
                    if (wrapJsonToObject.getException() == null) {
                        if (handler != null) {
                            handler.accept(wrapJsonToObject, body);
                        }
                        T o = wrapJsonToObject.getObject();
                        crudRepository.save(o);
                        //jRet.addData(o.getClass().getName().replace("DTO", ""), o);
                        String[] split = o.getClass().getName().split("\\.");
                        jRet.addData(split[split.length - 1].replace("DTO", ""), o);
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
        cmpStatistic.incShareStatistic("WebRequestPatch");
        Mono<String> bodyData = serverRequest.bodyToMono(String.class);
        return bodyData.flatMap(body -> {
            JsonResponse jRet = new JsonResponse();
            if (body != null && !body.isEmpty()) {
                try {
                    consumer.accept(body, jRet);
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
            System.out.println(body);
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
                    jRet.set(HttpStatus.EXPECTATION_FAILED, "idTask not found is json: " + body);
                    next = false;
                }
            }

            if (next) {
                if (!req.containsKey("status")) {
                    jRet.set(HttpStatus.EXPECTATION_FAILED, "status not found in json: " + body);
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
                    Double x2 = (Double) req.get("status");
                    serverDTO.setStatus(x2.intValue());
                    serverRepo.save(serverDTO);
                } catch (Exception e) {
                    jRet.set(HttpStatus.EXPECTATION_FAILED, "Set status server exception: " + e);
                    next = false;
                }
            }

            if (next) {
                if (task.getLinkIdVSrv() == null) {
                    jRet.set(HttpStatus.EXPECTATION_FAILED, "Task Virtual server is null");
                    next = false;
                }
            }

            VirtualServerDTO virtualServerDTO = null;
            if (next) {
                try {
                    virtualServerDTO = virtualServerRepo.findById(task.getLinkIdVSrv()).orElse(null);
                    if (virtualServerDTO == null) {
                        jRet.set(HttpStatus.EXPECTATION_FAILED, "Task VirtualServer is null");
                        next = false;
                    }
                } catch (Exception e) {
                    jRet.set(HttpStatus.EXPECTATION_FAILED, "Get VirtualServer exception: " + e);
                    next = false;
                }
            }

            if (next) {
                virtualServerDTO.setStatus(1);
                virtualServerRepo.save(virtualServerDTO);
            }

        });
    }

    public Mono<ServerResponse> patchServer(ServerRequest serverRequest) {
        return patchHandler(serverRequest, (body, jRet) -> {
            WrapJsonToObject<ServerDTO> wrapJsonToObject = Util.jsonToObject(body, ServerDTO.class);
            if (wrapJsonToObject.getException() == null) {
                ServerDTO byId = serverRepo.findById(wrapJsonToObject.getObject().getId()).orElse(null);
                if (byId != null) {
                    byId.patch(wrapJsonToObject.getObject());
                    serverRepo.save(byId);
                } else {
                    jRet.set(HttpStatus.EXPECTATION_FAILED, "Not found server by id");
                }
            } else {
                jRet.set(HttpStatus.EXPECTATION_FAILED, wrapJsonToObject.getException().toString());
            }
        });
    }

}