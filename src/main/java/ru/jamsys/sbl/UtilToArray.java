package ru.jamsys.sbl;

import ru.jamsys.sbl.service.SblService;
import ru.jamsys.sbl.service.thread.WrapThread;

import java.util.List;

public class UtilToArray {

    /**
    --------------------------------------------------------------------------------------------------------------
    throws Exception - это просто защита от забывчивости
    При длительных (не атомарных) итерированиях можно легко нарваться на Concurrent modification exception
    И к сожалению из-за проблем ковариантности и не отсутсвия конструкторов типов, универсального изящества мы не добъёмся
    public static <T> T[] toArray(List<T> l) throws Exception {
        return (T[]) l.toArray(new T[0]);
    }
    --------------------------------------------------------------------------------------------------------------
    */

    @SuppressWarnings("all")
    public static SblService[] toArraySblService(List<SblService> l) throws Exception {
        return l.toArray(new SblService[0]);
    }

    @SuppressWarnings("all")
    public static WrapThread[] toArrayWrapThread(List<WrapThread> l) throws Exception {
        return l.toArray(new WrapThread[0]);
    }

}
