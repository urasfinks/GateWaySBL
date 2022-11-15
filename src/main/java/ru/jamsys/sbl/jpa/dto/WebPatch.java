package ru.jamsys.sbl.jpa.dto;

public interface WebPatch<T> {
    void patch(T foreign);
}
