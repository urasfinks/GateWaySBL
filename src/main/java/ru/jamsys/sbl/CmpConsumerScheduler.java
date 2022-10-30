package ru.jamsys.sbl;

import ru.jamsys.sbl.component.CmpConsumer;
import ru.jamsys.sbl.consumer.SblConsumer;
import ru.jamsys.sbl.thread.NamedThreadFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class CmpConsumerScheduler {

    private ScheduledExecutorService executor;

    public void run() {
        executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory(getThreadName()));
        executor.scheduleAtFixedRate(() -> {
            try {
                Util.forEach(CmpConsumer.toArray(getConsumerComponent().getListConsumerService()), getConsumer());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 1, getPeriod(), TimeUnit.SECONDS);
    }

    protected void shutdown(){
        executor.shutdownNow();
    }

    protected CmpConsumer getConsumerComponent(){
        return null;
    }

    protected String getThreadName(){
        return null;
    }

    protected int getPeriod(){
        return 1;
    }

    protected Consumer<SblConsumer> getConsumer(){
        return null;
    }

}
