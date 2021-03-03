package com.kingsoft.kubemon.function;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface CompletableFutureUtils {

    /**
     * 合并多个 futures 的 stream 为一个 CompletableFuture
     */
    static <T> CompletableFuture<List<T>> sequence(Stream<CompletableFuture<T>> futureStream) {
        List<CompletableFuture<T>> futures = futureStream
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return sequence(futures);
    }

    /**
     * 合并多个 futures 为一个 CompletableFuture
     */
    static <T> CompletableFuture<List<T>> sequence(List<CompletableFuture<T>> futures) {
        return CompletableFuture
                .allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
    }
}