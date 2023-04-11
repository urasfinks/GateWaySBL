package ru.jamsys.sbl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.core.env.Environment;
import reactor.util.annotation.Nullable;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
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

    static ObjectMapper objectMapper2 = new ObjectMapper();

    public static <T> WrapJsonToObject<T> jsonToObjectOverflowProperties(String json, Class<T> t) {
        WrapJsonToObject<T> ret = new WrapJsonToObject<>();
        try {
            objectMapper2.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            ret.setObject(objectMapper2.readValue(json, t));
        } catch (Exception e) {
            ret.setException(e);
            e.printStackTrace();
        }
        return ret;
    }

    public static <T> WrapJsonToObject<T> jsonToObject(String json, Class<T> t) {
        WrapJsonToObject<T> ret = new WrapJsonToObject<>();
        try {
            ret.setObject(objectMapper.readValue(json, t));
        } catch (Exception e) {
            ret.setException(e);
            e.printStackTrace();
        }
        return ret;
    }

    private static Environment env = null;

    public static String getApplicationProperties(String key) throws Exception {
        if (env == null) {
            env = SblApplication.context.getBean(Environment.class);
        }
        if (env.getProperty(key) == null) {
            throw new Exception("Properties " + key + " is empty");
        }
        return env.getProperty(key);
    }

    public static void sleepMillis(int seconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static String genUser() {
        return "u" + ThreadLocalRandom.current().nextInt(10000, 99999);
    }

    public static String genPassword() {

        /*
        * net user  168e4&Zx /add  Если пароль содержит амперсанд - то при добавлении через консоль считается что это команда и всё падает
        * */
        int length = 8;

        final char[] lowercase = "abcdefghijklmnopqrstuvwxyz".toCharArray();
        final char[] uppercase = "ABCDEFGJKLMNPRSTUVWXYZ".toCharArray();
        final char[] numbers = "0123456789".toCharArray();
        final char[] symbols = "!".toCharArray();
        final char[] allAllowed = "abcdefghijklmnopqrstuvwxyzABCDEFGJKLMNPRSTUVWXYZ0123456789!".toCharArray();

        //Use cryptographically secure random number generator
        Random random = new SecureRandom();

        StringBuilder password = new StringBuilder();

        for (int i = 0; i < length - 4; i++) {
            password.append(allAllowed[random.nextInt(allAllowed.length)]);
        }

        //Ensure password policy is met by inserting required random chars in random positions
        password.insert(random.nextInt(password.length()), lowercase[random.nextInt(lowercase.length)]);
        password.insert(random.nextInt(password.length()), uppercase[random.nextInt(uppercase.length)]);
        password.insert(random.nextInt(password.length()), numbers[random.nextInt(numbers.length)]);
        password.insert(random.nextInt(password.length()), symbols[random.nextInt(symbols.length)]);
        return "p" + password.toString();
    }

    public static String stackTraceToString(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    public static String telegramSend(String data) {
        return telegramSend("-901324385", data);
    }

    public static String telegramSend(String idChat, String data) {
        if (idChat == null) {
            return "{\"status\": \"idChatTelegram is null\"}";
        }
        try {
            String urlString = "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s";

            //String apiToken = "6091094290:AAH8VKY9qyo7ezvnq_3LFw7Alh0zOS-sjqM";
            String apiToken = "6036561084:AAGlIBVSnAOSK8TT-rEDKBJCBX7M40OCv1I";
            if (apiToken != null) {
                urlString = String.format(urlString, apiToken, idChat, URLEncoder.encode(data, StandardCharsets.UTF_8.toString()));
                //System.out.println(urlString);
                URL url = new URL(urlString);
                URLConnection conn = url.openConnection();

                StringBuilder sb = new StringBuilder();
                InputStream is = new BufferedInputStream(conn.getInputStream());
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String inputLine = "";
                while ((inputLine = br.readLine()) != null) {
                    sb.append(inputLine);
                }
                return sb.toString();
            } else {
                return "{\"status\": \"Telegram bot token is null\"}";
            }
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> result = new HashMap<>();
            result.put("status", e.toString());
            return Util.jsonObjectToString(result);
        }
    }

}
