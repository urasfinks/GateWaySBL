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
import java.util.function.Consumer;
import java.util.function.Function;

import ru.jamsys.sbl.web.GreetingClient;

@Component
public class CmpStatistic extends CmpServiceScheduler {

    private CmpStatisticCpu cmpStatisticCpu;

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
            statistic.setCpu((int) cmpStatisticCpu.getCpuUsage());
            Map<String, SblServiceStatistic> map = new HashMap<>();
            for (SblServiceStatistic item : (List<SblServiceStatistic>) result) {
                map.put(item.getServiceName(), item);
            }
            statistic.setService(map);
            //Util.logConsole(Thread.currentThread(), Util.jsonObjectToString(statistic));

            greetingClient.getMessage(Util.jsonObjectToString(statistic)).block();
        };
    }

    @PreDestroy
    public void destroy() {
        super.shutdown();
    }

}
