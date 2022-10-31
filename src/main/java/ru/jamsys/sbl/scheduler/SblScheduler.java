package ru.jamsys.sbl.scheduler;

import java.util.function.Consumer;

public interface SblScheduler {

    void run();

    <T> Consumer<T> getConsumer();

    void shutdown();

}
