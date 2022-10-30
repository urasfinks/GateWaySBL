package ru.jamsys.sbl.consumer;

import lombok.Data;

@Data
public class ConsumerStatistic implements Cloneable {
    int countOperationUseful;
    int countOperationUseless;
    int countThread;
    int queueSize;
    int tps;
    int overclockLimit;

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public static ConsumerStatistic instance(int countOperationUseful, int countThread, int queueSize) {
        ConsumerStatistic t = new ConsumerStatistic();
        t.setCountOperationUseful(countOperationUseful);
        t.setCountThread(countThread);
        t.setQueueSize(queueSize);
        return t;
    }
}
