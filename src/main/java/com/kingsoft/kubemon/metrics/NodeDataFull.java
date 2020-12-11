package com.kingsoft.kubemon.metrics;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.kingsoft.kubemon.ahc.Json;

import java.util.List;

public class NodeDataFull {
    @JsonIgnore
    private final NodeDataSimple nodeDataSimple;

    private final List<PodData> pods;

    private final QuantityAccumulator resourceRequests;
    private final QuantityAccumulator resourceLimits;

    private final int cpuRequestPercent;
    private final int memoryRequestPercent;

    private final int cpuLimitPercent;
    private final int memoryLimitPercent;

    private NodeDataFull(NodeDataSimple nodeDataSimple, List<PodData> pods) {
        this.nodeDataSimple = nodeDataSimple;
        final QuantityAccumulator allocatable = this.nodeDataSimple.getAllocatable();
        this.pods = pods;

        this.resourceRequests = this.pods.stream()
                .map(PodData::getRequests)
                .reduce(QuantityAccumulator.ZERO, QuantityAccumulator::add);
        this.cpuRequestPercent = this.resourceRequests.cpuProportion(allocatable);
        this.memoryRequestPercent = this.resourceRequests.memoryProportion(allocatable);

        this.resourceLimits = this.pods.stream()
                .map(PodData::getLimits)
                .reduce(QuantityAccumulator.ZERO, QuantityAccumulator::add);
        this.cpuLimitPercent = this.resourceLimits.cpuProportion(allocatable);
        this.memoryLimitPercent = this.resourceLimits.memoryProportion(allocatable);

    }

    public static NodeDataFull of(NodeDataSimple nodeDataSimple, List<PodData> podDataList) {
        return new NodeDataFull(nodeDataSimple, podDataList);
    }

    /*@JsonIgnore
    public NodeDataSimple getNodeDataSimple() {
        return this.nodeDataSimple;
    }*/

    // delegate by nodeDataSimple
    public String getName() {
        return this.nodeDataSimple.getName();
    }

    // delegate by nodeDataSimple
    public QuantityAccumulator getCapacity() {
        return this.nodeDataSimple.getCapacity();
    }

    // delegate by nodeDataSimple
    public QuantityAccumulator getAllocatable() {
        return this.nodeDataSimple.getAllocatable();
    }

    // delegate by nodeDataSimple
    public QuantityAccumulator getUsage() {
        return this.nodeDataSimple.getUsage();
    }

    public List<PodData> getPods() {
        return this.pods;
    }

    public QuantityAccumulator getResourceRequests() {
        return this.resourceRequests;
    }

    public QuantityAccumulator getResourceLimits() {
        return resourceLimits;
    }

    public int getCpuRequestPercent() {
        return this.cpuRequestPercent;
    }

    public int getMemoryRequestPercent() {
        return this.memoryRequestPercent;
    }

    public int getCpuLimitPercent() {
        return cpuLimitPercent;
    }

    public int getMemoryLimitPercent() {
        return memoryLimitPercent;
    }

    public JsonNode asJsonNode() {
        return Json.toJson(this);
    }
}
