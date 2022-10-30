package ru.jamsys.sbl.component;

import org.springframework.stereotype.Component;
import ru.jamsys.sbl.consumer.SBLConsumer;
import ru.jamsys.sbl.message.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Component
public class ConsumerComponent {

    Map<String, SBLConsumer> listConsumerService = new ConcurrentHashMap<>();

    public List<SBLConsumer> getListConsumerService() {
        return new ArrayList<>(listConsumerService.values());
    }

    public SBLConsumer instance(String name, int countThreadMin, int countThreadMax, long keepAlive, Consumer<Message> worker) {
        SBLConsumer cs = new SBLConsumer(name, countThreadMin, countThreadMax, keepAlive, worker);
        listConsumerService.put(name, cs);
        return cs;
    }

    public static <T> T[] toArray(List<T> l) throws Exception {
        return (T[]) l.toArray(new SBLConsumer[0]);
    }

    public SBLConsumer get(String name) {
        return listConsumerService.get(name);
    }

    public void shutdown(String name) {
        SBLConsumer cs = listConsumerService.remove(name);
        cs.shutdown();
    }
}
