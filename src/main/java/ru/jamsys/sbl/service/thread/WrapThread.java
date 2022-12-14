package ru.jamsys.sbl.service.thread;

import lombok.Data;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class WrapThread {

    private Thread thread;
    private AtomicBoolean isRun = new AtomicBoolean(true);
    private long lastWakeUp = System.currentTimeMillis();
    private AtomicInteger countIteration = new AtomicInteger(0);

    public void incCountIteration() {
        countIteration.incrementAndGet();
    }

}
