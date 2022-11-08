package ru.jamsys.sbl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import reactor.util.annotation.Nullable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class Util {

    @SuppressWarnings("unused")
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

    @SuppressWarnings("unused")
    public static long getTimestamp() {
        return System.currentTimeMillis() / 1000;
    }

    static ObjectMapper objectMapper = new ObjectMapper();

    @Nullable
    public static String jsonObjectToString(Object o) {

        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Nullable
    public static String jsonObjectToStringPretty(Object o) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static <T> WrapJsonToObject<T> jsonToObject(String json, Class<T> t) {
        WrapJsonToObject<T> ret = new WrapJsonToObject<>();
        try {
            ret.setObject(objectMapper.readValue(json, t));
        } catch (Exception e) {
            ret.setException(e);
        }
        return ret;
    }

    public static void sleepMillis(int seconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
