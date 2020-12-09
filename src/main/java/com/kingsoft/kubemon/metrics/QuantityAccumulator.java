package com.kingsoft.kubemon.metrics;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kingsoft.kubemon.ahc.Json;
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
    private final BigDecimal ephemeralStorage;
    private final BigDecimal pods;

    public static final QuantityAccumulator ZERO
            = new QuantityAccumulator(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

    public QuantityAccumulator(Map<String, Quantity> map) {
        Quantity cpuQuantity = map.getOrDefault("cpu", Quantity.parse("0n"));
        Quantity memoryQuantity = map.getOrDefault("memory", Quantity.parse("0Ki"));
        Quantity ephemeralStorageQuantity = map.getOrDefault("ephemeral-storage", Quantity.parse("0Ki"));
        Quantity podsQuantity = map.getOrDefault("pods", Quantity.parse("0"));
        this.cpus = Quantity.getAmountInBytes(cpuQuantity);
        this.memoryBytes = Quantity.getAmountInBytes(memoryQuantity);
        this.ephemeralStorage = Quantity.getAmountInBytes(ephemeralStorageQuantity);
        this.pods = Quantity.getAmountInBytes(podsQuantity);
    }

    public QuantityAccumulator(BigDecimal cpus,
                               BigDecimal memoryBytes,
                               BigDecimal ephemeralStorage,
                               BigDecimal pods) {
        this.cpus = cpus;
        this.memoryBytes = memoryBytes;
        this.ephemeralStorage = ephemeralStorage;
        this.pods = pods;
    }

    public QuantityAccumulator add(QuantityAccumulator another) {
        BigDecimal cpuSum = this.cpus.add(another.getCpus());
        BigDecimal memoryBytesSum = this.memoryBytes.add(another.getMemoryBytes());
        BigDecimal ephemeralStorage = this.ephemeralStorage.add(another.getEphemeralStorage());
        BigDecimal pods = this.pods.add(another.getPods());
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

    public BigDecimal getEphemeralStorage() {
        return ephemeralStorage;
    }

    public BigDecimal getEphemeralStorageGi() {
        BigDecimal multiple = new BigDecimal("2").pow(-30, MathContext.DECIMAL64);
        return this.ephemeralStorage.multiply(multiple).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getPods() {
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


    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof QuantityAccumulator)) return false;
        final QuantityAccumulator other = (QuantityAccumulator) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$cpus = this.getCpus();
        final Object other$cpus = other.getCpus();
        if (this$cpus == null ? other$cpus != null : !this$cpus.equals(other$cpus)) return false;
        final Object this$memoryBytes = this.getMemoryBytes();
        final Object other$memoryBytes = other.getMemoryBytes();
        if (this$memoryBytes == null ? other$memoryBytes != null : !this$memoryBytes.equals(other$memoryBytes))
            return false;
        final Object this$ephemeralStorage = this.getEphemeralStorage();
        final Object other$ephemeralStorage = other.getEphemeralStorage();
        if (this$ephemeralStorage == null ? other$ephemeralStorage != null : !this$ephemeralStorage.equals(other$ephemeralStorage))
            return false;
        final Object this$pods = this.getPods();
        final Object other$pods = other.getPods();
        if (this$pods == null ? other$pods != null : !this$pods.equals(other$pods)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof QuantityAccumulator;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $cpus = this.getCpus();
        result = result * PRIME + ($cpus == null ? 43 : $cpus.hashCode());
        final Object $memoryBytes = this.getMemoryBytes();
        result = result * PRIME + ($memoryBytes == null ? 43 : $memoryBytes.hashCode());
        final Object $ephemeralStorage = this.getEphemeralStorage();
        result = result * PRIME + ($ephemeralStorage == null ? 43 : $ephemeralStorage.hashCode());
        final Object $pods = this.getPods();
        result = result * PRIME + ($pods == null ? 43 : $pods.hashCode());
        return result;
    }

    public String toString() {
        return "QuantityAccumulator(cpus=" + this.getCpus() +
                ", memoryGi=" + this.getMemoryGi() +
                ", ephemeralStorageGi=" + this.getEphemeralStorageGi() +
                ", pods=" + this.getPods() + ")";
    }

    public String toJsonStr() {
        ObjectNode obj = Json.newObject();
        obj.put("cpus", this.getCpus());
        obj.put("memoryGi", this.getMemoryGi());
        if (this.getEphemeralStorageGi().compareTo(BigDecimal.ZERO) > 0) {
            obj.put("ephemeralStorageGi", this.getEphemeralStorage());
        }
        if (this.getPods().compareTo(BigDecimal.ZERO) > 0) {
            obj.put("pods", this.getPods());
        }
        return Json.stringify(obj);
    }
}