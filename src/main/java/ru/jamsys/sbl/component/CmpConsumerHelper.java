package ru.jamsys.sbl.component;

import org.springframework.stereotype.Component;
import ru.jamsys.sbl.CmpConsumerScheduler;
import ru.jamsys.sbl.consumer.SblConsumer;

import javax.annotation.PreDestroy;
import java.util.function.Consumer;

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
    protected Consumer<SblConsumer> getConsumer() {
        return SblConsumer::helper;
    }

    @PreDestroy
    public void destroy() {
        super.shutdown();
    }

}
