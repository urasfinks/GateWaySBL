package ru.jamsys.sbl.consumer;

import ru.jamsys.sbl.SblServiceStatistic;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class SblConsumerUtil {
    public static int getNeedCountThread(SblServiceStatistic stat) {
        try {
            return new BigDecimal(stat.getQueueSize())
                    .divide(
                            new BigDecimal(stat.getTpsOutput())
                                    .divide(new BigDecimal(stat.getThreadCount()), 2, RoundingMode.HALF_UP),
                            2, RoundingMode.HALF_UP
                    )
                    .setScale(0, RoundingMode.CEILING)
                    .intValue();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}
