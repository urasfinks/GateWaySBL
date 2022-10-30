package ru.jamsys.sbl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class Util {

    public static String[] toArray(List<String> l) throws Exception {
        //concurrent modification exception, да тут не может быть исключения,
        // но так лучше что бы не забывали обработать исключения при работе в многопоточном доступе
        return l.toArray(new String[0]);
    }

    public static <T> void printArray(T[] arr) {
        System.out.println(Arrays.toString(arr));
    }

    public static <T, R> List<R> forEach(T[] array, Function<T, R> fn) {
        List<R> list = new ArrayList<>();
        for (T item : array) {
            R r = fn.apply(item);
            if (r != null) {
                list.add(r);
            }
        }
        return list;
    }

    public static void logConsole(Thread t, String data) {
        System.out.println(LocalDateTime.now().toString() + " " + t.getName() + " " + data);
    }

    public static long getTimestamp() {
        return System.currentTimeMillis() / 1000;
    }

}
