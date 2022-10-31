package ru.jamsys.sbl.supplier;


import ru.jamsys.sbl.message.Message;
import ru.jamsys.sbl.thread.SblService;
import ru.jamsys.sbl.thread.SblServiceImpl;
import ru.jamsys.sbl.thread.WrapThread;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class SblServiceSupplier extends SblServiceImpl implements Supplier<Message> {

    private final Supplier<Message> supplier;

    protected volatile int tpsOutputMax = -1; //-1 infinity

    public SblServiceSupplier(String name, int threadCountMin, int threadCountMax, long threadKeepAlive, Supplier<Message> supplier) {
        super(name, threadCountMin, threadCountMax, threadKeepAlive);
        this.supplier = supplier;
        overclocking(threadCountMin);
    }

    @Override
    public void helper() {

    }

    @Override
    public void iteration(WrapThread wrapThread, SblService service) {
        while (wrapThread.getIsRun().get() && !isLimitTpsMain()) {
            Message message = get();
            if (message != null) {
                incTpsMain();
            } else {
                break;
            }
        }
    }

    @Override
    public void setTpsMainMax(int max) {
        tpsOutputMax = max;
    }

    @Override
    public AtomicInteger getTpsMain() {
        return tpsOutput;
    }

    @Override
    public int getTpsMainMax() {
        return tpsOutputMax;
    }

    @Override
    public Message get() {
        return supplier.get();
    }
}
