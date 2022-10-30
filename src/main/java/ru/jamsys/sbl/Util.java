package ru.jamsys.sbl;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class Util {

    public static <T> T[] copyToArrayString(List<T> l) throws Exception { //concurrent modification exception, да не панацея, но так лучше
        T[] ts = (T[]) l.toArray(new String[0]);
        return ts;
    }

    public static <T> void printArray(T[] arr) {
        System.out.println(Arrays.toString(arr));
    }

    public static <T> void forEach(T[] array, Consumer<T> fn) {
        for (T item : array) {
            fn.accept(item);
        }
    }

    public static void logConsole(String data) {
        System.out.println(LocalDateTime.now().toString() + " " + data);
    }

    public static long getTimestamp(){
        return System.currentTimeMillis() / 1000;
    }
}
