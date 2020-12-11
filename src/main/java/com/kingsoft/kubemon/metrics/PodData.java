package com.kingsoft.kubemon.metrics;

import com.fasterxml.jackson.databind.JsonNode;
import com.kingsoft.kubemon.ahc.Json;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@EqualsAndHashCode
@Getter
public class PodData {
    private final String name; // Pod::metadata.name

    private final String namespace; // Pod::metadata.namespace
    private final String nodeName; // Pod::spec.nodeName

    private final QuantityAccumulator requests; // Pod::spec.containers[].resources.requests
    private final QuantityAccumulator limits; // Pod::spec.containers[].resources.limits
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
        this.requests = requests;
        this.limits = limits;
        this.usage = usage;
    }

    public JsonNode asJsonNode() {
        return Json.toJson(this);
    }
}
