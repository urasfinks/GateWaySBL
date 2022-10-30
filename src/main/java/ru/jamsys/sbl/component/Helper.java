package ru.jamsys.sbl.component;

import org.springframework.stereotype.Component;
import ru.jamsys.sbl.consumer.SBLConsumer;
import ru.jamsys.sbl.thread.Scheduler;

import java.util.function.Consumer;

@Component
public class Helper extends Scheduler {

    final private ConsumerComponent consumerComponent;

    public Helper(ConsumerComponent consumerComponent) {
        this.consumerComponent = consumerComponent;
    }

    @Override
    protected ConsumerComponent getConsumerComponent() {
        return consumerComponent;
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
    protected Consumer<SBLConsumer> getLogic() {
        return (consumer)->{
            consumer.helper();
        };
    }
}
