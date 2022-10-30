package ru.jamsys.sbl.consumer;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.sbl.Util;
import ru.jamsys.sbl.thread.WrapThread;
import ru.jamsys.sbl.message.Message;
import ru.jamsys.sbl.message.MessageHandle;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

@Getter
public class SblConsumer {

    private String name;
    private final int threadCountMin;
    private final int threadCountMax;
    private final long threadKeepAlive;
    private final Consumer<Message> consumer;

    private ConcurrentLinkedDeque<Message> queueTask = new ConcurrentLinkedDeque<>();
    private ConcurrentLinkedDeque<WrapThread> threadParkQueue = new ConcurrentLinkedDeque<>();
    private List<WrapThread> threadList = new CopyOnWriteArrayList<>();
    private volatile SblConsumerStatistic statLast = new SblConsumerStatistic();

    private AtomicInteger threadNameCounter = new AtomicInteger(0);
    private AtomicInteger tpsIdle = new AtomicInteger(0);
    private AtomicInteger tpsOutput = new AtomicInteger(0);
    private AtomicInteger tpsInput = new AtomicInteger(0);
    private AtomicBoolean isActive = new AtomicBoolean(true);

    @Setter
    private boolean debug = false;

    @Setter
    private volatile int tpsInputMax = -1; //-1 infinity

    public SblConsumer(String name, int threadCountMin, int threadCountMax, long threadKeepAlive, Consumer<Message> consumer) {
        this.name = name;
        this.threadCountMin = threadCountMin;
        this.threadCountMax = threadCountMax;
        this.consumer = consumer;
        this.threadKeepAlive = threadKeepAlive;
        overclocking(threadCountMin);
    }

    private boolean isNotActive() {
        return !isActive.get();
    }

    public void accept(Message message) throws SblConsumerShutdownException, SblConsumerTpsOverflowException {
        if (isNotActive()) {
            throw new SblConsumerShutdownException("Consumer shutdown");
        }
        if (tpsInputMax > 0 && tpsInput.get() >= tpsInputMax) {
            throw new SblConsumerTpsOverflowException("Max tps: " + tpsInputMax);
        }
        queueTask.add(message);
        tpsInput.incrementAndGet();
        message.onHandle(MessageHandle.PUT, this);
        if (threadParkQueue.size() > 0) {//Если ждунов нет, то и вообще ничего делать не надо
            if (threadParkQueue.size() == threadList.size()) { //Если общее кол-во тредов равно коли-ву ждунов
                wakeUpThread();
            } else if (queueTask.size() > 0) {//Если в очереди есть задачи, попробуем пробудить (так как add выше)
                wakeUpThread();
            }
        }
    }

    private void wakeUpThread() {
        WrapThread wrapThread = threadParkQueue.pollLast();
        if (wrapThread != null) {
            wrapThread.setLastWakeUp(System.currentTimeMillis());
            LockSupport.unpark(wrapThread.getThread());
        }
    }

    private void overclocking(int count) {
        if (isNotActive() || threadList.size() == threadCountMax) {
            return;
        }
        for (int i = 0; i < count; i++) {
            addThead();
        }
    }

    private void addThead() {
        if (threadList.size() < threadCountMax) {
            final SblConsumer self = this;
            final WrapThread wrapThread = new WrapThread();
            wrapThread.setThread(new Thread(() -> {
                while (wrapThread.getIsRun().get()) {
                    wrapThread.incCountIteration(); // Это для отслеживания, что поток вообще работать)
                    tpsIdle.incrementAndGet();
                    while (!queueTask.isEmpty() && wrapThread.getIsRun().get()) { //Всегда проверяем, что поток не выводят из эксплуатации
                        Message message = queueTask.pollLast();
                        if (message != null) {
                            tpsOutput.incrementAndGet();
                            message.onHandle(MessageHandle.EXECUTE, self);
                            try {
                                consumer.accept(message);
                                message.onHandle(MessageHandle.COMPLETE, self);
                            } catch (Exception e) {
                                message.setError(e);
                            }
                        }
                    }
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

    private void removeThread(WrapThread wth) {
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
                Util.logConsole("removeThread: " + wrapThread);
            }
        }
    }

    public void statistic() { //Пока пришел к мысли, что не надо смешивать статистику и принятие решений увелечений и уменьшению потоков
        statLast.setTpsInput(tpsInput.getAndSet(0));
        statLast.setTpsIdle(tpsIdle.getAndSet(0));
        statLast.setTpsOutput(tpsOutput.getAndSet(0));
        statLast.setThreadCount(threadList.size());
        statLast.setQueueSize(queueTask.size());
        statLast.setThreadCountPark(threadParkQueue.size());
        if (debug) {
            Util.logConsole("Statistic: " + statLast.toString());
        }
    }

    public void helper() {
        try {
            SblConsumerStatistic stat = (SblConsumerStatistic) statLast.clone();
            if (debug) {
                Util.logConsole("Helper: QueueSize: " + stat.getQueueSize() + "; CountThread: " + stat.getThreadCount());
            }
            if (stat.getQueueSize() > 0) { //Если очередь наполнена
                //Расчет необходимого кол-ва потоков, что бы обработать всю очередь
                int needCountThread = SblConsumerUtil.getNeedCountThread(stat);
                if (needCountThread > 0 && threadList.size() < threadCountMax) {
                    if (debug) {
                        Util.logConsole("Helper: addThread: " + needCountThread);
                    }
                    overclocking(needCountThread);
                }
            } else if (stat.getThreadCount() > threadCountMin) { //нет необходимости удалять, когда потоков заявленный минимум
                long now = System.currentTimeMillis();
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
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
}
