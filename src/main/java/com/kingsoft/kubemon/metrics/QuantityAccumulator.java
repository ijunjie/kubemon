package com.kingsoft.kubemon.metrics;

import io.fabric8.kubernetes.api.model.Quantity;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Objects;

/**
 * @author WANGJUNJIE2
 */
public class QuantityAccumulator {
    private final BigDecimal cpus;
    private final BigDecimal memoryBytes;

    public static final QuantityAccumulator ZERO = new QuantityAccumulator(BigDecimal.ZERO, BigDecimal.ZERO);

    public QuantityAccumulator(Map<String, Quantity> usageMap) {
        Quantity cpuQuantity = usageMap.getOrDefault("cpu", Quantity.parse("0n"));
        Quantity memoryQuantity = usageMap.getOrDefault("memory", Quantity.parse("0Ki"));
        this.cpus = Quantity.getAmountInBytes(cpuQuantity);
        this.memoryBytes = Quantity.getAmountInBytes(memoryQuantity);
    }

    public QuantityAccumulator(BigDecimal cpus, BigDecimal memoryBytes) {
        this.cpus = cpus;
        this.memoryBytes = memoryBytes;
    }

    public QuantityAccumulator add(QuantityAccumulator another) {
        BigDecimal cpuSum = this.cpus.add(another.getCpus());
        BigDecimal memoryBytesSum = this.memoryBytes.add(another.getMemoryBytes());
        return new QuantityAccumulator(cpuSum, memoryBytesSum);
    }

    public BigDecimal getCpus() {
        return cpus;
    }

    public BigDecimal getMemoryBytes() {
        return memoryBytes;
    }

    public BigDecimal getMemoryGi() {
        BigDecimal multiple = new BigDecimal("2").pow(-30, MathContext.DECIMAL64);
        return this.memoryBytes.multiply(multiple).setScale(2, RoundingMode.HALF_UP);
    }

    public int cpuProportion(QuantityAccumulator another) {
        Objects.requireNonNull(another);
        return this.cpus
                .divide(another.getCpus(), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .intValue();
    }

    public int memoryProportion(QuantityAccumulator another) {
        Objects.requireNonNull(another);
        return this.memoryBytes
                .divide(another.getMemoryBytes(), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .intValue();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QuantityAccumulator that = (QuantityAccumulator) o;

        if (!cpus.equals(that.cpus)) return false;
        return memoryBytes.equals(that.memoryBytes);
    }

    @Override
    public int hashCode() {
        int result = cpus.hashCode();
        result = 31 * result + memoryBytes.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "QuantityAccumulator{" +
                "cpus=" + cpus +
                ", memoryBytes=" + memoryBytes +
                ", memoryGi=" + getMemoryGi() +
                '}';
    }
}