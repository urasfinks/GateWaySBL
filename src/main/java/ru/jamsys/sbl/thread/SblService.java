package ru.jamsys.sbl.thread;

import ru.jamsys.sbl.SblServiceStatistic;

import java.util.concurrent.atomic.AtomicInteger;

public interface SblService {

    SblServiceStatistic statistic();

    void helper();

    String getName();

    void iteration(WrapThread wrapThread, SblService service);

    void shutdown();

    void setDebug(boolean b);

    SblServiceStatistic getStatLast();

    void setTpsMainMax(int max);

    int getTpsMainMax();

    AtomicInteger getTpsMain();

}
