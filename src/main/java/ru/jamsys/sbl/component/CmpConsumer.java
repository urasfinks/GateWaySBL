package ru.jamsys.sbl.component;

import org.springframework.stereotype.Component;
import ru.jamsys.sbl.consumer.SblConsumer;
import ru.jamsys.sbl.consumer.SblConsumerShutdownException;
import ru.jamsys.sbl.message.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Component
public class CmpConsumer {

    Map<String, SblConsumer> listConsumerService = new ConcurrentHashMap<>();

    public List<SblConsumer> getListConsumerService() {
        return new ArrayList<>(listConsumerService.values());
    }

    public SblConsumer instance(String name, int countThreadMin, int countThreadMax, long keepAlive, Consumer<Message> worker) {
        SblConsumer cs = new SblConsumer(name, countThreadMin, countThreadMax, keepAlive, worker);
        listConsumerService.put(name, cs);
        return cs;
    }

    public static SblConsumer[] toArray(List<SblConsumer> l) throws Exception {
        return l.toArray(new SblConsumer[0]);
    }

    public SblConsumer get(String name) {
        return listConsumerService.get(name);
    }

    public void shutdown(String name) {
        // Так как shutdown публичный метод, его может вызвать кто-то другой, поэтому будем ждать пока сервис остановится
        SblConsumer cs = listConsumerService.get(name);
        while (true) {
            try {
                cs.shutdown();
                break;
            } catch (SblConsumerShutdownException e) {
                e.printStackTrace();
            }
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        listConsumerService.remove(name);
    }

}
