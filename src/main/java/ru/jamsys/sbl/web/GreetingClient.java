package ru.jamsys.sbl.web;

import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class GreetingClient {

    private final WebClient client;

    // Spring Boot auto-configures a `WebClient.Builder` instance with nice defaults and customizations.
    // We can use it to create a dedicated `WebClient` for our component.

    public GreetingClient(WebClient.Builder builder) {
        this.client = builder.baseUrl("http://elasticsearch2:9200").build();
    }

    public Mono<String> getMessage(String data) {
        return this.client.post().uri("/statistic/_doc")
                .body(BodyInserters.fromValue(data))
                .headers(httpHeaders -> httpHeaders.set("Content-Type", "application/json"))
                .retrieve()
                .bodyToMono(String.class);
    }

}