package ru.jamsys.sbl.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Component
public class GreetingClient {

    private WebClient client = null;

    private Environment env;

    @Autowired
    public void setEnv(Environment env) {
        this.env = env;
    }

    WebClient.Builder builder;

    @Autowired
    public void setBuilder(WebClient.Builder builder) {
        this.builder = builder;
    }

    public Mono<String> getMessage(String data) throws Exception {
        if (client == null) {
            if (env.getProperty("elk.url") == null) {
                throw new Exception("Properties elk.url is empty");
            }
            this.client = builder.baseUrl(env.getProperty("elk.url")).build();
        }
        return this.client.post().uri("/statistic/_doc")
                .body(BodyInserters.fromValue(data))
                .headers(httpHeaders -> httpHeaders.set("Content-Type", "application/json"))
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<String> nettyRequest(String host, String uri, String data, long secTimeout) {
        return builder
                //.clientConnector(new ReactorClientHttpConnector(client))
                .baseUrl(host)
                .build()
                .post()
                .uri(uri)
                .body(BodyInserters.fromValue(data))
                .headers(httpHeaders -> httpHeaders.set("Content-Type", "application/json"))
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(secTimeout));
    }

}