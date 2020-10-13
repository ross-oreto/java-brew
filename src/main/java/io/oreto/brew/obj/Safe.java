package io.oreto.brew.obj;

import java.util.Objects;
import java.util.function.Function;

public class Safe<T> {
    public static <T> Safe<T> of(T value) {
        return new Safe<>(value);
    }
    private final T value;

    private Safe(T value) {
        this.value = value;
    }

    public <U> Safe<U> q(Function<T, U> function) {
        U next = val(function);
        return of(next);
    }

    public <U> U val(Function<T, U> function) {
        Objects.requireNonNull(function);
        return (value == null) ? null : function.apply(value);
    }

    public T val() {
        return value;
    }

    public T orElse(T defaultVal) {
        return (value == null) ? defaultVal : value;
    }
}
