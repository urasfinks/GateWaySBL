package ru.jamsys.sbl.web;

import org.springframework.http.HttpStatus;
import ru.jamsys.sbl.Util;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class JsonResponse {

    public HttpStatus status;
    public String description;
    public Map<String, Object> data = new HashMap<>();

    public JsonResponse() {
        set(HttpStatus.OK, "");
    }

    public void set(HttpStatus status, String description) {
        this.status = status;
        this.description = description;
    }

    public void addData(String key, Object value){
        data.put(key, value);
    }

    @NotNull
    @Override
    public String toString() {
        return Optional.ofNullable(Util.jsonObjectToStringPretty(this)).orElse("{}");
    }
}