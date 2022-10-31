package ru.jamsys.sbl.thread;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.sbl.SblServiceStatistic;
import ru.jamsys.sbl.Util;
import ru.jamsys.sbl.consumer.SblConsumerShutdownException;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

public abstract class SblServiceAbstract implements SblService {

    @Getter
    protected String name;

    private int threadCountMin;
    private AtomicInteger threadCountMax; //Я подумал, что неплохо в рантайме управлять
    private long threadKeepAlive;

    @Getter
    private volatile int tpsInputMax = -1; //-1 infinity

    private final AtomicInteger threadNameCounter = new AtomicInteger(0);

    private AtomicBoolean isActive = new AtomicBoolean(false);
    private List<WrapThread> threadList = new CopyOnWriteArrayList<>();
    private ConcurrentLinkedDeque<WrapThread> threadParkQueue = new ConcurrentLinkedDeque<>();

    private AtomicInteger tpsIdle = new AtomicInteger(0);
    private AtomicInteger tpsInput = new AtomicInteger(0);
    private AtomicInteger tpsOutput = new AtomicInteger(0);

    private volatile SblServiceStatistic statLast = new SblServiceStatistic();

    public SblServiceStatistic getStatLast() {
        return statLast;
    }

    @Override
    public SblServiceStatistic getStatClone() {
        return statLast.clone();
    }

    @Setter
    protected boolean debug = false;

    @Override
    public void incThreadMax() {
        threadCountMax.incrementAndGet();
    }

    @Override
    public void decThreadMax() {
        threadCountMax.decrementAndGet();
    }

    @Override
    public void setTpsInputMax(int max) {
        tpsInputMax = max;
    }

    @Override
    public SblServiceStatistic statistic() {
        statLast.setTpsIdle(tpsIdle.getAndSet(0));
        statLast.setTpsInput(tpsInput.getAndSet(0));
        statLast.setTpsOutput(tpsOutput.getAndSet(0));
        statLast.setThreadCount(threadList.size());
        statLast.setThreadCountPark(threadParkQueue.size());
        return statLast;
    }

    @Override
    public void shutdown() throws SblConsumerShutdownException {
        if (isActive.compareAndSet(true, false)) { //Только один поток будет останавливать
            while (threadList.size() > 0) {
                try {
                    WrapThread wrapThread = threadParkQueue.getFirst(); //Замысел такой, что бы выцеплять только заверенные процессы
                    forceRemoveThread(wrapThread);
                } catch (NoSuchElementException e) { //Если нет в отстойнике - подождём немного
                    try {
                        TimeUnit.MILLISECONDS.sleep(100);
                    } catch (InterruptedException e2) {
                        e2.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (threadList.size() > 0) {
            throw new SblConsumerShutdownException("ThreadPoolSize: " + threadList.size());
        }
    }

    protected boolean isThreadRemove(SblServiceStatistic stat) {
        return stat.getThreadCount() > threadCountMin;
    }

    protected boolean isThreadAdd() {
        return threadList.size() < threadCountMax.get();
    }

    protected boolean isThreadParkAll() {
        return threadParkQueue.size() > 0 && threadParkQueue.size() == threadList.size();
    }

    protected int getThreadListSize() {
        return threadList.size();
    }

    protected int getThreadParkQueueSize() {
        return threadParkQueue.size();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected boolean configure(String name, int threadCountMin, int threadCountMax, long threadKeepAliveMillis) {
        if (isActive.compareAndSet(false, true)) {
            this.name = name;
            this.threadCountMin = threadCountMin;
            this.threadCountMax = new AtomicInteger(threadCountMax);
            this.threadKeepAlive = threadKeepAliveMillis;
            return true;
        }
        return false;
    }

    protected boolean isLimitTpsInputOverflow() {
        return tpsInputMax > 0 && tpsInput.get() >= tpsInputMax;
    }

    protected int getDiffTpsInput() {
        SblServiceStatistic stat = getStatClone();
        //return tpsInputMax > 0 ? (tpsInputMax - stat.getTpsInput()) : threadParkQueue.size();
        if (tpsInputMax > 0) {
            int tpsInputGet = tpsInput.get();
            int x = tpsInputMax - tpsInputGet;
            int threadParkQueueSize = getThreadParkQueueSize();
            if (x > threadParkQueueSize) {
                x = threadParkQueueSize;
            }
            x = x/4;
            System.out.println("DIFF: tpsInputMax: " + tpsInputMax + "; tpsInput: " + tpsInputGet + "; => " + x + "; Park: " + threadParkQueueSize);
            return x;
        } else {
            return threadParkQueue.size();
        }
    }

    protected void incTpsInput() {
        tpsInput.incrementAndGet();
    }

    protected void incTpsOutput() {
        tpsOutput.incrementAndGet();
    }

    protected boolean isActive() {
        return isActive.get();
    }

    protected void wakeUpOnceThread() {
        while (true) {
            WrapThread wrapThread = threadParkQueue.pollLast(); //Всегда забираем с конца, в начале тушаться потоки под нож
            if (wrapThread != null) {
                //Так как последующая операция перед вставкой в очередь - блокировка
                //Надо проверить, что поток припаркован (возможна гонка)
                if (wrapThread.getThread().getState().equals(Thread.State.WAITING)) {
                    wrapThread.setLastWakeUp(System.currentTimeMillis());
                    LockSupport.unpark(wrapThread.getThread());
                    break;
                } else { // Ещё статус не переключился, просто отбрасываем в начала очереди, к тем, кто ждёт ножа
                    threadParkQueue.addFirst(wrapThread);
                }
            } else { //Null - элементы закончились, хватит
                break;
            }
        }
    }

    protected void overclocking(int count) {
        if (!isActive() || threadList.size() == threadCountMax.get()) {
            return;
        }
        for (int i = 0; i < count; i++) {
            addThead();
        }
    }

    protected void addThead() {
        if (threadList.size() < threadCountMax.get()) {
            final SblService self = this;
            final WrapThread wrapThread = new WrapThread();
            wrapThread.setThread(new Thread(() -> {
                while (isActive() && wrapThread.getIsRun().get()) {
                    wrapThread.incCountIteration(); // Это для отслеживания, что поток вообще работает
                    tpsIdle.incrementAndGet();
                    iteration(wrapThread, self);
                    if (wrapThread.getIsRun().get()) { //Если мы все, то больше парковаться не надо
                        //В методе wakeUpOnceThread решена проблема гонки за предварительный старт
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

    protected void removeThread(WrapThread wth) {
        if (threadList.size() > threadCountMin) {
            forceRemoveThread(wth);
        }
    }

    private void forceRemoveThread(WrapThread wth) { //Этот метод может загасит сервис до конца, используйте обычный removeThread
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

    protected void checkKeepAliveAndRemoveThread() { //Проверка ждунов, что они давно не вызывались и у них кол-во итераций равно 0 -> нож
        try {
            final long now = System.currentTimeMillis();
            //Хотелось, что бы удаление было 1 тред в секунду, но так как helper запускается раз в 2 секунды, то и удалять будем по 2
            final AtomicInteger c = new AtomicInteger(2);
            Util.forEach(WrapThread.toArray(threadList), (wth) -> {
                long future = wth.getLastWakeUp() + threadKeepAlive;
                //Время последнего оживления превысило keepAlive + поток реально не работал
                if (now > future && wth.getCountIteration().get() == 0 && c.getAndDecrement() > 0) {
                    removeThread(wth);
                } else {
                    wth.getCountIteration().set(0);
                }
                return null;
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
