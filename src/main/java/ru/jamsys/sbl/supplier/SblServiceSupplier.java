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

import java.util.function.Function;
import java.util.function.Supplier;

@Component
@Scope("prototype")
public class SblServiceSupplier extends SblServiceAbstract implements Supplier<Message> {

    private Supplier<Message> supplier;

    protected volatile int tpsOutputMax = -1; //-1 infinity

    private SblServiceSupplierScheduler scheduler;

    public void configure(String name, int threadCountMin, int threadCountMax, long threadKeepAliveMillis, long threadSleepMillis, Supplier<Message> supplier) {
        if (super.configure(name, threadCountMin, threadCountMax, threadKeepAliveMillis)) {
            this.supplier = supplier;
            scheduler = new SblServiceSupplierScheduler(name + "-Supplier-Scheduler", threadSleepMillis);
            scheduler.run(this);
            overclocking(threadCountMin);
        }
    }

    public Function<Integer, Integer> getNeedCountThread() {
        return (y) -> 100;
    }

    @Override
    public void threadStabilizer() {
        try {
            SblServiceStatistic stat = getStatClone();
            if (getThreadParkQueueSize() == 0) {//В очереди нет ждунов, значит все трудятся, накинем ещё
                if (debug) {
                    Util.logConsole(Thread.currentThread(), "AddThread: " + getThreadListSize());
                }
                overclocking(getNeedCountThread().apply(getThreadListSize())); //Добавляем ровно столько же
            } else if (isThreadRemove(stat)) { //нет необходимости удалять, когда потоков заявленный минимум
                checkKeepAliveAndRemoveThread();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void tick() {
        //При маленькой нагрузке дёргаем всегда последний тред, что бы не было простоев
        //Далее раскрутку оставляем на откуп стабилизатору
        if (isActive() && isThreadParkAll()) {
            wakeUpOnceThread();
        }
    }

    @Override
    public void iteration(WrapThread wrapThread, SblService service) {
        while (isActive() && wrapThread.getIsRun().get() && !isLimitTpsInputOverflow()) {
            incTpsInput();
            Message message = get();
            if (message != null) {
                incTpsOutput();
            } else {
                break;
            }
        }
        //System.out.println("Finish: isActive: " + isActive + "; threadIsRun: " + wrapThread.getIsRun().get() + "; isLimitOverflow: " + (isLimitTpsMainOverflow()));
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
