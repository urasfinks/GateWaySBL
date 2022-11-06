package ru.jamsys.sbl;

import com.fasterxml.jackson.core.JsonProcessingException;
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
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        try {
            return objectMapper.writeValueAsString(o);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> WrapJsonToObject jsonToObject(String json, Class<T> t) {
        try {
            return new WrapJsonToObject(objectMapper.readValue(json, t), null);
        }catch (Exception e){
            return new WrapJsonToObject(null, e);
        }
    }

    public static void sleepMillis(int seconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
