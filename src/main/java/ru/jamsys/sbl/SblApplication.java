package ru.jamsys.sbl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import ru.jamsys.sbl.component.CmpService;
import ru.jamsys.sbl.component.CmpServiceStabilizer;
import ru.jamsys.sbl.component.CmpStatistic;
import ru.jamsys.sbl.component.CmpStatisticCpu;
import ru.jamsys.sbl.message.MessageImpl;
import ru.jamsys.sbl.service.SblService;

@SpringBootApplication
public class SblApplication {

    static ConfigurableApplicationContext context;

    public static void initContext(ConfigurableApplicationContext context, boolean debug){
        CmpStatistic cmpStatistic = context.getBean(CmpStatistic.class);
        cmpStatistic.setDebug(debug);
        cmpStatistic.run();

        CmpServiceStabilizer cmpServiceStabilizer = context.getBean(CmpServiceStabilizer.class);
        cmpServiceStabilizer.setDebug(debug);
        cmpServiceStabilizer.run();

        context.getBean(CmpStatisticCpu.class);
    }

    public static void main(String[] args) {
        context = SpringApplication.run(SblApplication.class, args);
        initContext(context, false);
        t1();

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

    public static void t1(){
        SblService test = context.getBean(CmpService.class).instance("Test", 1, 250, 60, 333, () -> {
            return new MessageImpl();
        }, message -> {
            Util.logConsole(Thread.currentThread(), message.getCorrelation());
        });
        test.setTpsInputMax(5);
        test.setDebug(true);
    }

}
