package ru.jamsys.sbl.consumer;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.jamsys.sbl.thread.SblService;
import ru.jamsys.sbl.thread.SblServiceAbstract;
import ru.jamsys.sbl.SblServiceStatistic;
import ru.jamsys.sbl.Util;
import ru.jamsys.sbl.thread.WrapThread;
import ru.jamsys.sbl.message.Message;
import ru.jamsys.sbl.message.MessageHandle;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.*;
import java.util.function.Consumer;

@Component
@Scope("prototype")
public class SblServiceConsumer extends SblServiceAbstract implements Consumer<Message> {

    private Consumer<Message> consumer;
    private final ConcurrentLinkedDeque<Message> queueTask = new ConcurrentLinkedDeque<>();


    public void configure(String name, int threadCountMin, int threadCountMax, long threadKeepAliveMillis, Consumer<Message> consumer) {
        if (super.configure(name, threadCountMin, threadCountMax, threadKeepAliveMillis)) {
            this.consumer = consumer;
            overclocking(threadCountMin);
        }
    }

    @Override
    public void accept(Message message) throws SblConsumerShutdownException, SblConsumerTpsOverflowException {
        if (!isActive()) {
            throw new SblConsumerShutdownException("Consumer shutdown");
        }
        if (isLimitTpsInputOverflow()) {
            throw new SblConsumerTpsOverflowException("Max tps: " + getTpsInputMax());
        }
        queueTask.add(message);
        incTpsInput();
        message.onHandle(MessageHandle.PUT, this);
        //Если в очереди есть задачи есть ждуны
        // попробуем пробудить (так как add выше)
        if (getThreadParkQueueSize() > 0 && queueTask.size() > 0) {
            wakeUpOnceThread();
        }
    }

    @Override
    public void iteration(WrapThread wrapThread, SblService service) {
        while (isActive() && !queueTask.isEmpty() && wrapThread.getIsRun().get()) { //Всегда проверяем, что поток не выводят из эксплуатации
            Message message = queueTask.pollLast();
            if (message != null) {
                long startTime = System.currentTimeMillis();
                message.onHandle(MessageHandle.EXECUTE, service);
                try {
                    consumer.accept(message);
                    message.onHandle(MessageHandle.COMPLETE, service);
                    incTpsOutput(System.currentTimeMillis() - startTime);
                } catch (Exception e) {
                    message.setError(e);
                }
            }
        }
    }


    @Override
    public SblServiceStatistic statistic() {
        getStatLast().setQueueSize(queueTask.size());
        return super.statistic();
    }

    @Override
    public void threadStabilizer() {
        try {
            SblServiceStatistic stat = getStatClone();
            if (debug) {
                Util.logConsole(Thread.currentThread(), "QueueSize: " + stat.getQueueSize() + "; CountThread: " + stat.getThreadCount());
            }
            if (stat.getQueueSize() > 0) { //Если очередь наполнена
                //Расчет необходимого кол-ва потоков, что бы обработать всю очередь
                int needCountThread = getNeedCountThread(stat, debug);
                if (needCountThread > 0 && isThreadAdd()) {
                    if (debug) {
                        Util.logConsole(Thread.currentThread(), "addThread: " + needCountThread);
                    }
                    overclocking(needCountThread);
                }
            } else if (isThreadRemove(stat)) { //нет необходимости удалять, когда потоков заявленный минимум
                checkKeepAliveAndRemoveThread();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getNeedCountThread(SblServiceStatistic stat, boolean debug) {
        try {
            BigDecimal threadTps = new BigDecimal(Math.max(stat.getTpsOutput(), 1)) //Если все потоки встали и не один не отдал ни одного tps схватим / by zero
                    .divide(new BigDecimal(Math.max(stat.getThreadCount(), 1)), 2, RoundingMode.HALF_UP);
            //Может случится такое, что потоки встанут на длительную работу или перестанут работать из-за отсутсвия задач
            //И средняя по транзакция будет равна 0
            //System.out.println(threadTps.doubleValue() + "; " + BigDecimal.valueOf(0).doubleValue());
            if (threadTps.doubleValue() == 0.0) {
                if (stat.getQueueSize() > 0) { //Задачи есть, вернём сколько потоков, столько и задач
                    return stat.getQueueSize();
                }
            } else {
                return new BigDecimal(stat.getQueueSize())
                        .divide(threadTps, 2, RoundingMode.HALF_UP)
                        .setScale(0, RoundingMode.CEILING)
                        .intValue();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

}
