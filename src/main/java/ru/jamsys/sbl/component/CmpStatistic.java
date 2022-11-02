package ru.jamsys.sbl.component;

import org.springframework.stereotype.Component;
import ru.jamsys.sbl.Util;
import ru.jamsys.sbl.scheduler.CmpServiceScheduler;
import ru.jamsys.sbl.service.SblService;
import ru.jamsys.sbl.SblServiceStatistic;

import javax.annotation.PreDestroy;
import java.util.function.Consumer;
import java.util.function.Function;

@Component
public class CmpStatistic extends CmpServiceScheduler {

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

    @Override
    protected Consumer<Object> getResultHandler() {
        return result -> Util.logConsole(Thread.currentThread(), result.toString());
    }

    @PreDestroy
    public void destroy() {
        super.shutdown();
    }

}
