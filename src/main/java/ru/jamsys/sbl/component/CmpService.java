package ru.jamsys.sbl.component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import ru.jamsys.sbl.service.SblServiceConsumer;
import ru.jamsys.sbl.service.consumer.SblConsumerShutdownException;
import ru.jamsys.sbl.message.Message;
import ru.jamsys.sbl.service.SblServiceSupplier;
import ru.jamsys.sbl.service.SblService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Component
public class CmpService {

    private ApplicationContext context;

    @Autowired
    public void setContext(ApplicationContext context) {
        this.context = context;
    }

    Map<String, SblService> listService = new ConcurrentHashMap<>();

    public List<SblService> getListService() {
        return new ArrayList<>(listService.values());
    }

    public SblService instance(String name, int countThreadMin, int countThreadMax, long keepAliveMills, long schedulerSleepMillis, Consumer<Message> consumer) {
        SblServiceConsumer sblServiceConsumer = context.getBean(SblServiceConsumer.class);
        sblServiceConsumer.configure(name, countThreadMin, countThreadMax, keepAliveMills, schedulerSleepMillis, consumer);
        listService.put(name, sblServiceConsumer);
        return sblServiceConsumer;
    }

    public SblService instance(String name, int countThreadMin, int countThreadMax, long keepAlive, long schedulerSleepMillis, Supplier<Message> supplier, Consumer<Message> consumer) {
        SblServiceSupplier sblServiceSupplier = context.getBean(SblServiceSupplier.class);
        sblServiceSupplier.configure(name, countThreadMin, countThreadMax, keepAlive, schedulerSleepMillis, supplier, consumer);
        listService.put(name, sblServiceSupplier);
        return sblServiceSupplier;
    }

    public void shutdown(String name) {
        // Так как shutdown публичный метод, его может вызвать кто-то другой, поэтому будем ждать пока сервис остановится
        SblService cs = listService.get(name);
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
        listService.remove(name);
    }

}
