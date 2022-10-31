package ru.jamsys.sbl.scheduler;

import java.util.function.Consumer;

public interface SblScheduler {

    void run();

    Consumer<Void> getConsumer();

    void shutdown();

}
