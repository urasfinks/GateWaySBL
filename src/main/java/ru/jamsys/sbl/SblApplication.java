package ru.jamsys.sbl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import ru.jamsys.sbl.component.CmpServiceStabilizer;
import ru.jamsys.sbl.component.CmpStatistic;
import ru.jamsys.sbl.component.CmpStatisticCpu;
import java.util.UUID;

@SpringBootApplication
public class SblApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(SblApplication.class, args);
        context.getBean(CmpStatistic.class).run();
        context.getBean(CmpServiceStabilizer.class).run();
        context.getBean(CmpStatisticCpu.class);

//        GreetingClient greetingClient = context.getBean(GreetingClient.class);
//        System.out.println(">> message = " + greetingClient.getMessage("{\"timestamp\":1667456542,\"cpu\":0}").block());

//        new Thread(() -> {
//            for (int i = 0; i < 100000000; i++) {
//                String x = UUID.randomUUID().toString();
//                //System.out.println(x);
//            }
//            try {
//                Thread.sleep(3000);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }).start();


    }


}
