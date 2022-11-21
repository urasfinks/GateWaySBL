package ru.jamsys.sbl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import ru.jamsys.sbl.component.CmpService;
import ru.jamsys.sbl.component.CmpServiceStabilizer;
import ru.jamsys.sbl.component.CmpStatistic;
import ru.jamsys.sbl.component.CmpStatisticCpu;
import ru.jamsys.sbl.jpa.service.PingService;
import ru.jamsys.sbl.jpa.service.StatisticService;
import ru.jamsys.sbl.jpa.service.TaskService;
import ru.jamsys.sbl.service.SblService;

@SpringBootApplication
public class SblApplication {

    public static ConfigurableApplicationContext context;

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
    }

    public static void t1() {
        TaskService taskService = context.getBean(TaskService.class);
        SblService schedulerTask = context.getBean(CmpService.class).instance("SchedulerTask", 1, 1, 60, 5000, taskService::exec, null);
        schedulerTask.setTpsInputMax(1);
        schedulerTask.setDebug(false);

        PingService pingService = context.getBean(PingService.class);
        SblService schedulerPing = context.getBean(CmpService.class).instance("SchedulerPing", 1, 1, 60, 3000, pingService::exec, null);
        schedulerPing.setTpsInputMax(1);
        schedulerPing.setDebug(false);

        StatisticService statisticService = context.getBean(StatisticService.class);
        SblService schedulerStatistic = context.getBean(CmpService.class).instance("SchedulerStatistic", 1, 1, 60, 1000, statisticService::exec, null);
        schedulerStatistic.setTpsInputMax(1);
        schedulerStatistic.setDebug(false);

    }

}
