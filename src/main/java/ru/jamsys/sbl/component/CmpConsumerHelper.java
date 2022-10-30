package ru.jamsys.sbl.component;

import org.springframework.stereotype.Component;
import ru.jamsys.sbl.CmpConsumerScheduler;
import ru.jamsys.sbl.consumer.SblConsumer;

import javax.annotation.PreDestroy;
import java.util.function.Function;

@Component
public class CmpConsumerHelper extends CmpConsumerScheduler {

    final private CmpConsumer cmpConsumer;

    public CmpConsumerHelper(CmpConsumer cmpConsumer) {
        this.cmpConsumer = cmpConsumer;
    }

    @Override
    protected CmpConsumer getConsumerComponent() {
        return cmpConsumer;
    }

    @Override
    protected String getThreadName() {
        return "Helper";
    }

    @Override
    protected int getPeriod() {
        return 2;
    }

    @Override
    protected <T,R> Function<T,R> getConsumer() {
        return consumer -> {
            ((SblConsumer)consumer).helper();
            return null;
        };
    }

    @PreDestroy
    public void destroy() {
        super.shutdown();
    }

}
