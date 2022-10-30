package ru.jamsys.sbl.message;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.sbl.consumer.SBLConsumer;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MessageImpl implements Message {

    Queue<String> routeMap = new ConcurrentLinkedQueue();
    Queue<Exception> errorQueue = new ConcurrentLinkedQueue();

    @Setter
    @Getter
    private String body = "";

    @Getter
    private final String correlation = java.util.UUID.randomUUID().toString();

    @Override
    public void onHandle(MessageHandle handle, SBLConsumer service) {
        routeMap.add(convertTimestamp(System.currentTimeMillis()) + " " + service.getName() + " " + handle.toString());
    }

    @Override
    public String getBody() {
        return null;
    }

    @Override
    public void setError(Exception e) {
        errorQueue.add(e);
    }
}
