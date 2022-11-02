package ru.jamsys.sbl.scheduler;

import reactor.util.annotation.Nullable;
import ru.jamsys.sbl.Util;
import ru.jamsys.sbl.UtilToArray;
import ru.jamsys.sbl.component.CmpService;
import ru.jamsys.sbl.service.SblService;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class CmpServiceScheduler extends SblSchedulerAbstract {

    public CmpServiceScheduler(String name, int periodMillis) {
        super(name, periodMillis);
    }

    @Override
    public <T> Consumer<T> getConsumer() {
        return (t) -> {
            try {
                CmpService cmpService = getCmpService();
                if (cmpService != null) {
                    List<Object> objects = Util.forEach(UtilToArray.toArraySblService(cmpService.getListService()), getSblServiceHandler());
                    Consumer<Object> handler = getResultHandler();
                    if (handler != null) {
                        handler.accept(objects);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
    }

    @Nullable
    protected CmpService getCmpService() {
        return null;
    }

    @Nullable
    protected Function<SblService, Object> getSblServiceHandler() {
        return null;
    }

    @Nullable
    protected Consumer<Object> getResultHandler() {
        return null;
    }

}
