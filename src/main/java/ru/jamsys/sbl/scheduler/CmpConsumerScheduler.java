package ru.jamsys.sbl.scheduler;

import ru.jamsys.sbl.component.CmpService;

import java.util.function.Function;

public interface CmpConsumerScheduler {

    <T, R> Function<T, R> getConsumer();
    String getThreadName();
    CmpService getComponentService();

}
