package ru.jamsys.sbl;

import lombok.Data;

@Data
public class WrapJsonToObject<T> {
    T object;
    Exception exception;

    public WrapJsonToObject(T object, Exception exception) {
        this.object = object;
        this.exception = exception;
    }
}
