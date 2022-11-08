package ru.jamsys.sbl.web;

import org.springframework.http.HttpStatus;

public class JsonResponse {

    public HttpStatus status;
    public String data;

    public JsonResponse(HttpStatus status, String data) {
        set(status, data);
    }

    public void set(HttpStatus status, String data) {
        this.status = status;
        this.data = data;
    }
    
}