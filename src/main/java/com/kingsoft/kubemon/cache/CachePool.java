package com.kingsoft.kubemon.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.kingsoft.kubemon.function.F;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class CachePool<K, V> {

    protected LoadingCache<K, V> loadingCache;

    public CachePool() {
        this.loadingCache = CacheBuilder.newBuilder()
                .initialCapacity(initialCapacity())
                .removalListener(removalListener())
                .expireAfterWrite(expireAfterWrite()._1, expireAfterWrite()._2)
                .build(new CacheLoader<K, V>() {
                    @Override
                    public V load(K key) throws Exception {
                        return loadValue(key);
                    }
                });
    }

    public abstract int initialCapacity();

    public abstract RemovalListener<K, V> removalListener();

    public abstract F.Tuple<Long, TimeUnit> expireAfterWrite();

    protected abstract V loadValue(K k);

    public V get(K k) {
        try {
            return loadingCache.get(k);
        } catch (ExecutionException e) {
            log.error("", e);
            return loadValue(k);
        }
    }
}
