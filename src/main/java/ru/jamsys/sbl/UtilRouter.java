package ru.jamsys.sbl;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class UtilRouter {

    public static String addPortForwarding(String routerIp, String nameRule, String internetPort, String localIp, String localPort) throws IOException, InterruptedException {

        /*String routerIp = "192.168.0.1";
        String nameRule = "Tst1";
        String localIp = "192.168.0.106";
        String localPort = "3001";
        String internetPort = "22001";*/


            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + routerIp + "/cgi-bin/luci/;stok=/login?form=login"))
                    .POST(HttpRequest.BodyPublishers.ofString("data=%7B%22method%22%3A%22login%22%2C%22params%22%3A%7B%22username%22%3A%22kingston%22%2C%22password%22%3A%22c4e0655cb599079eb9d014c07feec052bd5fd16a00ed434f6d984cbcf839e258f10155d590517fcbb91c4259dc139fdd05f07551825db96c6a65822423ee4431b19b0b29b414064a763d5371d3bfbe4efdd48043e83e33b8a9bf36169514ca1bd7c80acb20a813f6315e25650bad11b16cc95b35d6e4af0fbd68d0c477e273ff%22%7D%7D"))
                    //.setHeader("Content-Length","374")
                    .setHeader("Accept", "application/json, text/javascript, */*; q=0.01")
                    .setHeader("X-Requested-With", "XMLHttpRequest")
                    .setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Safari/537.36")
                    .setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .setHeader("Origin", "http://" + routerIp)
                    .setHeader("Referer", "http://" + routerIp + "/webpages/login.html")
                    .setHeader("Accept-Encoding", "gzip, deflate")
                    .setHeader("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
                    .setHeader("Cookie", "sysauth=a2caa3a833ca3dcf951cdf97d980849d")
                    //.setHeader("Connection","keep-alive")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            Map<String, Object> x = (Map<String, Object>) new Gson().fromJson(response.body(), Map.class).get("result");
            String stok = (String) x.get("stok");
            String sysAuth = response.headers().map().get("set-cookie").get(0).split(";")[0];
            System.out.println("Cookie: " + sysAuth + "; Stok: " + stok);

            HttpRequest request2 = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + routerIp + "/cgi-bin/luci/;stok=" + stok + "/admin/nat?form=vs"))
                    .POST(HttpRequest.BodyPublishers.ofString("data=" + URLEncoder.encode("{\"method\":\"add\",\"params\":{\"index\":11,\"old\":\"add\",\"new\":{\"name\":\"" + nameRule + "\",\"interface\":\"WAN1\",\"external_port\":\"" + internetPort + "\",\"internal_port\":\"" + localPort + "\",\"ipaddr\":\"" + localIp + "\",\"protocol\":\"ALL\",\"enable\":\"on\"},\"key\":\"add\"}}", StandardCharsets.UTF_8)))
                    //.setHeader("Content-Length","374")
                    .setHeader("Accept", "application/json, text/javascript, */*; q=0.01")
                    .setHeader("X-Requested-With", "XMLHttpRequest")
                    .setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Safari/537.36")
                    .setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .setHeader("Origin", "http://" + routerIp)
                    .setHeader("Referer", "http://" + routerIp + "/webpages/index.html")
                    .setHeader("Accept-Encoding", "gzip, deflate")
                    .setHeader("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
                    .setHeader("Cookie", sysAuth)
                    //.setHeader("Connection","keep-alive")
                    .build();

            HttpResponse<String> response2 = client.send(request2, HttpResponse.BodyHandlers.ofString());
            return response2.body();
    }

}
