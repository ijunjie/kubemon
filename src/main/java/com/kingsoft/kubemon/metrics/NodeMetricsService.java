package com.kingsoft.kubemon.metrics;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ListOptions;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeStatus;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.NodeMetrics;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.dsl.internal.NodeMetricOperationsImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;

@Slf4j
@Service
public class NodeMetricsService {

    @Autowired
    DefaultKubernetesClient client;

    ForkJoinPool forkJoinPool = new ForkJoinPool(16);

    public QuantityAccumulator allPodsRequests() {
        List<Pod> pods = client.pods().inAnyNamespace().list().getItems();
        List<String> nonTerminatedPhases = Arrays.asList("pending", "running");
        return pods.stream()
                .filter(x -> x.getStatus() != null
                        && x.getStatus().getPhase() != null
                        && nonTerminatedPhases.contains(x.getStatus().getPhase().toLowerCase()))
                .map(Pod::getSpec)
                .filter(Objects::nonNull)
                .map(PodSpec::getContainers)
                .flatMap(Collection::stream)
                .map(Container::getResources)
                .filter(Objects::nonNull)
                .map(ResourceRequirements::getRequests)
                .filter(Objects::nonNull)
                .map(QuantityAccumulator::new)
                .reduce(QuantityAccumulator.ZERO, QuantityAccumulator::add);
    }

    public List<Node> nodes() {
        ListOptions listOptions = new ListOptionsBuilder()
                .withLabelSelector("kubernetes.io/role=node")
                .build();
        return client.nodes().list(listOptions).getItems();
    }

    public QuantityAccumulator capacity(List<Node> nodeList) {
        return accumulator(nodeList, NodeStatus::getCapacity);
    }

    public QuantityAccumulator allocatable(List<Node> nodeList) {
        return accumulator(nodeList, NodeStatus::getAllocatable);
    }

    private QuantityAccumulator accumulator(List<Node> nodeList, Function<NodeStatus, Map<String, Quantity>> mapper) {
        return forkJoinPool
                .submit(() -> nodeList.parallelStream()
                        .map(Node::getStatus)
                        .map(mapper)
                        .filter(Objects::nonNull)
                        .map(QuantityAccumulator::new)
                        .reduce(QuantityAccumulator.ZERO, QuantityAccumulator::add))
                .join();
    }

    public QuantityAccumulator usage(List<Node> nodeList) {
        NodeMetricOperationsImpl nodeMetricOperations = client.top().nodes();
        Function<String, Optional<NodeMetrics>> nodeNameToMetrics = x -> {
            try {
                NodeMetrics metrics = nodeMetricOperations.metrics(x);
                //log.info("node: {}, usages: {}", x, metrics);
                return Optional.ofNullable(metrics);
            } catch (Exception e) {
                log.error("node " + x + " is skipped.", e);
                return Optional.empty();
            }
        };
        return forkJoinPool
                .submit(() -> nodeList.stream()
                        .map(Node::getMetadata)
                        .map(ObjectMeta::getName)
                        .map(nodeNameToMetrics)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .map(NodeMetrics::getUsage)
                        .filter(Objects::nonNull)
                        .map(QuantityAccumulator::new)
                        .reduce(QuantityAccumulator.ZERO, QuantityAccumulator::add))
                .join();
    }

    /*----------------------------*/
    public void inspectNode(Node node) {
        String nodeName = node.getMetadata().getName();

        NodeStatus nodeStatus = node.getStatus();

        Map<String, Quantity> capacityMap = nodeStatus.getCapacity();
        QuantityAccumulator capacity = new QuantityAccumulator(capacityMap);

        Map<String, Quantity> allocatableMap = nodeStatus.getAllocatable();
        QuantityAccumulator allocatable = new QuantityAccumulator(allocatableMap);
        // capacity=QuantityAccumulator(cpus=16, memoryGi=62.76, ephemeralStorageGi=295.17, pods=110)
        // allocatable=QuantityAccumulator(cpus=15.890, memoryGi=57.24, ephemeralStorageGi=264.65, pods=110)

        try {
            NodeMetrics metrics = client.top().nodes().metrics(nodeName);
            Map<String, Quantity> usageQuantity = metrics.getUsage();
            QuantityAccumulator usage = new QuantityAccumulator(usageQuantity);
            log.info("node={}, \n\tcapacity={},\n\t allocatable={}, \n\t usage={}",
                    nodeName, capacity, allocatable, usage);
        } catch (Exception e) {
            log.error("node metrics error, skipped.", e);
        }
    }
}
