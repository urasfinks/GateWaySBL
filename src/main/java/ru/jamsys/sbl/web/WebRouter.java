package ru.jamsys.sbl.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

@Configuration(proxyBeanMethods = false)
public class WebRouter {

    @Bean
    public RouterFunction<ServerResponse> route(SblWebHandler sblWebHandler) {

        return RouterFunctions.route()
                .GET("/hello", accept(MediaType.TEXT_PLAIN), sblWebHandler::hello)
                .PUT("/Client", accept(MediaType.TEXT_PLAIN), sblWebHandler::putClient)
                .PUT("/Server", accept(MediaType.TEXT_PLAIN), sblWebHandler::putServer)
                .PUT("/VirtualServer", sblWebHandler::putVirtualServer)
                .PUT("/VirtualServerStatus", sblWebHandler::putVirtualServerStatus)
                .GET("/Client", sblWebHandler::getClient)
                .GET("/Server", sblWebHandler::getServer)
                .GET("/VirtualServer", sblWebHandler::getVirtualServer)
                .GET("/VirtualServerStatus", sblWebHandler::getVirtualServerStatus)
                .build();

    }
}