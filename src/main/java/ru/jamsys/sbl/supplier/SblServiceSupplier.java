package ru.jamsys.sbl.supplier;

import ru.jamsys.sbl.message.Message;
import ru.jamsys.sbl.thread.SblService;
import ru.jamsys.sbl.thread.SblServiceImpl;
import ru.jamsys.sbl.SblServiceStatistic;
import ru.jamsys.sbl.thread.WrapThread;

import java.util.function.Supplier;

public class SblServiceSupplier extends SblServiceImpl implements Supplier<Message> {

    private final Supplier<Message> supplier;

    public SblServiceSupplier(String name, int threadCountMin, int threadCountMax, long threadKeepAlive, Supplier<Message> supplier) {
        super(name, threadCountMin, threadCountMax, threadKeepAlive);
        this.supplier = supplier;
        overclocking(threadCountMin);
    }

    @Override
    public SblServiceStatistic statistic() {
        return null;
    }

    @Override
    public void helper() {
        System.out.println("Opa Helper");
    }

    @Override
    public void iteration(WrapThread wrapThread, SblService service) {

    }

    @Override
    public Message get() {
        return null;
    }
}
