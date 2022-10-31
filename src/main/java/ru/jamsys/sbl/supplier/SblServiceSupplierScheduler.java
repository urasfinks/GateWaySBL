package ru.jamsys.sbl.supplier;

import ru.jamsys.sbl.scheduler.SblSchedulerAbstract;

import java.util.function.Consumer;

public class SblServiceSupplierScheduler extends SblSchedulerAbstract {

    public SblServiceSupplierScheduler(String name, long periodMillis) {
        super(name, periodMillis);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Consumer<SblServiceSupplier> getConsumer() {
        return SblServiceSupplier::tick;
    }
}
