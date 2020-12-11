package com.kingsoft.kubemon.metrics;

import com.fasterxml.jackson.databind.JsonNode;
import com.kingsoft.kubemon.ahc.Json;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(staticName = "of")
@EqualsAndHashCode
public class NodeDataSimple {
    private final String name;
    private final QuantityAccumulator capacity;
    private final QuantityAccumulator allocatable;
    private final QuantityAccumulator usage;

    public String getName() {
        return this.name;
    }

    public QuantityAccumulator getCapacity() {
        return this.capacity;
    }

    public QuantityAccumulator getAllocatable() {
        return this.allocatable;
    }

    public QuantityAccumulator getUsage() {
        return this.usage;
    }

    public JsonNode asJsonNode() {
        return Json.toJson(this);
    }
}
