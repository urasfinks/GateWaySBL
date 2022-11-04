package ru.jamsys.sbl.component;

import org.springframework.stereotype.Component;
import ru.jamsys.sbl.scheduler.CmpServiceScheduler;
import ru.jamsys.sbl.service.SblService;

import javax.annotation.PreDestroy;
import java.util.function.Function;

@Component
public class CmpServiceStabilizer extends CmpServiceScheduler {

    final private CmpService cmpService;

    public CmpServiceStabilizer(CmpService cmpService) {
        super("ServiceStabilizer", 2000);
        this.cmpService = cmpService;
    }

    @Override
    protected CmpService getCmpService() {
        return cmpService;
    }

    @Override
    protected Function<SblService, Object> getSblServiceHandler() {
        return consumer -> {
            consumer.threadStabilizer();
            return null;
        };
    }

    @PreDestroy
    public void destroy() {
        super.shutdown();
    }

    @Override
    public void tick() {

    }
}
