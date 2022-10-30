package ru.jamsys.sbl;

import java.util.concurrent.TimeUnit;

public class UtilTest {

    public static void sleep(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
