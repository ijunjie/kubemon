package com.kingsoft.kubemon.metrics;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kingsoft.kubemon.ahc.Json;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor(staticName = "of")
@Getter
@ToString
public class NodeDataSimple {
    private final String name;
    private final QuantityAccumulator capacity;
    private final QuantityAccumulator allocatable;
    private final QuantityAccumulator usage;

    public String toJsonStr() {
        ObjectNode obj = Json.newObject();
        obj.put("name", this.getName());
        obj.put("capacity", this.getCapacity().toJsonStr());
        obj.put("allocatable", this.getAllocatable().toJsonStr());
        obj.put("usage", this.getUsage().toJsonStr());
        return Json.stringify(obj);
    }
}
