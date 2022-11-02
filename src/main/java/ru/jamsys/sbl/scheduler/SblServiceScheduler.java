package ru.jamsys.sbl.scheduler;

import java.util.function.Consumer;

public class SblServiceScheduler extends SblSchedulerAbstract {

    public SblServiceScheduler(String name, long periodMillis) {
        super(name, periodMillis);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Consumer<SblSchedulerTick> getConsumer() {
        return SblSchedulerTick::tick;
    }

}
