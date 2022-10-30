package ru.jamsys.sbl.component;

import org.springframework.stereotype.Component;
import ru.jamsys.sbl.scheduler.CmpServiceSchedulerImpl;
import ru.jamsys.sbl.Util;
import ru.jamsys.sbl.thread.SblService;
import ru.jamsys.sbl.SblServiceStatistic;

import javax.annotation.PreDestroy;
import java.util.function.Consumer;
import java.util.function.Function;

@Component
public class CmpStatistic extends CmpServiceSchedulerImpl {

    final private CmpService cmpService;

    public CmpStatistic(CmpService cmpService) {
        this.cmpService = cmpService;
    }

    @Override
    public CmpService getComponentService() {
        return cmpService;
    }


    @Override
    public String getThreadName() {
        return "Statistic";
    }

    @Override
    protected int getPeriod() {
        return 1;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T, R> Function<T, R> getConsumer() {
        return t -> {
            SblServiceStatistic r =  ((SblService) t).statistic();
            return r == null ? null : (R) r.clone();
        };
    }

    @Override
    protected <T> Consumer<T> getHandler() {
        return result -> Util.logConsole(Thread.currentThread(), result.toString());
    }

    @PreDestroy
    public void destroy() {
        super.shutdown();
    }

}
