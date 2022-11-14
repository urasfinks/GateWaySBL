package ru.jamsys.sbl.web;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

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

    public Mono<String> getMessageCustom(String host, String uri, String data) {
//        TcpClient tcpClient = TcpClient.create()
//                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
//                .doOnConnected(connection ->
//                        connection.addHandlerLast(new ReadTimeoutHandler(10))
//                                .addHandlerLast(new WriteTimeoutHandler(10)));

        HttpClient httpClient = HttpClient
                .create()
                .disableRetry(true)
                .responseTimeout(Duration.ofMillis(1000));

        return builder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(host)
                .build()
                .post()
                .uri(uri)
                .body(BodyInserters.fromValue(data))
                .headers(httpHeaders -> httpHeaders.set("Content-Type", "application/json"))
                .retrieve()
                .bodyToMono(String.class);
    }

}