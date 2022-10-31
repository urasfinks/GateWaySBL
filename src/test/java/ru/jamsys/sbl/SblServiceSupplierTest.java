package ru.jamsys.sbl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import ru.jamsys.sbl.component.CmpHelper;
import ru.jamsys.sbl.component.CmpService;
import ru.jamsys.sbl.component.CmpStatistic;
import ru.jamsys.sbl.message.MessageImpl;
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
        context.getBean(CmpHelper.class).run();
    }

    @Test
    void test() {
        run(1,5, 6000L, 1, 5, 10, clone ->
                Assertions.assertTrue(clone.getQueueSize() < 10, "Очередь слишком большая, для максимальных 5 тпс"));
    }

    void run(int countThreadMin, int countThreadMax, long keepAlive, int countIteration, int countMessage, int sleep, Consumer<SblServiceStatistic> fnExpected) {
        SblService test = context.getBean(CmpService.class).instance("Test", countThreadMin, countThreadMax, keepAlive, 1000, MessageImpl::new);
        test.setDebug(true);
        test.setTpsMainMax(5);
        UtilTest.sleep(sleep);
    }

}