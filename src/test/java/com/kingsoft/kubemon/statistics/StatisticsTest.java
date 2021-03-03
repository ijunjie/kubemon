package com.kingsoft.kubemon.statistics;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

@Slf4j
public class StatisticsTest {
    @Test
    public void testStatistics4() {
        Foo foo1 = Foo.of(4, 3, 7, 6);
        Foo foo2 = Foo.of(5, 1, 7, 3);
        Foo foo3 = Foo.of(7, 9, 1, 9);
        Foo foo4 = Foo.of(6, 4, 1, 5);
        Foo foo5 = Foo.of(8, 2, 2, 5);
        Statistics4.R collect = Stream.of(foo1, foo2, foo3, foo4, foo5)
                .collect(Statistics4.collector());
        log.info("count={}", collect.getCount());
        log.info("sum={}", collect.getSum());
        log.info("avg={}", collect.getAverage());
    }


    @RequiredArgsConstructor(staticName = "of")
    @Getter
    public static class Foo implements Statistics4 {
        final long a;
        final long b;
        final long c;
        final long d;

        @Override
        public Statistics4.Tuple<Long> asTuple4() {
            return new Statistics4.Tuple<>(a, b, c, d);
        }
    }
}
