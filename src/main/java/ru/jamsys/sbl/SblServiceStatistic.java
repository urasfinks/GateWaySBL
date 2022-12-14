package ru.jamsys.sbl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import reactor.util.annotation.Nullable;

import java.util.LongSummaryStatistics;
import java.util.concurrent.ConcurrentLinkedDeque;

@Data
public class SblServiceStatistic implements Cloneable {

    @JsonIgnore
    String serviceName;

    int threadCount;
    int queueSize;
    int tpsInput;
    int tpsOutput;
    int tpsIdle;
    int threadCountPark;

    long sumTimeTpsMax;
    long sumTimeTpsMin;
    double sumTimeTpsAvg;

    public void setTimeTransaction(ConcurrentLinkedDeque<Long> queue) {
        LongSummaryStatistics avgTimeTps = queue.stream().mapToLong(Long::longValue).summaryStatistics();
        sumTimeTpsMax = avgTimeTps.getMax();
        sumTimeTpsMin = avgTimeTps.getMin();
        sumTimeTpsAvg = avgTimeTps.getMin();
    }

    @Nullable
    public SblServiceStatistic clone() {
        try {
            return (SblServiceStatistic) super.clone();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static SblServiceStatistic instanceConsumerTest(int tpsOutput, int threadCount, int queueSize, int tpsInput) {
        SblServiceStatistic t = new SblServiceStatistic();
        t.setTpsOutput(tpsOutput);
        t.setThreadCount(threadCount);
        t.setQueueSize(queueSize);
        t.setTpsInput(tpsInput);
        return t;
    }

    public static SblServiceStatistic instanceSupplierTest(int sumTimeTpsAvg, int threadCount, int threadCountPark, int tpsInput) {
        SblServiceStatistic t = new SblServiceStatistic();
        t.setSumTimeTpsAvg(sumTimeTpsAvg);
        t.setThreadCount(threadCount);
        t.setThreadCountPark(threadCountPark);
        t.setTpsInput(tpsInput);
        return t;
    }

}
