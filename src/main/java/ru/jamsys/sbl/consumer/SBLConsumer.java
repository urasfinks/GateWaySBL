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
public class SBLConsumer {

    private String name;
    private int countThreadMin = 1;
    private int countThreadMax = 1;
    private volatile long keepAlive = 60000L;
    private Consumer<Message> worker = null;
    private ConcurrentLinkedDeque<Message> queueTask = new ConcurrentLinkedDeque<>();
    private ConcurrentLinkedDeque<WrapThread> queueParkThread = new ConcurrentLinkedDeque<>();
    private List<WrapThread> listThread = new CopyOnWriteArrayList<>();
    @Getter
    private volatile ConsumerStatistic lastStat = new ConsumerStatistic();

    private AtomicInteger nameCounter = new AtomicInteger(0);
    private AtomicInteger statUseful = new AtomicInteger(0);
    private AtomicInteger statUseless = new AtomicInteger(0);
    private AtomicInteger tps = new AtomicInteger(0);
    private int overclockLimit = 0; //Безразличная по значение переменная, просто для общего статуса, даже если будут расхождения
    private AtomicBoolean isActive = new AtomicBoolean(true);

    @Setter
    private boolean debug = false;

    public SBLConsumer(String name, int countThreadMin, int countThreadMax, long keepAlive, Consumer<Message> worker) {
        this.name = name;
        this.countThreadMin = countThreadMin;
        this.countThreadMax = countThreadMax;
        this.worker = worker;
        this.keepAlive = keepAlive;
        overclocking(countThreadMin);
    }

    private boolean isActive() {
        return !isActive.get();
    }

    public boolean handle(Message message) {
        if (isActive()) {
            return false;
        }
        tps.incrementAndGet();
        message.onHandle(MessageHandle.PUT, this);
        queueTask.add(message);
        if (queueParkThread.size() > 0) {//Если ждунов нет, то и вообще ничего делать не надо
            if (queueParkThread.size() == listThread.size()) { //Если общее кол-во тредов равно коли-ву ждунов
                wakeUpThread();
            } else if (queueTask.size() > 0) {//Если в очереди есть задачи, попробуем пробудить (так как add выше)
                wakeUpThread();
            }
        }
        return true;
    }

    private void wakeUpThread() {
        WrapThread wrapThread = queueParkThread.pollLast();
        if (wrapThread != null) {
            wrapThread.setLastWakeUp(System.currentTimeMillis());
            LockSupport.unpark(wrapThread.getThread());
        }
    }

    private void overclocking(int count) {
        if (isActive() || listThread.size() == countThreadMax) {
            return;
        }
        for (int i = 0; i < count; i++) {
            addThead();
        }
    }

    private void addThead() {
        if (listThread.size() < countThreadMax) {
            final SBLConsumer self = this;
            final WrapThread wrapThread = new WrapThread();
            wrapThread.setThread(new Thread(() -> {
                while (wrapThread.getIsRun().get()) {
                    wrapThread.incCountIteration(); // Это для отслеживания, что поток вообще работать)
                    statUseless.incrementAndGet();
                    while (!queueTask.isEmpty() && wrapThread.getIsRun().get()) { //Всегда проверяем, что поток не выводят из эксплуатации
                        Message message = queueTask.pollLast();
                        if (message != null) {
                            statUseful.incrementAndGet();
                            message.onHandle(MessageHandle.EXECUTE, self);
                            try {
                                worker.accept(message);
                                message.onHandle(MessageHandle.COMPLETE, self);
                            } catch (Exception e) {
                                message.setError(e);
                            }
                        }
                    }
                    if (wrapThread.getIsRun().get()) { //Если мы все, то больше парковаться не надо
                        queueParkThread.add(wrapThread);
                        LockSupport.park();
                    }
                }
            }));
            wrapThread.getThread().setName(getName() + "-" + nameCounter.getAndIncrement());
            wrapThread.getThread().start();
            listThread.add(wrapThread);
        } else {
            overclockLimit++;
        }
    }

    private void removeThread(WrapThread wth) {
        if (listThread.size() > countThreadMin) {
            forceRemoveThread(wth);
        }
    }

    private void forceRemoveThread(WrapThread wth) { //Этот метод может загасит сервис до конца, используйте обычный removeThread
        WrapThread wrapThread = wth != null ? wth : listThread.get(0);
        if (wrapThread != null) {
            wrapThread.getIsRun().set(false);
            LockSupport.unpark(wrapThread.getThread()); //Мы его оживляем, что бы он закончился
            listThread.remove(wrapThread);
            queueParkThread.remove(wrapThread); // На всякий случай
            if (debug) {
                Util.logConsole("removeThread: " + wrapThread);
            }
        }
    }

    public void statistic() { //Пока пришел к мысли, что не надо смешивать статистику и принятие решений увелечений и уменьшению потоков
        lastStat.setCountOperationUseful(statUseful.getAndSet(0));
        lastStat.setCountOperationUseless(statUseless.getAndSet(0));
        lastStat.setCountThread(listThread.size());
        lastStat.setQueueSize(queueTask.size());
        lastStat.setTps(tps.getAndSet(0));
        lastStat.setOverclockLimit(overclockLimit);
        overclockLimit = 0;
        if (debug) {
            Util.logConsole("Statistic: " + lastStat.toString());
        }
    }

    public void helper() {
        try {
            ConsumerStatistic stat = (ConsumerStatistic) lastStat.clone();
            if (debug) {
                Util.logConsole("Helper: QueueSize: " + stat.getQueueSize() + "; CountThread: " + stat.getCountThread());
            }
            if (stat.getQueueSize() > 0) { //Если очередь наполнена
                //Расчет необходимого кол-ва потоков, что бы обработать всю очередь
                int needCountThread = ConsumerUtil.getNeedCountThread(stat);
                if (needCountThread > 0 && listThread.size() < countThreadMax) {
                    if (debug) {
                        Util.logConsole("Helper: addThread: " + needCountThread);
                    }
                    overclocking(needCountThread);
                }
            } else if (stat.getCountThread() > countThreadMin) { //нет необходимости удалять, когда потоков заявленный минимум
                long now = System.currentTimeMillis();
                //Хотелось, что бы удаление было 1 тред в секунду, но так как helper запускается раз в 2 секунды, то и удалять будем по 2
                final AtomicInteger c = new AtomicInteger(2);
                Util.forEach(WrapThread.toArray(listThread), (wth) -> {
                    long future = wth.getLastWakeUp() + keepAlive;
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

    public void shutdown() {
        isActive.set(false);
        while (listThread.size() > 0) {
            WrapThread wrapThread = queueParkThread.getFirst(); //Замысел такой, что бы выцеплять только заверенные процессы
            if (wrapThread != null) {
                forceRemoveThread(wrapThread);
            } else {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
