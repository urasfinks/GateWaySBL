package ru.jamsys.sbl.service.thread;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import reactor.util.annotation.Nullable;
import ru.jamsys.sbl.SblServiceStatistic;
import ru.jamsys.sbl.Util;
import ru.jamsys.sbl.UtilToArray;
import ru.jamsys.sbl.service.SblService;
import ru.jamsys.sbl.service.consumer.SblConsumerShutdownException;
import ru.jamsys.sbl.scheduler.SblServiceScheduler;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Stream;

public abstract class SblServiceAbstract implements SblService {

    @Getter
    protected String name;

    private int threadCountMin;
    private AtomicInteger threadCountMax; //Я подумал, что неплохо в рантайме управлять
    private long threadKeepAlive;

    @Getter
    private volatile int tpsInputMax = -1; //-1 infinity

    private final AtomicInteger threadNameCounter = new AtomicInteger(0);

    private final AtomicBoolean isActive = new AtomicBoolean(false);
    private final List<WrapThread> threadList = new CopyOnWriteArrayList<>();
    private final ConcurrentLinkedDeque<WrapThread> threadParkQueue = new ConcurrentLinkedDeque<>();

    private final AtomicInteger tpsIdle = new AtomicInteger(0);
    private final AtomicInteger tpsInput = new AtomicInteger(0);
    private final AtomicInteger tpsOutput = new AtomicInteger(0);

    private final SblServiceStatistic statLast = new SblServiceStatistic();

    private final ConcurrentLinkedDeque<Long> timeTransactionQueue = new ConcurrentLinkedDeque<>();

    private SblServiceScheduler scheduler;

    public SblServiceStatistic getStatLast() {
        return statLast;
    }

    @Override
    @Nullable
    public SblServiceStatistic getStatClone() {
        return statLast.clone();
    }

    protected SblServiceStatistic getStatCurrent() {
        SblServiceStatistic curStat = new SblServiceStatistic();
        curStat.setServiceName(getName());
        curStat.setTpsInput(tpsInput.get());//
        curStat.setThreadCountPark(threadParkQueue.size());//
        //Сумарная статистика дожна браться за более долгое время, поэтому просто копируем
        curStat.setSumTimeTpsAvg(statLast.getSumTimeTpsAvg()); //
        return curStat;
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
        statLast.setServiceName(getName());
        statLast.setTpsIdle(tpsIdle.getAndSet(0));
        statLast.setTpsInput(tpsInput.getAndSet(0));
        statLast.setTpsOutput(tpsOutput.getAndSet(0));
        statLast.setThreadCount(threadList.size());
        statLast.setThreadCountPark(threadParkQueue.size());
        statLast.setTimeTransaction(timeTransactionQueue);
        timeTransactionQueue.clear();
        return statLast;
    }

    @Override
    public void shutdown() throws SblConsumerShutdownException {
        Util.logConsole(Thread.currentThread(), "TO SHUTDOWN");
        scheduler.shutdown();
        if (isActive.compareAndSet(true, false)) { //Только один поток будет останавливать
            long startShutdown = System.currentTimeMillis();
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
                if (System.currentTimeMillis() > startShutdown + 30000) { //Пошла жара
                    //Util.logConsole(Thread.currentThread(), "ERROR SHUTDOWN 30 sec -> throw INTERRUPT");
                    new Exception("ERROR SHUTDOWN 30 sec -> throw INTERRUPT").printStackTrace();
                    try {
                        Stream.of(threadList.toArray(new WrapThread[0])).forEach(threadWrap -> { //Боремся за атамарность конкурентны изменений
                            try {
                                threadWrap.getThread().interrupt();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    } catch (Exception e) { //Край, вилы
                        e.printStackTrace();
                    } finally {
                        threadList.clear();
                    }
                    break;
                }
            }
        } else {
            Util.logConsole(Thread.currentThread(), "Already shutdown in other Thread");
        }
        if (threadList.size() > 0) {
            throw new SblConsumerShutdownException("ThreadPoolSize: " + threadList.size());
        }
    }

    protected boolean isThreadRemove(@NonNull SblServiceStatistic stat) {
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

    protected void configure(String name, int threadCountMin, int threadCountMax, long threadKeepAliveMillis, long schedulerSleepMillis) {
        if (isActive.compareAndSet(false, true)) {
            this.name = name;
            this.threadCountMin = threadCountMin;
            this.threadCountMax = new AtomicInteger(threadCountMax);
            this.threadKeepAlive = threadKeepAliveMillis;

            scheduler = new SblServiceScheduler(name + "-Scheduler", schedulerSleepMillis);
            scheduler.run(this);
            overclocking(threadCountMin);
        }
    }

    protected boolean isLimitTpsInputOverflow() {
        return tpsInputMax > 0 && tpsInput.get() >= tpsInputMax;
    }

    protected void incTpsInput() {
        tpsInput.incrementAndGet();
    }

    protected void incTpsOutput(long time) {
        if (time > 0) {
            timeTransactionQueue.add(time);
        }
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
        if (!isActive() || threadList.size() >= threadCountMax.get()) {
            return;
        }
        for (int i = 0; i < count; i++) {
            addThread();
        }
    }

    protected void addThread() {
        if (isActive() && threadList.size() < threadCountMax.get()) {
            final SblService self = this;
            final WrapThread wrapThread = new WrapThread();
            wrapThread.setThread(new Thread(() -> {
                while (isActive() && wrapThread.getIsRun().get()) {
                    try {
                        wrapThread.incCountIteration(); // Это для отслеживания, что поток вообще работает
                        tpsIdle.incrementAndGet();
                        iteration(wrapThread, self);
                        //В методе wakeUpOnceThread решена проблема гонки за предварительный старт
                        threadParkQueue.add(wrapThread);
                        LockSupport.park();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                try {
                    forceRemoveThread(wrapThread);
                } catch (Exception e) {
                    e.printStackTrace();
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

    synchronized private void forceRemoveThread(WrapThread wth) { //Этот метод может загасит сервис до конца, используйте обычный removeThread
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
            Util.forEach(UtilToArray.toArrayWrapThread(threadList), (wth) -> {
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
