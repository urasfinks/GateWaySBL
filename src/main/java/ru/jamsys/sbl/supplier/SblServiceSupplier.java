package ru.jamsys.sbl.supplier;

import lombok.Setter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.jamsys.sbl.SblServiceStatistic;
import ru.jamsys.sbl.Util;
import ru.jamsys.sbl.consumer.SblConsumerShutdownException;
import ru.jamsys.sbl.message.Message;
import ru.jamsys.sbl.thread.SblService;
import ru.jamsys.sbl.thread.SblServiceAbstract;
import ru.jamsys.sbl.thread.WrapThread;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Function;
import java.util.function.Supplier;

@Component
@Scope("prototype")
public class SblServiceSupplier extends SblServiceAbstract implements Supplier<Message> {

    private Supplier<Message> supplier;
    private SblServiceSupplierScheduler scheduler;

    public void configure(String name, int threadCountMin, int threadCountMax, long threadKeepAliveMillis, long threadSleepMillis, Supplier<Message> supplier) {
        if (super.configure(name, threadCountMin, threadCountMax, threadKeepAliveMillis)) {
            this.supplier = supplier;
            scheduler = new SblServiceSupplierScheduler(name + "-Supplier-Scheduler", threadSleepMillis);
            scheduler.run(this);
            overclocking(threadCountMin);
        }
    }

    @Setter
    Function<Integer, Integer> formulaAddCountThread = (y) -> 100;

    @Override
    public void threadStabilizer() {
        try {
            SblServiceStatistic stat = getStatClone();
            if (getThreadParkQueueSize() == 0) {//В очереди нет ждунов, значит все трудятся, накинем ещё
                int needCountThread = formulaAddCountThread.apply(getThreadListSize());
                if (debug) {
                    Util.logConsole(Thread.currentThread(), "AddThread: " + needCountThread);
                }
                overclocking(needCountThread);
            } else if (isThreadRemove(stat)) { //Кол-во потоков больше минимума
                checkKeepAliveAndRemoveThread();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public SblServiceStatistic statistic() {

        return super.statistic();
    }

    public void tick() {
        //При маленькой нагрузке дёргаем всегда последний тред, что бы не было простоев
        //Далее раскрутку оставляем на откуп стабилизатору
        if (isActive()) {
            SblServiceStatistic stat = getStatClone();
            int diffTpsInput = getNeedCountThread(stat, getTpsInputMax(), debug);
            if (isThreadParkAll()) {
                wakeUpOnceThread();
            } else if (diffTpsInput > 0) {
                /*IntStream.range(0, diffTpsInput).forEach(index -> {
                    wakeUpOnceThread();
                });*/
                for (int i = 0; i < diffTpsInput; i++) {
                    wakeUpOnceThread();
                }
            }
        }
    }

    @Override
    public void iteration(WrapThread wrapThread, SblService service) {
        while (isActive() && wrapThread.getIsRun().get() && !isLimitTpsInputOverflow()) {
            incTpsInput();
            long startTime = System.currentTimeMillis();
            Message message = get();
            if (message != null) {
                incTpsOutput(System.currentTimeMillis() - startTime);
            } else {
                break;
            }
        }
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

    public static int getNeedCountThread(SblServiceStatistic stat, int tpsInputMax, boolean debug) {
        int needTransaction = tpsInputMax - stat.getTpsInput();
        if (needTransaction > 0) {
            try {
                BigDecimal threadTps = new BigDecimal(1000)
                        .divide(BigDecimal.valueOf(stat.getSumTimeTpsAvg()), 2, RoundingMode.HALF_UP);
                int needThread = new BigDecimal(needTransaction)
                        .divide(threadTps, 2, RoundingMode.HALF_UP)
                        .setScale(0, RoundingMode.CEILING)
                        .intValue();
                needThread = Math.min(needThread, stat.getThreadCountPark());
//                if (debug) {
//                    Util.logConsole(Thread.currentThread(), "getNeedCountThreadSupplier: needTransaction: " + needTransaction + "; threadTps: " + threadTps + "; needThread: " + needThread + "; " + stat);
//                }
                return needThread;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            if (debug) {
                Util.logConsole(Thread.currentThread(), "getNeedCountThreadSupplier: needTransaction: " + needTransaction + "; threadTps: ?; needThread: 0; " + stat);
            }
        }
        return 0;
    }

}
