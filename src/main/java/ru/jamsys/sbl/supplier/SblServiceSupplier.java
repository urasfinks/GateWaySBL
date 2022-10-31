package ru.jamsys.sbl.supplier;


import lombok.Getter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.jamsys.sbl.SblServiceStatistic;
import ru.jamsys.sbl.Util;
import ru.jamsys.sbl.consumer.SblConsumerShutdownException;
import ru.jamsys.sbl.message.Message;
import ru.jamsys.sbl.thread.SblService;
import ru.jamsys.sbl.thread.SblServiceAbstract;
import ru.jamsys.sbl.thread.WrapThread;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Getter
@Component
@Scope("prototype")
public class SblServiceSupplier extends SblServiceAbstract implements Supplier<Message> {

    private Supplier<Message> supplier;

    protected volatile int tpsOutputMax = -1; //-1 infinity

    private SblServiceSupplierScheduler scheduler;

    public void configure(String name, int threadCountMin, int threadCountMax, long threadKeepAliveMillis, long threadSleepMillis, Supplier<Message> supplier) {
        if (super.configure(name, threadCountMin, threadCountMax, threadKeepAliveMillis)) {
            this.supplier = supplier;
            scheduler = new SblServiceSupplierScheduler(name + "-Scheduler", threadSleepMillis);
            scheduler.run(this);
            overclocking(threadCountMin);
        }
    }

    @Override
    public void helper() {
        try {
            SblServiceStatistic stat = statLast.clone();
            if (debug) {
                Util.logConsole(Thread.currentThread(), "CountThread: " + stat.getThreadCount());
            }
            if (threadParkQueue.size() == 0) { //Добавляем ровно столько же
                overclocking(threadList.size());
            } else if (stat.getThreadCount() > threadCountMin) { //нет необходимости удалять, когда потоков заявленный минимум
                checkKeepAliveAndRemoveThread();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void tick() {
        if (isActive() && threadParkQueue.size() > 0) { //При маленькой нагрузке будет дёргаться всегда последний тред, а все остальные под нож
            wakeUpThread();
        }
    }

    @Override
    public void iteration(WrapThread wrapThread, SblService service) {
        while (wrapThread.getIsRun().get() && !isLimitTpsMain()) {
            Message message = get();
            if (message != null) {
                incTpsMain();
            } else {
                break;
            }
        }
    }

    @Override
    public void setTpsMainMax(int max) {
        tpsOutputMax = max;
    }

    @Override
    public AtomicInteger getTpsMain() {
        return tpsOutput;
    }

    @Override
    public int getTpsMainMax() {
        return tpsOutputMax;
    }

    @Override
    public Message get() {
        return supplier.get();
    }

    @Override
    public void shutdown() throws SblConsumerShutdownException {
        super.shutdown();
        scheduler.shutdown();
    }

}
