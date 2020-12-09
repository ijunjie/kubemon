package com.kingsoft.kubemon.metrics;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kingsoft.kubemon.ahc.Json;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.stream.Collectors;

@ToString
@Getter
public class PodData {
    private final String name; // Pod::metadata.name

    private final String namespace; // Pod::metadata.namespace
    private final String nodeName; // Pod::spec.nodeName

    private final QuantityAccumulator resourceRequirementRequests; // Pod::spec.containers[].resources.requests
    private final QuantityAccumulator resourceRequirementLimits; // Pod::spec.containers[].resources.limits
    private final QuantityAccumulator usage; // PodMetrics::containers[].usage

    public PodData(Pod pod, QuantityAccumulator usage) {
        this.name = pod.getMetadata().getName();
        this.namespace = pod.getMetadata().getNamespace();
        this.nodeName = pod.getSpec().getNodeName();
        List<Container> containers = pod.getSpec().getContainers();
        List<ResourceRequirements> resourceRequirements = containers.stream()
                .map(Container::getResources)
                .collect(Collectors.toList());
        QuantityAccumulator requests = resourceRequirements.stream()
                .map(ResourceRequirements::getRequests)
                .map(QuantityAccumulator::new)
                .reduce(QuantityAccumulator.ZERO, QuantityAccumulator::add);
        QuantityAccumulator limits = resourceRequirements.stream()
                .map(ResourceRequirements::getLimits)
                .map(QuantityAccumulator::new)
                .reduce(QuantityAccumulator.ZERO, QuantityAccumulator::add);
        this.resourceRequirementRequests = requests;
        this.resourceRequirementLimits = limits;
        this.usage = usage;
    }

    public String toJsonStr() {
        ObjectNode obj = Json.newObject();
        obj.put("name", this.getName());
        obj.put("namespace", this.getNamespace());
        obj.put("nodeName", this.getNodeName());
        obj.put("requests", this.getResourceRequirementRequests().toJsonStr());
        obj.put("limits", this.getResourceRequirementLimits().toJsonStr());
        obj.put("usage", this.getUsage().toJsonStr());
        return Json.stringify(obj);
    }
}
