package ru.jamsys.sbl.component;

import org.springframework.stereotype.Component;
import ru.jamsys.sbl.consumer.SBLConsumer;
import ru.jamsys.sbl.thread.Scheduler;

import java.util.function.Consumer;

@Component
public class Statistic extends Scheduler {

    final private ConsumerComponent consumerComponent;

    public Statistic(ConsumerComponent consumerComponent) {
        this.consumerComponent = consumerComponent;
    }

    @Override
    protected ConsumerComponent getConsumerComponent() {
        return consumerComponent;
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
    protected Consumer<SBLConsumer> getLogic() {
        return (consumer)->{
            consumer.statistic();
        };
    }
}
