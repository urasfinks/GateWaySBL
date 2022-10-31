package ru.jamsys.sbl.scheduler;

import ru.jamsys.sbl.Util;
import ru.jamsys.sbl.component.CmpService;
import ru.jamsys.sbl.thread.SblService;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class CmpServiceScheduler extends SblSchedulerAbstract {

    public CmpServiceScheduler(String name, int periodMillis) {
        super(name, periodMillis);
    }

    @Override
    public Consumer<Void> getConsumer() {
        return (t) -> {
            try {
                List<Object> objects = Util.forEach(CmpService.toArray(getCmpService().getListService()), getSblServiceHandler());
                Consumer<Object> handler = getResultHandler();
                if (handler != null) {
                    handler.accept(objects);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
    }

    protected CmpService getCmpService() {
        return null;
    }

    protected Function<SblService, Object> getSblServiceHandler() {
        return null;
    }

    protected Consumer<Object> getResultHandler() {
        return null;
    }

}
