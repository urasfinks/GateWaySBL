package ru.jamsys.sbl.thread;

import ru.jamsys.sbl.SblServiceStatistic;

public interface SblService {

    SblServiceStatistic statistic();
    void helper();
    String getName();
    void iteration(WrapThread wrapThread, SblService service);

    void shutdown();

    void setDebug(boolean b);

    void setTpsInputMax(int tpsInputMax);

    SblServiceStatistic getStatLast();
}
