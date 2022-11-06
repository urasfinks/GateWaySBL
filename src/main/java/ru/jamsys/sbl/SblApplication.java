package ru.jamsys.sbl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import ru.jamsys.sbl.component.CmpService;
import ru.jamsys.sbl.component.CmpServiceStabilizer;
import ru.jamsys.sbl.component.CmpStatistic;
import ru.jamsys.sbl.component.CmpStatisticCpu;
import ru.jamsys.sbl.jpa.dto.VirtualServerDto;
import ru.jamsys.sbl.jpa.repo.VirtualServerRepo;
import ru.jamsys.sbl.message.MessageImpl;
import ru.jamsys.sbl.service.SblService;

import java.sql.Timestamp;
import java.util.List;

@SpringBootApplication
public class SblApplication {

    static ConfigurableApplicationContext context;

    public static void initContext(ConfigurableApplicationContext context, boolean debug) {
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
        //t2();
    }

    public static void t2() {

    }

    public static void t1() {
        VirtualServerRepo virtualServerRepo = context.getBean(VirtualServerRepo.class);
        SblService test = context.getBean(CmpService.class).instance("Scheduler", 1, 10, 60, 5000, () -> {
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            List<VirtualServerDto> removeList = virtualServerRepo.getRemove(timestamp);
            if (removeList.size() > 0) {
                MessageImpl message = new MessageImpl();
                message.setHeader("removeList", removeList);
                return message;
            } else {
                return null;
            }
        }, message -> {
            List<VirtualServerDto> removeList = message.getHeader("removeList");
            for(VirtualServerDto item: removeList){
                item.setStatus(-1);
                virtualServerRepo.save(item);
            }
            //Util.logConsole(Thread.currentThread(), removeList.toString());
        });
        test.setTpsInputMax(1);
        test.setDebug(false);
    }

}
