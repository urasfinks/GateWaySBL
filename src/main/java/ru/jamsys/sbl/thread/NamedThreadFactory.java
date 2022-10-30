package ru.jamsys.sbl.thread;

import java.util.concurrent.ThreadFactory;

public class NamedThreadFactory implements ThreadFactory{
    private int threadsNum;
    private final String namePattern;

    public NamedThreadFactory(String baseName){
        namePattern = baseName + "-%d";
    }

    @Override
    public Thread newThread(Runnable runnable){
        threadsNum++;
        return new Thread(runnable, String.format(namePattern, threadsNum));
    }
}
