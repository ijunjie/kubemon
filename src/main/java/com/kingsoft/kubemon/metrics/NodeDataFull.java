package com.kingsoft.kubemon.metrics;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kingsoft.kubemon.ahc.Json;
import lombok.Getter;

import java.util.List;

@Getter
public class NodeDataFull {
    private final NodeDataSimple nodeDataSimple;

    private final List<PodData> podDataList;

    private final int podSize;

    private final QuantityAccumulator resourceRequests;

    private final int cpuRequestPercent;
    private final int memoryRequestPercent;

    private NodeDataFull(NodeDataSimple nodeDataSimple, List<PodData> podDataList) {
        this.nodeDataSimple = nodeDataSimple;
        this.podDataList = podDataList;

        this.podSize = podDataList.size();
        this.resourceRequests = this.podDataList.stream()
                .map(PodData::getResourceRequirementRequests)
                .reduce(QuantityAccumulator.ZERO, QuantityAccumulator::add);
        this.cpuRequestPercent = this.resourceRequests.cpuProportion(this.nodeDataSimple.getAllocatable());
        this.memoryRequestPercent = this.resourceRequests.memoryProportion(this.nodeDataSimple.getAllocatable());
    }

    public static NodeDataFull of(NodeDataSimple nodeDataSimple, List<PodData> podDataList) {
        return new NodeDataFull(nodeDataSimple, podDataList);
    }


    public String toJsonStr() {
        ObjectNode obj = Json.newObject();
        obj.put("name", this.nodeDataSimple.getName());
        obj.put("capacity", this.nodeDataSimple.getCapacity().toJsonStr());
        obj.put("allocatable", this.nodeDataSimple.getAllocatable().toJsonStr());
        obj.put("usage", this.nodeDataSimple.getUsage().toJsonStr());
        // TODO
        //obj.set("podDataList", Json.toJson(this.podDataList));
        obj.put("podSize", this.podDataList.size());
        obj.put("resourceRequests", this.getResourceRequests().toJsonStr());
        obj.put("cpuRequestPercent", this.getCpuRequestPercent());
        obj.put("memoryRequestPercent", this.getMemoryRequestPercent());
        return Json.stringify(obj);
    }


}
