package ru.jamsys.sbl.component;

import org.springframework.stereotype.Component;
import ru.jamsys.sbl.CmpConsumerScheduler;
import ru.jamsys.sbl.consumer.SblConsumer;

import javax.annotation.PreDestroy;
import java.util.function.Consumer;

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
    protected Consumer<SblConsumer> getConsumer() {
        return SblConsumer::statistic;
    }

    @PreDestroy
    public void destroy() {
        super.shutdown();
    }

}
