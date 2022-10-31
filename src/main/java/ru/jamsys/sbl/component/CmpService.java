package ru.jamsys.sbl.component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import ru.jamsys.sbl.consumer.SblServiceConsumer;
import ru.jamsys.sbl.consumer.SblConsumerShutdownException;
import ru.jamsys.sbl.message.Message;
import ru.jamsys.sbl.supplier.SblServiceSupplier;
import ru.jamsys.sbl.thread.SblService;

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

    public SblService instance(String name, int countThreadMin, int countThreadMax, long keepAliveMills, Consumer<Message> consumer) {
        SblServiceConsumer sblServiceConsumer = context.getBean(SblServiceConsumer.class);
        sblServiceConsumer.configure(name, countThreadMin, countThreadMax, keepAliveMills, consumer);
        listService.put(name, sblServiceConsumer);
        return sblServiceConsumer;
    }

    public SblService instance(String name, int countThreadMin, int countThreadMax, long keepAlive, long threadSleepMillis, Supplier<Message> supplier) {
        SblServiceSupplier sblServiceSupplier = context.getBean(SblServiceSupplier.class);
        sblServiceSupplier.configure(name, countThreadMin, countThreadMax, keepAlive, threadSleepMillis, supplier);
        listService.put(name, sblServiceSupplier);
        return sblServiceSupplier;
    }

    public static SblService[] toArray(List<SblService> l) throws Exception {
        return l.toArray(new SblService[0]);
    }

    public SblService get(String name) {
        return listService.get(name);
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
