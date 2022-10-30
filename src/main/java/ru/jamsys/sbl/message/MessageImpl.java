package ru.jamsys.sbl.message;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.sbl.thread.SblService;

import java.util.concurrent.ConcurrentLinkedQueue;

public class MessageImpl implements Message {

    ConcurrentLinkedQueue<String> routeMap = new ConcurrentLinkedQueue<>();
    ConcurrentLinkedQueue<Exception> errorQueue = new ConcurrentLinkedQueue<>();

    @Setter
    @Getter
    private String body = "";

    @Getter
    private final String correlation = java.util.UUID.randomUUID().toString();

    @Override
    public void onHandle(MessageHandle handle, SblService service) {
        routeMap.add(convertTimestamp(System.currentTimeMillis()) + " " + service.getName() + " " + handle.toString());
    }

    @Override
    public void setError(Exception e) {
        errorQueue.add(e);
    }
}
