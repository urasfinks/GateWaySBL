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
public abstract class SblServiceAbstract implements SblService {

    protected String name;

    protected int threadCountMin;
    protected AtomicInteger threadCountMax; //Я подумал, что неплохо в рантайме управлять
    protected long threadKeepAlive;

    private final AtomicInteger threadNameCounter = new AtomicInteger(0);

    protected AtomicBoolean isActive = new AtomicBoolean(false);
    protected List<WrapThread> threadList = new CopyOnWriteArrayList<>();
    protected ConcurrentLinkedDeque<WrapThread> threadParkQueue = new ConcurrentLinkedDeque<>();

    protected AtomicInteger tpsIdle = new AtomicInteger(0);
    protected AtomicInteger tpsOutput = new AtomicInteger(0);


    protected volatile SblServiceStatistic statLast = new SblServiceStatistic();

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

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean configure(String name, int threadCountMin, int threadCountMax, long threadKeepAliveMillis) {
        if (isActive.compareAndSet(false, true)) {
            this.name = name;
            this.threadCountMin = threadCountMin;
            this.threadCountMax = new AtomicInteger(threadCountMax);
            this.threadKeepAlive = threadKeepAliveMillis;
            return true;
        }
        return false;
    }

    protected boolean isLimitTpsMain() {
        return getTpsMainMax() > 0 && getTpsMain().get() >= getTpsMainMax();
    }

    protected void incTpsMain() {
        getTpsMain().incrementAndGet();
    }

    @Override
    public SblServiceStatistic statistic() {
        statLast.setTpsIdle(tpsIdle.getAndSet(0));
        statLast.setTpsOutput(tpsOutput.getAndSet(0));
        statLast.setThreadCount(threadList.size());
        statLast.setThreadCountPark(threadParkQueue.size());
        return statLast;
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
        while (true) {
            WrapThread wrapThread = threadParkQueue.pollLast(); //Всегда забираем с конца, в начале тушаться потоки под нож
            if (wrapThread != null) {
                //Так как последующая операция перед вставкой в очередь - блокировка
                //Надо проверить, что поток заблокирован (возможна гонка)
                if (wrapThread.getThread().getState().equals(Thread.State.WAITING)) {
                    wrapThread.setLastWakeUp(System.currentTimeMillis());
                    LockSupport.unpark(wrapThread.getThread());
                } else { // Ещё статус не переключился, просто отбрасываем в начала очереди, к тем, кто ждёт ножа
                    threadParkQueue.addFirst(wrapThread);
                }
            } else { //Null - элементы закончились, хватит
                break;
            }
        }
    }

    protected void overclocking(int count) {
        if (isNotActive() || threadList.size() == threadCountMax.get()) {
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
                while (wrapThread.getIsRun().get()) {
                    wrapThread.incCountIteration(); // Это для отслеживания, что поток вообще работает
                    tpsIdle.incrementAndGet();
                    iteration(wrapThread, self);
                    if (wrapThread.getIsRun().get()) { //Если мы все, то больше парковаться не надо
                        //Можно нарваться на гонку, когда его выхватят из очереди, скажут давай поехали

                        //wrapThread.setParkTime(System.currentTimeMillis() + 10);
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

    public void checkKeepAliveAndRemoveThread() { //Проверка ждунов, что они давно не вызывались и у них кол-во итераций равно 0 -> нож
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
