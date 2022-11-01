package ru.jamsys.sbl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import ru.jamsys.sbl.component.CmpService;
import ru.jamsys.sbl.component.CmpThreadStabilizer;
import ru.jamsys.sbl.component.CmpStatistic;
import ru.jamsys.sbl.consumer.SblServiceConsumer;
import ru.jamsys.sbl.consumer.SblConsumerShutdownException;
import ru.jamsys.sbl.consumer.SblConsumerTpsOverflowException;
import ru.jamsys.sbl.message.Message;
import ru.jamsys.sbl.message.MessageImpl;
import ru.jamsys.sbl.thread.SblService;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

class SblServiceConsumerTest {

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
    void overclocking() { //Проверяем разгон потоков под рост задач
        run(1, 5, 60000L, 2, 10, 10, -1, clone ->
                Assertions.assertEquals(5, clone.getThreadCount(), "Кол-во потоков должно быть 5")
        );
    }

    @Test
    void damping() { //Проверяем удаление потоков после ненадобности
        run(1, 5, 6000L, 2, 5, 17, -1, clone ->
                Assertions.assertEquals(1, clone.getThreadCount(), "Должен остаться только 1 поток")
        );
    }

    @Test
    void timeout() { //Проверяем время жизни потоков, после теста они должны все статься
        run(1, 5, 16000L, 2, 5, 17, -1, clone ->
                Assertions.assertTrue(clone.getThreadCount() > 1, "Кол-во потокв дожно быть больше одного")
        );
    }

    @Test
    void summaryCount() { //Проверяем, что сообщения все обработаны при большом кол-ве потоков
        run(1, 1000, 16000L, 1, 5000, 10, -1, clone ->
                Assertions.assertEquals(1000, clone.getThreadCount(), "Кол-во потокв дожно быть 1000")
        );
    }

    @Test
    void tpsInputMax() { //Проверяем, что в очередь не падает больше 5 сообщений в секунду
        run(1, 1, 16000L, 2, 20, 12, 5, clone ->
                Assertions.assertTrue(clone.getQueueSize() < 10, "Очередь слишком большая, для максимальных 5 тпс")
        );
    }

    void run(int countThreadMin, int countThreadMax, long keepAlive, int countIteration, int countMessage, int sleep, int tpsInputMax, Consumer<SblServiceStatistic> fnExpected) {
        Util.logConsole(Thread.currentThread(), "Start test");
        AtomicInteger c = new AtomicInteger(0);
        SblService test = context.getBean(CmpService.class).instance("Test", countThreadMin, countThreadMax, keepAlive, (msg) -> {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            c.incrementAndGet();
            //Util.logConsole("[" + c.incrementAndGet() + "] " + msg.getCorrelation());
        });
        test.setDebug(true);
        test.setTpsInputMax(tpsInputMax);

        AtomicInteger realInsert = new AtomicInteger(0);

        Thread t1 = new Thread(() -> {
            int count = 0;
            while (true) {
                count++;
                if (count == countIteration + 1) {
                    break;
                }
                for (int i = 0; i < countMessage; i++) {
                    Message message = new MessageImpl();
                    try {
                        ((SblServiceConsumer) test).accept(message);
                        realInsert.incrementAndGet();
                    } catch (SblConsumerShutdownException | SblConsumerTpsOverflowException e) {
                        System.out.println(e.toString());
                    }
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        t1.start();
        UtilTest.sleepSec(sleep);
        Assertions.assertEquals(realInsert.get(), c.get(), "Не все задачи были обработаны");
        SblServiceStatistic clone = test.getStatClone();
        if (clone != null) {
            fnExpected.accept(clone);
        }
        context.getBean(CmpService.class).shutdown("Test");
    }

    @Test
    void getNeedCountThread() {
        Assertions.assertEquals(0, SblServiceConsumer.getNeedCountThread(SblServiceStatistic.instanceConsumerTest(100,1,0, 0), true), "#1");
        Assertions.assertEquals(1, SblServiceConsumer.getNeedCountThread(SblServiceStatistic.instanceConsumerTest(10,1,10, 0), true), "#2");
        Assertions.assertEquals(2, SblServiceConsumer.getNeedCountThread(SblServiceStatistic.instanceConsumerTest(10,1,20, 0), true), "#3");
        Assertions.assertEquals(3, SblServiceConsumer.getNeedCountThread(SblServiceStatistic.instanceConsumerTest(10,1,30, 0), true), "#4");
        Assertions.assertEquals(0, SblServiceConsumer.getNeedCountThread(SblServiceStatistic.instanceConsumerTest(10,2,0, 0), true), "#5");
        Assertions.assertEquals(1, SblServiceConsumer.getNeedCountThread(SblServiceStatistic.instanceConsumerTest(10,2,5, 0), true), "#6");
        Assertions.assertEquals(2, SblServiceConsumer.getNeedCountThread(SblServiceStatistic.instanceConsumerTest(10,2,10, 0), true), "#7");
        Assertions.assertEquals(6, SblServiceConsumer.getNeedCountThread(SblServiceStatistic.instanceConsumerTest(10,2,30, 0), true), "#8");
        Assertions.assertEquals(1, SblServiceConsumer.getNeedCountThread(SblServiceStatistic.instanceConsumerTest(100,2,30, 0), true), "#9");
        Assertions.assertEquals(1, SblServiceConsumer.getNeedCountThread(SblServiceStatistic.instanceConsumerTest(100,3,30, 0), true), "#10");
        Assertions.assertEquals(9, SblServiceConsumer.getNeedCountThread(SblServiceStatistic.instanceConsumerTest(100,3,300, 0), true), "#11");

        //Плохие сценарии
        Assertions.assertEquals(300, SblServiceConsumer.getNeedCountThread(SblServiceStatistic.instanceConsumerTest(0,1,300, 0), true), "#12");
        Assertions.assertEquals(1, SblServiceConsumer.getNeedCountThread(SblServiceStatistic.instanceConsumerTest(10,0,10, 0), true), "#13");
        Assertions.assertEquals(10, SblServiceConsumer.getNeedCountThread(SblServiceStatistic.instanceConsumerTest(1,0,10, 0), true), "#13");
        Assertions.assertEquals(10, SblServiceConsumer.getNeedCountThread(SblServiceStatistic.instanceConsumerTest(1,1000,10, 100), true), "#14");
    }

}