package ru.jamsys.sbl.consumer;

import lombok.Getter;
import ru.jamsys.sbl.thread.SblService;
import ru.jamsys.sbl.thread.SblServiceImpl;
import ru.jamsys.sbl.SblServiceStatistic;
import ru.jamsys.sbl.Util;
import ru.jamsys.sbl.thread.WrapThread;
import ru.jamsys.sbl.message.Message;
import ru.jamsys.sbl.message.MessageHandle;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Getter
public class SblServiceConsumer extends SblServiceImpl implements Consumer<Message> {

    private final Consumer<Message> consumer;
    private ConcurrentLinkedDeque<Message> queueTask = new ConcurrentLinkedDeque<>();

    public SblServiceConsumer(String name, int threadCountMin, int threadCountMax, long threadKeepAlive, Consumer<Message> consumer) {
        super(name, threadCountMin, threadCountMax, threadKeepAlive);
        this.consumer = consumer;
        overclocking(threadCountMin);
    }

    @Override
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

    @Override
    public void iteration(WrapThread wrapThread, SblService service){
        while (!queueTask.isEmpty() && wrapThread.getIsRun().get()) { //Всегда проверяем, что поток не выводят из эксплуатации
            Message message = queueTask.pollLast();
            if (message != null) {
                tpsOutput.incrementAndGet();
                message.onHandle(MessageHandle.EXECUTE, service);
                try {
                    consumer.accept(message);
                    message.onHandle(MessageHandle.COMPLETE, service);
                } catch (Exception e) {
                    message.setError(e);
                }
            }
        }
    }

    @Override
    public SblServiceStatistic statistic() {
        statLast.setTpsInput(tpsInput.getAndSet(0));
        statLast.setTpsIdle(tpsIdle.getAndSet(0));
        statLast.setTpsOutput(tpsOutput.getAndSet(0));
        statLast.setThreadCount(threadList.size());
        statLast.setQueueSize(queueTask.size());
        statLast.setThreadCountPark(threadParkQueue.size());
        return statLast;
    }

    @Override
    public void helper() {
        try {
            SblServiceStatistic stat = statLast.clone();
            if (debug) {
                Util.logConsole(Thread.currentThread(), "QueueSize: " + stat.getQueueSize() + "; CountThread: " + stat.getThreadCount());
            }
            if (stat.getQueueSize() > 0) { //Если очередь наполнена
                //Расчет необходимого кол-ва потоков, что бы обработать всю очередь
                int needCountThread = SblConsumerUtil.getNeedCountThread(stat);
                if (needCountThread > 0 && threadList.size() < threadCountMax) {
                    if (debug) {
                        Util.logConsole(Thread.currentThread(), "addThread: " + needCountThread);
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
                    return null;
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
