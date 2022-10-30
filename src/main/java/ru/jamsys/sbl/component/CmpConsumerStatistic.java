package ru.jamsys.sbl.component;

import org.springframework.stereotype.Component;
import ru.jamsys.sbl.CmpConsumerScheduler;
import ru.jamsys.sbl.Util;
import ru.jamsys.sbl.consumer.SblConsumer;

import javax.annotation.PreDestroy;
import java.util.function.Consumer;
import java.util.function.Function;

@Component
public class CmpConsumerStatistic extends CmpConsumerScheduler {

    final private CmpConsumer cmpConsumer;

    public CmpConsumerStatistic(CmpConsumer cmpConsumer) {
        this.cmpConsumer = cmpConsumer;
    }

    @Override
    protected CmpConsumer getConsumerComponent() {
        return cmpConsumer;
    }

    @Override
    protected String getThreadName() {
        return "Statistic";
    }

    @Override
    protected int getPeriod() {
        return 1;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T,R> Function<T,R> getConsumer() {
        return consumer -> (R) ((SblConsumer) consumer).statistic().clone();
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
