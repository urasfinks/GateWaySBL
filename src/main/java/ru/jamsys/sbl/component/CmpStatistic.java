package ru.jamsys.sbl.component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.jamsys.sbl.Util;
import ru.jamsys.sbl.json.Statistic;
import ru.jamsys.sbl.scheduler.CmpServiceScheduler;
import ru.jamsys.sbl.service.SblService;
import ru.jamsys.sbl.SblServiceStatistic;

import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import ru.jamsys.sbl.web.GreetingClient;

@Component
public class CmpStatistic extends CmpServiceScheduler {

    private CmpStatisticCpu cmpStatisticCpu;

    private final Map<String, AtomicInteger> shareStat = new ConcurrentHashMap<>();

    public void incShareStatistic(String name) {
        if (!shareStat.containsKey(name)) {
            shareStat.put(name, new AtomicInteger(1));
        } else {
            shareStat.get(name).incrementAndGet();
        }
    }

    @Autowired
    public void setCmpStatisticCpu(CmpStatisticCpu cmpStatisticCpu) {
        this.cmpStatisticCpu = cmpStatisticCpu;
    }

    private GreetingClient greetingClient;

    @Autowired
    public void setGreetingClient(GreetingClient greetingClient) {
        this.greetingClient = greetingClient;
    }


    final private CmpService cmpService;

    public CmpStatistic(CmpService cmpService) {
        super("Statistic", 1000);
        this.cmpService = cmpService;
    }


    @Override
    protected CmpService getCmpService() {
        return cmpService;
    }

    @Override
    protected Function<SblService, Object> getSblServiceHandler() {
        return t -> {
            SblServiceStatistic r = t.statistic();
            return r == null ? null : r.clone();
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Consumer<Object> getResultHandler() {
        return result -> {
            Statistic statistic = new Statistic();
            statistic.setCpu(cmpStatisticCpu.getCpuUsage());
            Map<String, SblServiceStatistic> map = new HashMap<>();
            for (SblServiceStatistic item : (List<SblServiceStatistic>) result) {
                map.put(item.getServiceName(), item);
            }
            statistic.setService(map);

            Map<String, Integer> map2 = new HashMap<>();
            String[] list = shareStat.keySet().toArray(new String[0]);
            for (String item : list) {
                int andSet = shareStat.get(item).getAndSet(0);
//                if (andSet == 0) {
//                    shareStat.remove(item);
//                } else {
//
//                }
                map2.put(item, andSet);
            }
            statistic.setShare(map2);
            //Util.logConsole(Thread.currentThread(), Util.jsonObjectToString(statistic));

            try {
                greetingClient.getMessage(Util.jsonObjectToString(statistic)).block();
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
    }

    @PreDestroy
    public void destroy() {
        super.shutdown();
    }

}
