package ru.jamsys.sbl.thread;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.sbl.SblServiceStatistic;
import ru.jamsys.sbl.Util;
import ru.jamsys.sbl.consumer.SblConsumerShutdownException;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

@Getter
public abstract class SblServiceImpl implements SblService {

    protected String name;

    protected final int threadCountMin;
    protected final int threadCountMax;
    protected final long threadKeepAlive;

    private AtomicInteger threadNameCounter = new AtomicInteger(0);

    protected AtomicBoolean isActive = new AtomicBoolean(true);
    protected List<WrapThread> threadList = new CopyOnWriteArrayList<>();
    protected ConcurrentLinkedDeque<WrapThread> threadParkQueue = new ConcurrentLinkedDeque<>();

    protected AtomicInteger tpsIdle = new AtomicInteger(0);
    protected AtomicInteger tpsOutput = new AtomicInteger(0);
    protected AtomicInteger tpsInput = new AtomicInteger(0);

    protected volatile SblServiceStatistic statLast = new SblServiceStatistic();

    @Setter
    protected boolean debug = false;

    @Setter
    protected volatile int tpsInputMax = -1; //-1 infinity

    public SblServiceImpl(String name, int threadCountMin, int threadCountMax, long threadKeepAlive) {
        this.name = name;
        this.threadCountMin = threadCountMin;
        this.threadCountMax = threadCountMax;
        this.threadKeepAlive = threadKeepAlive;
    }

    protected boolean isNotActive() {
        return !isActive.get();
    }

    public void shutdown() throws SblConsumerShutdownException {
        if (isActive.compareAndSet(true, false)) { //Только один поток будет останавливать
            while (threadList.size() > 0) {
                WrapThread wrapThread = threadParkQueue.getFirst(); //Замысел такой, что бы выцеплять только заверенные процессы
                if (wrapThread != null) {
                    forceRemoveThread(wrapThread);
                } else {
                    try {
                        TimeUnit.MILLISECONDS.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if (threadList.size() > 0) {
            throw new SblConsumerShutdownException("ThreadPoolSize: " + threadList.size());
        }
    }

    protected void removeThread(WrapThread wth) {
        if (threadList.size() > threadCountMin) {
            forceRemoveThread(wth);
        }
    }

    protected void forceRemoveThread(WrapThread wth) { //Этот метод может загасит сервис до конца, используйте обычный removeThread
        WrapThread wrapThread = wth != null ? wth : threadList.get(0);
        if (wrapThread != null) {
            wrapThread.getIsRun().set(false);
            LockSupport.unpark(wrapThread.getThread()); //Мы его оживляем, что бы он закончился
            threadList.remove(wrapThread);
            threadParkQueue.remove(wrapThread); // На всякий случай
            if (debug) {
                Util.logConsole(Thread.currentThread(), "removeThread: " + wrapThread);
            }
        }
    }

    protected void wakeUpThread() {
        WrapThread wrapThread = threadParkQueue.pollLast();
        if (wrapThread != null) {
            wrapThread.setLastWakeUp(System.currentTimeMillis());
            LockSupport.unpark(wrapThread.getThread());
        }
    }

    protected void overclocking(int count) {
        if (isNotActive() || threadList.size() == threadCountMax) {
            return;
        }
        for (int i = 0; i < count; i++) {
            addThead();
        }
    }

    protected void addThead() {
        if (threadList.size() < threadCountMax) {
            final SblService self = this;
            final WrapThread wrapThread = new WrapThread();
            wrapThread.setThread(new Thread(() -> {
                while (wrapThread.getIsRun().get()) {
                    wrapThread.incCountIteration(); // Это для отслеживания, что поток вообще работает
                    tpsIdle.incrementAndGet();
                    iteration(wrapThread, self);
                    if (wrapThread.getIsRun().get()) { //Если мы все, то больше парковаться не надо
                        threadParkQueue.add(wrapThread);
                        LockSupport.park();
                    }
                }
            }));
            wrapThread.getThread().setName(getName() + "-" + threadNameCounter.getAndIncrement());
            wrapThread.getThread().start();
            threadList.add(wrapThread);
        }
    }
}
