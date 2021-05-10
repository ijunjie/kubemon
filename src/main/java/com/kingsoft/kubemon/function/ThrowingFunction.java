package com.kingsoft.kubemon.function;

public interface ThrowingFunction<T, R, E extends Throwable> {

    static <T, R, E extends Throwable> ThrowingFunction<T, R, E> unchecked(ThrowingFunction<T, R, E> func) {
        return t -> {
            try {
                return func.apply(t);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        };
    }

    R apply(T t) throws E;
}
