package ru.jamsys.sbl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import ru.jamsys.sbl.component.CmpThreadStabilizer;
import ru.jamsys.sbl.component.CmpService;
import ru.jamsys.sbl.component.CmpStatistic;
import ru.jamsys.sbl.message.MessageImpl;
import ru.jamsys.sbl.supplier.SblServiceSupplier;
import ru.jamsys.sbl.thread.SblService;

import java.util.function.Consumer;

class SblServiceSupplierTest {

    static ConfigurableApplicationContext context;

    @BeforeAll
    static void beforeAll() {
        String[] args = new String[]{};
        context = SpringApplication.run(SblApplication.class, args);
        CmpStatistic cmpConsumerStatistic = context.getBean(CmpStatistic.class);
        cmpConsumerStatistic.setDebug(true);
        cmpConsumerStatistic.run();
        context.getBean(CmpThreadStabilizer.class).run();
    }

    @Test
    void overclocking() {
        run(1, 5, 6000L, 10, 5, clone ->
                Assertions.assertEquals(5, clone.getThreadCount(), "Должен был разогнаться до 5 потоков"));
    }

    @Test
    void overTps() {
        run(1, 20, 6000L,15, 5, clone ->
                Assertions.assertEquals(5, clone.getTpsOutput(), "Не должно превышать 5 тпс"));
    }

    @Test
    void overTps2() {
        run(1, 250, 3000L,300, 250, clone ->
                Assertions.assertEquals(5, clone.getTpsOutput(), "Не должно превышать 5 тпс"));
    }

    void run(int countThreadMin, int countThreadMax, long keepAlive, int sleep, int maxTps, Consumer<SblServiceStatistic> fnExpected) {
        SblService test = context.getBean(CmpService.class).instance("Test", countThreadMin, countThreadMax, keepAlive, 1000, ()->{
            UtilTest.sleepMillis(500);
            return new MessageImpl();
        });
        test.setDebug(true);
        test.setTpsInputMax(maxTps);
        UtilTest.sleepSec(sleep);
        SblServiceStatistic clone = test.getStatClone();
        if (clone != null) {
            fnExpected.accept(clone);
        }
        context.getBean(CmpService.class).shutdown("Test");
    }

    @Test
    void getNeedCountThread() {
        Assertions.assertEquals(125, SblServiceSupplier.getNeedCountThread(SblServiceStatistic.instanceSupplierTest(500,100,150, 1), 250, true), "#1");
        Assertions.assertEquals(63, SblServiceSupplier.getNeedCountThread(SblServiceStatistic.instanceSupplierTest(500,100,150, 125), 250, true), "#2");
        Assertions.assertEquals(0, SblServiceSupplier.getNeedCountThread(SblServiceStatistic.instanceSupplierTest(500,100,0, 1), 250, true), "#3");
        Assertions.assertEquals(10, SblServiceSupplier.getNeedCountThread(SblServiceStatistic.instanceSupplierTest(500,50,10, 1), 250, true), "#4");
        Assertions.assertEquals(10, SblServiceSupplier.getNeedCountThread(SblServiceStatistic.instanceSupplierTest(0,50,10, 1), 250, true), "#5");
    }

}