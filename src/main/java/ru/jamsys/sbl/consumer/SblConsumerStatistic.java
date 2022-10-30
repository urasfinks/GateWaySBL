package ru.jamsys.sbl.consumer;

import lombok.Data;

@Data
public class SblConsumerStatistic implements Cloneable {

    int threadCount;
    int queueSize;
    int tpsInput;
    int tpsOutput;
    int tpsIdle;
    int threadCountPark;

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public static SblConsumerStatistic instance(int tpsOutput, int threadCount, int queueSize) {
        SblConsumerStatistic t = new SblConsumerStatistic();
        t.setTpsOutput(tpsOutput);
        t.setThreadCount(threadCount);
        t.setQueueSize(queueSize);
        return t;
    }

}
