package com.kingsoft.kubemon.cache;

import com.google.common.cache.RemovalListener;
import com.kingsoft.kubemon.function.F;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class DemoCachePool extends CachePool<String, Integer> {

    public DemoCachePool() {
        super();
    }

    @Override
    public int initialCapacity() {
        return 10;
    }

    @Override
    public RemovalListener<String, Integer> removalListener() {
        return notification -> {
            log.info(">>>>>>>>>>>>>>>>removal");
        };
    }

    @Override
    public F.Tuple<Long, TimeUnit> expireAfterWrite() {
        return new F.Tuple<>(5L, TimeUnit.SECONDS);
    }

    @Override
    protected Integer loadValue(String s) {
        log.info(">>>>>>>>>>>>>>>>>>foo cache pool load");
        if (s == null) {
            return 0;
        }
        return s.length();
    }
}
