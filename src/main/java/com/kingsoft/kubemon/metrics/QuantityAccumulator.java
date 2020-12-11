package com.kingsoft.kubemon.metrics;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.api.model.Quantity;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author WANGJUNJIE2
 */
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public class QuantityAccumulator {
    private final BigDecimal cpus;
    private final BigDecimal memoryBytes;

    private final Optional<BigDecimal> ephemeralStorage;
    private final Optional<BigDecimal> pods;

    public static final QuantityAccumulator ZERO
            = new QuantityAccumulator(BigDecimal.ZERO, BigDecimal.ZERO, Optional.empty(), Optional.empty());

    public QuantityAccumulator(Map<String, Quantity> map) {
        Quantity cpuQuantity = map.getOrDefault("cpu", Quantity.parse("0n"));
        Quantity memoryQuantity = map.getOrDefault("memory", Quantity.parse("0Ki"));
        this.cpus = Quantity.getAmountInBytes(cpuQuantity);
        this.memoryBytes = Quantity.getAmountInBytes(memoryQuantity);

        Quantity rawEphemeralStorage = map.get("ephemeral-storage");
        this.ephemeralStorage = Optional.ofNullable(rawEphemeralStorage).map(Quantity::getAmountInBytes);
        Quantity rawPodsQuantity = map.get("pods");
        this.pods = Optional.ofNullable(rawPodsQuantity).map(Quantity::getAmountInBytes);
    }

    public QuantityAccumulator(BigDecimal cpus,
                               BigDecimal memoryBytes,
                               Optional<BigDecimal> ephemeralStorage,
                               Optional<BigDecimal> pods) {
        this.cpus = cpus;
        this.memoryBytes = memoryBytes;
        this.ephemeralStorage = ephemeralStorage;
        this.pods = pods;
    }

    public QuantityAccumulator add(QuantityAccumulator another) {
        BigDecimal cpuSum = this.cpus.add(another.getCpus());
        BigDecimal memoryBytesSum = this.memoryBytes.add(another.getMemoryBytes());

        Optional<BigDecimal> ephemeralStorage = this.ephemeralStorage
                .flatMap(v1 -> another.getEphemeralStorage()
                        .map(v1::add));
        Optional<BigDecimal> pods = this.pods
                .flatMap(v1 -> another.getPods()
                        .map(v1::add));
        return new QuantityAccumulator(cpuSum, memoryBytesSum, ephemeralStorage, pods);
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

    public Optional<BigDecimal> getEphemeralStorage() {
        return ephemeralStorage;
    }

    public Optional<BigDecimal> getEphemeralStorageGi() {
        BigDecimal multiple = new BigDecimal("2").pow(-30, MathContext.DECIMAL64);
        return this.ephemeralStorage.map(v -> v.multiply(multiple).setScale(2, RoundingMode.HALF_UP));
    }

    public Optional<BigDecimal> getPods() {
        return pods;
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
}