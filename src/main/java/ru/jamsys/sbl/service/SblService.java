package ru.jamsys.sbl.service;

import ru.jamsys.sbl.SblServiceStatistic;
import ru.jamsys.sbl.service.thread.WrapThread;

public interface SblService {

    SblServiceStatistic statistic();

    void threadStabilizer();

    String getName();

    void iteration(WrapThread wrapThread, SblService service);

    void shutdown();

    void setDebug(boolean b);

    SblServiceStatistic getStatClone();

    @SuppressWarnings("unused")
    void incThreadMax();

    @SuppressWarnings("unused")
    void decThreadMax();

    void setTpsInputMax(int maxTps);

}
