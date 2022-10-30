package ru.jamsys.sbl.consumer;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ConsumerUtil {
    public static int getNeedCountThread(ConsumerStatistic stat) {
        try {
            return new BigDecimal(stat.getQueueSize())
                    .divide(
                            new BigDecimal(stat.getCountOperationUseful())
                                    .divide(new BigDecimal(stat.getCountThread()), 2, RoundingMode.HALF_UP),
                            2, RoundingMode.HALF_UP
                    )
                    .setScale(0, RoundingMode.CEILING)
                    .intValue();
        } catch (Exception e) {
        }
        return 0;
    }
}
