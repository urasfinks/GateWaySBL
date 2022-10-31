package ru.jamsys.sbl.scheduler;

import lombok.Getter;
import lombok.Setter;

import ru.jamsys.sbl.thread.NamedThreadFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


public abstract class SblSchedulerAbstract implements SblScheduler {

    @Setter
    protected boolean debug = false;

    private final ScheduledExecutorService executor;
    private AtomicBoolean isRun = new AtomicBoolean(false);

    @Getter
    private String name;
    @Getter
    private long periodMillis;

    public SblSchedulerAbstract(String name, long periodMillis) {
        this.name = name;
        this.periodMillis = periodMillis;
        executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory(getName()));
    }

    public void run() {
        run(null);
    }

    public <T> void run(T t) {
        if (isRun.compareAndSet(false, true)) {
            executor.scheduleAtFixedRate(() -> getConsumer().accept(t), 1, getPeriodMillis(), TimeUnit.MILLISECONDS);
        }
    }

    public void shutdown() {
        if (isRun.get()) {
            executor.shutdownNow();
            isRun.set(false);
        }
    }

}