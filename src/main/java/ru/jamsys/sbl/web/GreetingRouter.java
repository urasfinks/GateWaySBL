package ru.jamsys.sbl.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

@Configuration(proxyBeanMethods = false)
public class GreetingRouter {

    @Bean
    public RouterFunction<ServerResponse> route(GreetingHandler greetingHandler) {

        return RouterFunctions.route()
                .GET("/hello", accept(MediaType.TEXT_PLAIN), greetingHandler::hello)
                .PUT("/Client", accept(MediaType.TEXT_PLAIN), greetingHandler::putClient)
                .PUT("/Server", accept(MediaType.TEXT_PLAIN), greetingHandler::putServer)
                .PUT("/VirtualServer", greetingHandler::putVirtualServer)
                .PUT("/VirtualServerStatus", greetingHandler::putVirtualServerStatus)
                .build();

    }
}