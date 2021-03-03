package com.kingsoft.kubemon.statistics;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.stream.Collector;

public interface Statistics3 {
    Tuple<Long> asTuple3();

    static Collector<Statistics3, R, R> collector() {
        return Collector.of(R::new,
                (r, t) -> r.accept(t.asTuple3()),
                (l, r) -> {
                    l.combine(r);
                    return l;
                });
    }

    @EqualsAndHashCode
    @ToString
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    class Tuple<T> {
        public T t1;
        public T t2;
        public T t3;
    }

    class R {
        private long count = 0;
        private Tuple<Long> sum = new Tuple<>(0L, 0L, 0L);

        public R() {
        }

        public void accept(Tuple<Long> value) {
            ++count;
            sum.t1 += value.t1;
            sum.t2 += value.t2;
            sum.t3 += value.t3;
        }

        public void combine(R other) {
            count += other.count;
            sum.t1 += other.sum.t1;
            sum.t2 += other.sum.t2;
            sum.t3 += other.sum.t3;
        }

        public final long getCount() {
            return count;
        }

        public final Tuple<Long> getSum() {
            return sum;
        }

        public final Tuple<Double> getAverage() {
            long count = getCount();
            if (count <= 0) {
                return new Tuple<>(0.0d, 0.0d, 0.0d);
            }
            Tuple<Long> sum = getSum();
            double t1 = (double) sum.t1 / count;
            double t2 = (double) sum.t2 / count;
            double t3 = (double) sum.t3 / count;
            return new Tuple<>(t1, t2, t3);
        }
    }
}
