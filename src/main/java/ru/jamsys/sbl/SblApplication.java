package ru.jamsys.sbl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import ru.jamsys.sbl.component.CmpService;
import ru.jamsys.sbl.component.CmpServiceStabilizer;
import ru.jamsys.sbl.component.CmpStatistic;
import ru.jamsys.sbl.component.CmpStatisticCpu;
import ru.jamsys.sbl.jpa.dto.ClientDto;
import ru.jamsys.sbl.jpa.dto.ServerDto;
import ru.jamsys.sbl.jpa.dto.VirtualServerDto;
import ru.jamsys.sbl.jpa.repo.ClientRepo;
import ru.jamsys.sbl.jpa.repo.ServerRepo;
import ru.jamsys.sbl.jpa.repo.VirtualServerRepo;
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
        //t1();
        t2();


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

    public static void t2() {
        /*VirtualServerRepo serverRepo = context.getBean(VirtualServerRepo.class);
        Iterable<VirtualServerDto> all = serverRepo.findAll();
        for (VirtualServerDto item: all){
            System.out.println(item.toString());
        }*/

        /*ClientDto c1 = Util.jsonToObject("{\n" +
                "  \"mail\": \"urasfinks@yandex.ru\",\n" +
                "  \"login\": \"admin\",\n" +
                "  \"password\": \"12345\"\n" +
                "}", ClientDto.class);
        System.out.println(c1.toString());
        ClientRepo clientRepo = context.getBean(ClientRepo.class);
        clientRepo.save(c1);*/
        /*Srv s = new Srv();
        s.setName("Second");
        s.setIp("127.0.0.1");
        s.setStatus(0);
        srvRepo.save(s);*/
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
