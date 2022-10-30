package ru.jamsys.sbl;

import lombok.Setter;
import ru.jamsys.sbl.component.CmpConsumer;
import ru.jamsys.sbl.thread.NamedThreadFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

public class CmpConsumerScheduler {

    @Setter
    protected boolean debug = false;

    private ScheduledExecutorService executor;

    public void run() {
        executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory(getThreadName()));
        executor.scheduleAtFixedRate(() -> {
            try {
                List<Object> objects = Util.forEach(CmpConsumer.toArray(getConsumerComponent().getListConsumerService()), getConsumer());
                Consumer<Object> handler = getHandler();
                if (handler != null) {
                    handler.accept(objects);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 1, getPeriod(), TimeUnit.SECONDS);
    }

    protected void shutdown() {
        executor.shutdownNow();
    }

    protected CmpConsumer getConsumerComponent() {
        return null;
    }

    protected String getThreadName() {
        return null;
    }

    protected int getPeriod() {
        return 1;
    }

    protected  <T,R> Function<T, R> getConsumer() {
        return null;
    }

    protected <T> Consumer<T> getHandler() {
        return null;
    }

}
