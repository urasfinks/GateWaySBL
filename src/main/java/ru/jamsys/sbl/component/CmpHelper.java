package ru.jamsys.sbl.component;

import org.springframework.stereotype.Component;
import ru.jamsys.sbl.scheduler.CmpServiceScheduler;
import ru.jamsys.sbl.thread.SblService;

import javax.annotation.PreDestroy;
import java.util.function.Function;

@Component
public class CmpHelper extends CmpServiceScheduler {

    final private CmpService cmpService;

    public CmpHelper(CmpService cmpService) {
        super("Helper", 2000);
        this.cmpService = cmpService;
    }

    @Override
    protected CmpService getCmpService() {
        return cmpService;
    }

    @Override
    protected Function<SblService, Object> getSblServiceHandler() {
        return consumer -> {
            consumer.helper();
            return null;
        };
    }

    @PreDestroy
    public void destroy() {
        super.shutdown();
    }
}
