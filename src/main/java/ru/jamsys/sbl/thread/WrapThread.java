package ru.jamsys.sbl.thread;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
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

    public static <T> T[] toArray(List<T> l) throws Exception {
        return (T[]) l.toArray(new WrapThread[0]);
    }

}
