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

        /*
        * POST - Исход вставки может быть разный (новый объект)
        * PUT  - Идемпотентность вставки приводит к отному и тому же резултату (относим сюда UPDATE) !замена целиком!
        * PATCH - Частичная замена свойств, но также идемпотентная
        * */
        return RouterFunctions.route()
                .GET("/Client", sblWebHandler::getClient)
                .GET("/Server", sblWebHandler::getServer)
                .GET("/VirtualServer", sblWebHandler::getVirtualServer)
                .GET("/VirtualServerStatus", sblWebHandler::getVirtualServerStatus)
                .GET("/TaskStatus", sblWebHandler::getTaskStatus)
                .GET("/healthCheck", accept(MediaType.TEXT_PLAIN), sblWebHandler::healthCheck)
                .GET("/Billid", sblWebHandler::getBillid)
                .GET("/Actions", sblWebHandler::getActions)
                .GET("/DeleteTime", sblWebHandler::getDeleteTime)
                .GET("/ActionsByIdClient/{id}", sblWebHandler::getActionsByIdClient)
                .GET("/DeleteTimeByIdVirtualServer/{id}", sblWebHandler::getDeleteTimeByIdVirtualServer)
                .GET("/BillidByIdClient/{id}", accept(MediaType.TEXT_PLAIN), sblWebHandler::getBillidByIdClient)
                .GET("/PromoByPromoCode/{id}", accept(MediaType.TEXT_PLAIN), sblWebHandler::getPromoCode)
                .GET("/TaskByIdClient/{id}", accept(MediaType.TEXT_PLAIN), sblWebHandler::getTaskByIdClient)
                .GET("/VirtualServerByIdClient/{id}", accept(MediaType.TEXT_PLAIN), sblWebHandler::getVirtualServerByIdClient)

                .GET("/VdsAvailable", sblWebHandler::getVdsAvailable)


                .POST("/api", sblWebHandler::getApi)
                .POST("/Actions", accept(MediaType.TEXT_PLAIN), sblWebHandler::postActions)
                .POST("/DeleteTime", accept(MediaType.TEXT_PLAIN), sblWebHandler::postDeleteTime)
                .POST("/Client", accept(MediaType.TEXT_PLAIN), sblWebHandler::postClient)
                .POST("/Billid", accept(MediaType.TEXT_PLAIN), sblWebHandler::postBillid)
                .POST("/Promo", accept(MediaType.TEXT_PLAIN), sblWebHandler::postPromo)
                .POST("/Server", accept(MediaType.TEXT_PLAIN), sblWebHandler::postServer)
                .POST("/Task", accept(MediaType.TEXT_PLAIN), sblWebHandler::postTask)
                //.POST("/VirtualServer", sblWebHandler::postVirtualServer) //Only native API
                .POST("/VirtualServerStatus", sblWebHandler::postVirtualServerStatus)
                .POST("/TaskStatus", sblWebHandler::postTaskStatus)

                .PATCH("/TaskComplete", accept(MediaType.TEXT_PLAIN), sblWebHandler::patchTaskComplete)
                .PATCH("/VirtualServer/{id}", accept(MediaType.TEXT_PLAIN), sblWebHandler::patchVirtualServer)
                .build();

    }
}