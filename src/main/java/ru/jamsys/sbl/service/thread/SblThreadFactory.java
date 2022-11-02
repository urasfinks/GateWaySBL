package ru.jamsys.sbl.service.thread;

import java.util.concurrent.ThreadFactory;

public class SblThreadFactory implements ThreadFactory {

    private int threadsNum;
    private final String namePattern;

    public SblThreadFactory(String baseName) {
        namePattern = baseName + "-%d";
    }

    @Override
    public Thread newThread(Runnable runnable) {
        threadsNum++;
        return new Thread(runnable, String.format(namePattern, threadsNum));
    }
    
}
