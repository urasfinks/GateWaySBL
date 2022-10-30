package ru.jamsys.sbl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.sbl.consumer.SblConsumerUtil;

class SblSblServiceConsumerUtilTest {

    @Test
    void getNeedCountThread() {
        Assertions.assertEquals(0, SblConsumerUtil.getNeedCountThread(SblServiceStatistic.instance(100,1,0)));
        Assertions.assertEquals(1, SblConsumerUtil.getNeedCountThread(SblServiceStatistic.instance(10,1,10)));
        Assertions.assertEquals(2, SblConsumerUtil.getNeedCountThread(SblServiceStatistic.instance(10,1,20)));
        Assertions.assertEquals(3, SblConsumerUtil.getNeedCountThread(SblServiceStatistic.instance(10,1,30)));
        Assertions.assertEquals(0, SblConsumerUtil.getNeedCountThread(SblServiceStatistic.instance(10,2,0)));
        Assertions.assertEquals(1, SblConsumerUtil.getNeedCountThread(SblServiceStatistic.instance(10,2,5)));
        Assertions.assertEquals(2, SblConsumerUtil.getNeedCountThread(SblServiceStatistic.instance(10,2,10)));
        Assertions.assertEquals(6, SblConsumerUtil.getNeedCountThread(SblServiceStatistic.instance(10,2,30)));
        Assertions.assertEquals(1, SblConsumerUtil.getNeedCountThread(SblServiceStatistic.instance(100,2,30)));
        Assertions.assertEquals(1, SblConsumerUtil.getNeedCountThread(SblServiceStatistic.instance(100,3,30)));
        Assertions.assertEquals(9, SblConsumerUtil.getNeedCountThread(SblServiceStatistic.instance(100,3,300)));

        //Плохие сценарии
        Assertions.assertEquals(0, SblConsumerUtil.getNeedCountThread(SblServiceStatistic.instance(0,1,300)));
    }
}