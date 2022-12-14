package ru.jamsys.sbl.service;

import lombok.NonNull;
import lombok.Setter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.jamsys.sbl.SblServiceStatistic;
import ru.jamsys.sbl.message.MessageHandle;
import ru.jamsys.sbl.scheduler.SblSchedulerTick;
import ru.jamsys.sbl.Util;
import ru.jamsys.sbl.message.Message;
import ru.jamsys.sbl.service.thread.SblServiceAbstract;
import ru.jamsys.sbl.service.thread.WrapThread;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Component
@Scope("prototype")
public class SblServiceSupplier extends SblServiceAbstract implements Supplier<Message>, Consumer<Message>, SblSchedulerTick {

    private Supplier<Message> supplier;
    private Consumer<Message> consumer;

    public void configure(String name, int threadCountMin, int threadCountMax, long threadKeepAliveMillis, long schedulerSleepMillis, Supplier<Message> supplier, Consumer<Message> consumer) {
        this.supplier = supplier;
        this.consumer = consumer;
        super.configure(name, threadCountMin, threadCountMax, threadKeepAliveMillis, schedulerSleepMillis);
    }

    @Override
    protected void removeThread(WrapThread wth) {
        //Кол-во неприкасаемых потоков, которые должны быть на паркинге для подстраховки (Натуральное число)
        int threadParkMinimum = 5;
        if (getThreadParkQueueSize() > threadParkMinimum) {
            super.removeThread(wth);
        }
    }

    @Setter
    Function<Integer, Integer> formulaAddCountThread = (y) -> y;

    @Override
    public void threadStabilizer() {
        try {
            SblServiceStatistic stat = getStatClone();
            if (stat != null) {
                if (getThreadParkQueueSize() == 0) {//В очереди нет ждунов, значит все трудятся, накинем ещё
                    int needCountThread = formulaAddCountThread.apply(getThreadListSize());
                    if (debug) {
                        Util.logConsole(Thread.currentThread(), "AddThread: " + needCountThread);
                    }
                    overclocking(needCountThread);
                } else if (isThreadRemove(stat)) { //Кол-во потоков больше минимума
                    checkKeepAliveAndRemoveThread();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void tick() {
        //При маленькой нагрузке дёргаем всегда последний тред, что бы не было простоев
        //Далее раскрутку оставляем на откуп стабилизатору
        if (isActive()) {
            SblServiceStatistic stat = getStatCurrent();
            int diffTpsInput = getNeedCountThread(stat, getTpsInputMax(), debug);
            if (isThreadParkAll()) {
                wakeUpOnceThread();
            } else if (diffTpsInput > 0) {
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
                message.onHandle(MessageHandle.CREATE, this);
                if (consumer != null) {
                    consumer.accept(message);
                }
            } else {
                break;
            }
        }
    }

    @Override
    public Message get() {
        return supplier.get();
    }

    public static int getNeedCountThread(@NonNull SblServiceStatistic stat, int tpsInputMax, boolean debug) {
        int needTransaction = tpsInputMax - stat.getTpsInput();
        int needThread = 0;
        BigDecimal threadTps = null;
        if (needTransaction > 0) {
            // Может возникнуть такая ситуация, когда за 1 секунду не будет собрана статистика
            if (stat.getSumTimeTpsAvg() > 0) {
                try {
                    threadTps = new BigDecimal(1000)
                            .divide(BigDecimal.valueOf(stat.getSumTimeTpsAvg()), 2, RoundingMode.HALF_UP);

                    if (threadTps.doubleValue() == 0.0) {
                        //Может случится такое, что потоки встанут на длительную работу или это просто начало
                        //И средняя по транзакция будет равна 0
                        //Пока думаю, освежу всех в отстойние)
                        needThread = stat.getThreadCountPark();
                    } else {
                        needThread = new BigDecimal(needTransaction)
                                .divide(threadTps, 2, RoundingMode.HALF_UP)
                                .setScale(0, RoundingMode.CEILING)
                                .intValue();
                        needThread = Math.min(needThread, stat.getThreadCountPark()); //Если необходимое число транзакций больше чем кол-во припаркованныех потоков
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                // getSumTimeTpsAvg = 0 => / zero, если нет статистики значит, все потоки встали на одну транзакцию
                // Но могут быть запаркованные с предыдущей операции
                needThread = stat.getThreadCountPark();
            }
        }
        if (debug) {
            Util.logConsole(Thread.currentThread(), "getNeedCountThreadSupplier: needTransaction: " + needTransaction + "; threadTps: " + threadTps + "; needThread: " + needThread + "; " + stat);
        }
        return needThread;
    }

    @Override
    public void accept(Message message) {

    }
}
