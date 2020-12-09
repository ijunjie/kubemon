package com.kingsoft.kubemon.metrics;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ListOptions;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeList;
import io.fabric8.kubernetes.api.model.NodeStatus;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.ContainerMetrics;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.NodeMetrics;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.NodeMetricsList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.dsl.internal.NodeMetricOperationsImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KubernetesMetricsService {
    private static List<String> nonTerminatedPhases = Arrays.asList("pending", "running");

    @Autowired
    DefaultKubernetesClient client;

    ForkJoinPool forkJoinPool = new ForkJoinPool(16);


    public String masterUrl() {
        URL masterUrl = client.getMasterUrl();
        return masterUrl.toString();
    }

    public QuantityAccumulator allPodsRequests() {
        List<Pod> pods = client.pods().inAnyNamespace().list().getItems();
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
            log.info("\n\tnode={}, \n\tcapacity={},\n\tallocatable={}, \n\tusage={}",
                    nodeName, capacity, allocatable, usage);
        } catch (Exception e) {
            log.error("node metrics error, skipped.", e);
        }
    }

    // ---------------------------
    public CompletableFuture<List<NodeDataFull>> nodeDataFullList() {
        CompletableFuture<List<NodeDataSimple>> simpleListFuture = nodeDataSimpleList();
        CompletionStage<Map<String, List<PodData>>> dictFuture = podDataGroupingByNode();
        return simpleListFuture.thenCombine(
                dictFuture,
                (simpleList, dict) -> simpleList.stream()
                        .map(x -> NodeDataFull.of(x, dict.getOrDefault(x.getName(), Collections.emptyList())))
                        .collect(Collectors.toList()));
    }

    public CompletableFuture<List<NodeDataSimple>> nodeDataSimpleList() {
        CompletableFuture<List<Node>> nodeListItemsFuture = nodeListItems();
        CompletableFuture<Map<String, QuantityAccumulator>> nodeMetricsMappingFuture = nodeMetricsMapping();
        return nodeListItemsFuture.thenCombine(
                nodeMetricsMappingFuture,
                (nodeListItems, nodeMetricsMapping) -> nodeListItems.stream()
                        .map(x -> NodeDataSimple.of(
                                x.getMetadata().getName(),
                                new QuantityAccumulator(x.getStatus().getCapacity()),
                                new QuantityAccumulator(x.getStatus().getAllocatable()),
                                nodeMetricsMapping.getOrDefault(x.getMetadata().getName(), QuantityAccumulator.ZERO)))
                        .collect(Collectors.toList()));
    }

    public CompletableFuture<List<Node>> nodeListItems() {
        ListOptions listOptions = new ListOptionsBuilder()
                .withLabelSelector("kubernetes.io/role=node")
                .build();
        return CompletableFuture
                .supplyAsync(() -> client.nodes().list(listOptions))
                .thenApply(NodeList::getItems)
                .exceptionally(e -> {
                    log.error("", e);
                    return Collections.emptyList();
                });
    }

    public CompletableFuture<Map<String, QuantityAccumulator>> nodeMetricsMapping() {
        return CompletableFuture
                .supplyAsync(() -> client.top().nodes().metrics())
                .thenApply(NodeMetricsList::getItems)
                .thenApply(nodeMetricsListItems -> nodeMetricsListItems.stream()
                        .collect(Collectors.toMap(
                                x -> x.getMetadata().getName(),
                                x -> new QuantityAccumulator(x.getUsage()))))
                .exceptionally(e -> {
                    log.error("", e);
                    return Collections.emptyMap();
                });
    }

    /*----------------------- Pod -------------------------*/
    public CompletionStage<Map<String, List<PodData>>> podDataGroupingByNode() {
        return podDataList()
                .thenApply(podDataList -> podDataList.stream()
                        .collect(Collectors.groupingBy(PodData::getName)));
    }


    public CompletionStage<List<PodData>> podDataList() {
        CompletionStage<List<Pod>> podListFuture = podListItems();
        CompletionStage<Map<String, QuantityAccumulator>> podMetricsMappingFuture = podMetricsMapping();
        return podListFuture.thenCombine(
                podMetricsMappingFuture,
                (podListItems, podMetricsMapping) -> podListItems.stream()
                        .map(x -> new PodData(x,
                                podMetricsMapping.getOrDefault(x.getMetadata().getName(), QuantityAccumulator.ZERO)))
                        .collect(Collectors.toList()));
    }

    /**
     * 过滤掉 running 之外的 pod
     *
     * @return future pod list items
     */
    public CompletionStage<List<Pod>> podListItems() {
        // Pending Running Succeeded Failed Unknown
        ListOptions listOptions = new ListOptionsBuilder()
                .withFieldSelector("status.phase=Running")
                .build();
        return CompletableFuture
                .supplyAsync(() -> client.pods().inAnyNamespace().list(listOptions))
                .thenApply(PodList::getItems)
                .exceptionally(e -> {
                    log.error("", e);
                    return Collections.emptyList();
                });
    }

    /**
     * 不过滤，作为字典
     *
     * @return future mapping
     */
    public CompletionStage<Map<String, QuantityAccumulator>> podMetricsMapping() {
        return CompletableFuture
                .supplyAsync(() -> client.top().pods().metrics())
                .thenApply(podMetricsList -> podMetricsList.getItems().stream()
                        .collect(Collectors.toMap(x -> x.getMetadata().getName(),
                                x -> x.getContainers().stream()
                                        .map(ContainerMetrics::getUsage)
                                        .map(QuantityAccumulator::new)
                                        .reduce(QuantityAccumulator.ZERO, QuantityAccumulator::add))))
                .exceptionally(e -> {
                    log.error("", e);
                    return Collections.emptyMap();
                });
    }
}
