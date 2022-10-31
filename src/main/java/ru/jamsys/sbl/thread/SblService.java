package ru.jamsys.sbl.thread;

import ru.jamsys.sbl.SblServiceStatistic;

public interface SblService {

    SblServiceStatistic statistic();

    void threadStabilizer();

    String getName();

    void iteration(WrapThread wrapThread, SblService service);

    void shutdown();

    void setDebug(boolean b);

    SblServiceStatistic getStatClone();

    void incThreadMax();

    void decThreadMax();

    void setTpsInputMax(int maxTps);

}
