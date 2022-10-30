package ru.jamsys.sbl.component;

import org.springframework.stereotype.Component;
import ru.jamsys.sbl.scheduler.CmpServiceSchedulerImpl;
import ru.jamsys.sbl.thread.SblService;

import javax.annotation.PreDestroy;
import java.util.function.Function;

@Component
public class CmpHelper extends CmpServiceSchedulerImpl {

    final private CmpService cmpService;

    public CmpHelper(CmpService cmpService) {
        this.cmpService = cmpService;
    }

    @Override
    public CmpService getComponentService() {
        return cmpService;
    }

    @Override
    public String getThreadName() {
        return "Helper";
    }

    @Override
    protected int getPeriod() {
        return 2;
    }

    @Override
    public <T,R> Function<T,R> getConsumer() {
        return consumer -> {
            ((SblService)consumer).helper();
            return null;
        };
    }

    @PreDestroy
    public void destroy() {
        super.shutdown();
    }

}
