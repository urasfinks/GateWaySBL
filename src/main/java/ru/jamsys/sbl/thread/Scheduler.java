package ru.jamsys.sbl.thread;

import ru.jamsys.sbl.Util;
import ru.jamsys.sbl.component.ConsumerComponent;
import ru.jamsys.sbl.consumer.SBLConsumer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Scheduler {
    public void run() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory(getThreadName()));
        executor.scheduleAtFixedRate(() -> {
            try {
                Util.forEach(ConsumerComponent.toArray(getConsumerComponent().getListConsumerService()), getLogic());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 1, getPeriod(), TimeUnit.SECONDS);
    }

    protected ConsumerComponent getConsumerComponent(){
        return null;
    }

    protected String getThreadName(){
        return null;
    }

    protected int getPeriod(){
        return 1;
    }

    protected Consumer<SBLConsumer> getLogic(){
        return null;
    }
}
