package ru.jamsys.sbl.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Mono;
import ru.jamsys.sbl.Util;
import ru.jamsys.sbl.WrapJsonToObject;
import ru.jamsys.sbl.component.CmpStatistic;
import ru.jamsys.sbl.jpa.dto.ClientDto;
import ru.jamsys.sbl.jpa.dto.ServerDto;
import ru.jamsys.sbl.jpa.dto.VirtualServerDto;
import ru.jamsys.sbl.jpa.dto.VirtualServerStatusDto;
import ru.jamsys.sbl.jpa.repo.ClientRepo;
import ru.jamsys.sbl.jpa.repo.ServerRepo;
import ru.jamsys.sbl.jpa.repo.VirtualServerRepo;
import ru.jamsys.sbl.jpa.repo.VirtualServerStatusRepo;

import java.util.Optional;

@Component
public class GreetingHandler {

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

    public Mono<ServerResponse> hello(ServerRequest request) {
        return ServerResponse.ok().contentType(MediaType.TEXT_PLAIN)
                .body(BodyInserters.fromValue("Hello world"));
    }

    public static class JRet {
        public HttpStatus status;
        public String data;
    }

    private <T> Mono<ServerResponse> shareHandler(ServerRequest serverRequest, CrudRepository cr, Class<T> t) {
        cmpStatistic.incShareStatistic("WebRequest");
        Mono<String> bodyData = serverRequest.bodyToMono(String.class);

        return bodyData.flatMap(body -> {
            JRet jret = new JRet();
            jret.status = HttpStatus.OK;
            jret.data = "";
            if (body != null && !body.isEmpty()) {
                try {
                    WrapJsonToObject c1 = Util.jsonToObject(body, t);
                    if (c1.getException() == null) {
                        cr.save(c1.getObject());
                    } else {
                        jret.status = HttpStatus.EXPECTATION_FAILED;
                        jret.data = c1.getException().toString();
                    }
                } catch (Exception e) {
                    jret.status = HttpStatus.EXPECTATION_FAILED;
                    jret.data = e.toString();
                }
            } else {
                jret.status = HttpStatus.EXPECTATION_FAILED;
                jret.data = "Empty request";
            }
            if (!jret.status.equals(HttpStatus.OK)) {
                cmpStatistic.incShareStatistic("WebError");
            }
            return ServerResponse.status(jret.status).body(BodyInserters.fromValue(Optional.of(Util.jsonObjectToStringPretty(jret)).orElse("{}")));
        });
    }

    public Mono<ServerResponse> putClient(ServerRequest serverRequest) {
        return shareHandler(serverRequest, clientRepo, ClientDto.class);
    }

    public Mono<ServerResponse> putServer(ServerRequest serverRequest) {
        return shareHandler(serverRequest, serverRepo, ServerDto.class);
    }

    public Mono<ServerResponse> putVirtualServer(ServerRequest serverRequest) {
        return shareHandler(serverRequest, virtualServerRepo, VirtualServerDto.class);
    }

    public Mono<ServerResponse> putVirtualServerStatus(ServerRequest serverRequest) {
        return shareHandler(serverRequest, virtualServerStatusRepo, VirtualServerStatusDto.class);
    }
}