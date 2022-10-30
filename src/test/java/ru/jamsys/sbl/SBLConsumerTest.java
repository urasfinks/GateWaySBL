package ru.jamsys.sbl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import ru.jamsys.sbl.component.ConsumerComponent;
import ru.jamsys.sbl.component.Helper;
import ru.jamsys.sbl.component.Statistic;
import ru.jamsys.sbl.consumer.SBLConsumer;
import ru.jamsys.sbl.consumer.ConsumerStatistic;
import ru.jamsys.sbl.message.Message;
import ru.jamsys.sbl.message.MessageImpl;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

class SBLConsumerTest {

    static ConfigurableApplicationContext context;

    @BeforeAll
    static void beforeAll() {
        String[] args = new String[]{};
        context = SpringApplication.run(SblApplication.class, args);
        context.getBean(Statistic.class).run();
        context.getBean(Helper.class).run();
    }

    @Test
    void handleOverclocking() { //Проверяем разгон потоков под рост задач
        run(1, 5, 60000L, 2, 5, 10, (clone) -> {
            Assertions.assertEquals(5, clone.getCountThread(), "Кол-во потоков должно быть 5");
        });
    }

    @Test
    void handleDamping() { //Проверяем удаление потоков после ненадобности
        run(1, 5, 6000L, 2, 5, 17, (clone) -> {
            Assertions.assertEquals(1, clone.getCountThread(), "Должен остаться только 1 поток");
        });
    }

    @Test
    void handleTimeout() { //Проверяем время жизни потоков, после теста они должны все статься
        run(1, 5, 16000L, 2, 5, 17, (clone) -> {
            Assertions.assertTrue(clone.getCountThread() > 1, "Кол-во потокв дожно быть больше одного");
        });
    }

    @Test
    void handleSummaryCount() { //Проверяем, что сообщения все обработаны при большом кол-ве потоков
        run(1, 1000, 16000L, 1, 5000, 15, (clone) -> {
            Assertions.assertTrue(clone.getCountThread() == 1000, "Кол-во потокв дожно быть 1000");
        });
    }

    void run(int countThreadMin, int countThreadMax, long keepAlive, int countIteration, int countMessage, int sleep, Consumer<ConsumerStatistic> fnExpected) {
        Util.logConsole("Start test");
        AtomicInteger c = new AtomicInteger(0);
        SBLConsumer test = context.getBean(ConsumerComponent.class).instance("Test", countThreadMin, countThreadMax, keepAlive, (msg) -> {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            c.incrementAndGet();
            //Util.logConsole("[" + c.incrementAndGet() + "] " + msg.getCorrelation());
        });
        test.setDebug(true);

        Thread t1 = new Thread(() -> {
            int count = 0;
            while (true) {
                count++;
                if (count == countIteration + 1) {
                    break;
                }
                for (int i = 0; i < countMessage; i++) {
                    Message message = new MessageImpl();
                    test.handle(message);
                }
                try {
                    Thread.sleep(5000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        t1.start();
        sleep(sleep);
        Assertions.assertEquals(countMessage * countIteration, c.get(), "Не все задачи были обработаны");
        try {
            ConsumerStatistic clone = (ConsumerStatistic) test.getLastStat().clone();
            fnExpected.accept(clone);
        } catch (Exception e) {
            Assertions.assertEquals(true, false, "Ошибка клонирования");
        }

        context.getBean(ConsumerComponent.class).shutdown("Test");
    }

    private void sleep(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}