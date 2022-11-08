package ru.jamsys.sbl.web;

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
import ru.jamsys.sbl.jpa.repo.ClientRepo;
import ru.jamsys.sbl.jpa.repo.ServerRepo;
import ru.jamsys.sbl.jpa.repo.VirtualServerRepo;
import ru.jamsys.sbl.jpa.repo.VirtualServerStatusRepo;

import java.util.Optional;

@Component
public class SblWebHandler {

    ClientRepo clientRepo;
    ServerRepo serverRepo;
    VirtualServerRepo virtualServerRepo;
    VirtualServerStatusRepo virtualServerStatusRepo;
    CmpStatistic cmpStatistic;

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
    public Mono<ServerResponse> hello(ServerRequest request) {
        return ServerResponse.ok().contentType(MediaType.TEXT_PLAIN)
                .body(BodyInserters.fromValue("Hello world"));
    }

    @NonNull
    public Mono<ServerResponse> getClient(ServerRequest serverRequest) {
        return ServerResponse.ok().body(Flux.fromIterable(clientRepo.findAll()), ClientDTO.class);
    }

    @NonNull
    public Mono<ServerResponse> getServer(ServerRequest serverRequest) {
        return ServerResponse.ok().body(Flux.fromIterable(serverRepo.findAll()), ClientDTO.class);
    }

    @NonNull
    public Mono<ServerResponse> getVirtualServer(ServerRequest serverRequest) {
        return ServerResponse.ok().body(Flux.fromIterable(virtualServerRepo.findAll()), ClientDTO.class);
    }

    @NonNull
    public Mono<ServerResponse> getVirtualServerStatus(ServerRequest serverRequest) {
        return ServerResponse.ok().body(Flux.fromIterable(virtualServerStatusRepo.findAll()), ClientDTO.class);
    }

    private <T> Mono<ServerResponse> shareHandler(ServerRequest serverRequest, CrudRepository crudRepository, Class<T> classType) {
        cmpStatistic.incShareStatistic("WebRequest");
        Mono<String> bodyData = serverRequest.bodyToMono(String.class);

        return bodyData.flatMap(body -> {
            JsonResponse jret = new JsonResponse(HttpStatus.OK, "");
            if (body != null && !body.isEmpty()) {
                try {
                    WrapJsonToObject<T> wrapJsonToObject = Util.jsonToObject(body, classType);
                    if (wrapJsonToObject.getException() == null) {
                        crudRepository.save(wrapJsonToObject.getObject());
                    } else {
                        jret.set(HttpStatus.EXPECTATION_FAILED, wrapJsonToObject.getException().toString());
                    }
                } catch (Exception e) {
                    jret.set(HttpStatus.EXPECTATION_FAILED, e.toString());
                }
            } else {
                jret.set(HttpStatus.EXPECTATION_FAILED, "Empty request");
            }
            if (!jret.status.equals(HttpStatus.OK)) {
                cmpStatistic.incShareStatistic("WebError");
            }
            String s = Util.jsonObjectToStringPretty(jret);

            return ServerResponse.status(jret.status).body(BodyInserters.fromValue(Optional.ofNullable(s).orElse("{}")));
        });
    }

    @NonNull
    public Mono<ServerResponse> putClient(ServerRequest serverRequest) {
        return shareHandler(serverRequest, clientRepo, ClientDTO.class);
    }

    @NonNull
    public Mono<ServerResponse> putServer(ServerRequest serverRequest) {
        return shareHandler(serverRequest, serverRepo, ServerDTO.class);
    }

    @NonNull
    public Mono<ServerResponse> putVirtualServer(ServerRequest serverRequest) {
        return shareHandler(serverRequest, virtualServerRepo, VirtualServerDTO.class);
    }

    @NonNull
    public Mono<ServerResponse> putVirtualServerStatus(ServerRequest serverRequest) {
        return shareHandler(serverRequest, virtualServerStatusRepo, VirtualServerStatusDTO.class);
    }

}