package ru.jamsys.sbl.scheduler;

import lombok.Setter;
import ru.jamsys.sbl.Util;
import ru.jamsys.sbl.component.CmpService;
import ru.jamsys.sbl.thread.NamedThreadFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public abstract class CmpServiceSchedulerImpl implements CmpConsumerScheduler {

    @Setter
    protected boolean debug = false;

    private ScheduledExecutorService executor;

    public void run() {
        executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory(getThreadName()));
        executor.scheduleAtFixedRate(() -> {
            try {
                List<Object> objects = Util.forEach(CmpService.toArray(getComponentService().getListService()), getConsumer());
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

    protected int getPeriod() {
        return 1;
    }

    protected <T> Consumer<T> getHandler() {
        return null;
    }

}
