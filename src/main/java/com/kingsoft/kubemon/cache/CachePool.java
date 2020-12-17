package com.kingsoft.kubemon.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.kingsoft.kubemon.function.F;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class CachePool<K, V> {

    protected LoadingCache<K, V> loadingCache;

    public CachePool() {
        this.loadingCache = Caffeine.newBuilder()
                .initialCapacity(initialCapacity())
                .removalListener(removalListener())
                .expireAfterWrite(expireAfterWrite()._1, expireAfterWrite()._2)
                .build(this::loadValue);
    }

    public abstract int initialCapacity();

    public abstract RemovalListener<K, V> removalListener();

    public abstract F.Tuple<Long, TimeUnit> expireAfterWrite();

    protected abstract V loadValue(K k);

    public V get(K k) {
        try {
            return loadingCache.get(k);
        } catch (Exception e) {
            log.error("", e);
            return loadValue(k);
        }
    }
}
