package ru.jamsys.sbl.web;

import org.springframework.http.HttpStatus;
import ru.jamsys.sbl.Util;

import javax.validation.constraints.NotNull;
import java.util.Optional;

public class JsonResponse {

    public HttpStatus status;
    public String data;

    public JsonResponse() {
        set(HttpStatus.OK, "");
    }
    public JsonResponse(HttpStatus status, String data) {
        set(status, data);
    }

    public void set(HttpStatus status, String data) {
        this.status = status;
        this.data = data;
    }

    @NotNull
    @Override
    public String toString() {
        return Optional.ofNullable(Util.jsonObjectToStringPretty(this)).orElse("{}");
    }
}