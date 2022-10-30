package ru.jamsys.sbl;

import lombok.Data;

@Data
public class SblServiceStatistic implements Cloneable {

    int threadCount;
    int queueSize;
    int tpsInput;
    int tpsOutput;
    int tpsIdle;
    int threadCountPark;

    public SblServiceStatistic clone() {
        try {
            return (SblServiceStatistic) super.clone();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static SblServiceStatistic instance(int tpsOutput, int threadCount, int queueSize) {
        SblServiceStatistic t = new SblServiceStatistic();
        t.setTpsOutput(tpsOutput);
        t.setThreadCount(threadCount);
        t.setQueueSize(queueSize);
        return t;
    }

}
