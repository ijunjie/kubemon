package com.kingsoft.kubemon.metrics;

import io.fabric8.kubernetes.api.model.ListOptions;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.ContainerMetrics;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.NodeMetricsList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KubernetesMetricsService {

    @Autowired
    DefaultKubernetesClient client;

    /* ------------- node -------------- */
    public CompletableFuture<List<NodeDataFull>> nodeDataFullList() {
        ListOptions listOptions = new ListOptionsBuilder()
                .withLabelSelector("kubernetes.io/role=node")
                .build();
        CompletableFuture<List<NodeDataSimple>> simpleListFuture = nodeDataSimpleList(listOptions);
        CompletionStage<Map<String, List<PodData>>> dictFuture = podDataGroupingByNode();
        return simpleListFuture.thenCombine(
                dictFuture,
                (simpleList, dict) -> simpleList.stream()
                        .map(x -> NodeDataFull.of(x, dict.getOrDefault(x.getName(), Collections.emptyList())))
                        .collect(Collectors.toList()));
    }

    public CompletableFuture<List<NodeDataSimple>> nodeDataSimpleList(ListOptions listOptions) {
        CompletableFuture<List<Node>> nodeListItemsFuture = nodeList(listOptions);
        CompletableFuture<Map<String, QuantityAccumulator>> nodeMetricsMappingFuture = nodeUsageMappingByName();
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

    public CompletableFuture<List<Node>> nodeList(ListOptions listOptions) {
        return CompletableFuture
                .supplyAsync(() -> client.nodes().list(listOptions))
                .thenApply(NodeList::getItems)
                .exceptionally(e -> {
                    log.error("", e);
                    return Collections.emptyList();
                });
    }

    public CompletableFuture<Map<String, QuantityAccumulator>> nodeUsageMappingByName() {
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

    /*----------------------- pod -------------------------*/
    public CompletionStage<Map<String, List<PodData>>> podDataGroupingByNode() {
        // Pending Running Succeeded Failed Unknown
        ListOptions listOptions = new ListOptionsBuilder()
                .withFieldSelector("status.phase=Running")
                .build();
        return podDataList(listOptions)
                .thenApply(podDataList -> podDataList.stream()
                        .collect(Collectors.groupingBy(PodData::getName)));
    }


    public CompletionStage<List<PodData>> podDataList(ListOptions listOptions) {
        CompletionStage<List<Pod>> podListFuture = podList(listOptions);
        CompletionStage<Map<String, QuantityAccumulator>> podUsageMappingFuture = podUsageMappingByName();
        return podListFuture.thenCombine(
                podUsageMappingFuture,
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
    public CompletionStage<List<Pod>> podList(ListOptions listOptions) {
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
    public CompletionStage<Map<String, QuantityAccumulator>> podUsageMappingByName() {
        return CompletableFuture
                .supplyAsync(() -> client.top().pods().metrics())
                .thenApply(podMetricsList -> podMetricsList.getItems().stream()
                        .collect(Collectors.toMap(
                                x -> x.getMetadata().getName(),
                                x -> x.getContainers().stream()
                                        .map(ContainerMetrics::getUsage)
                                        .map(QuantityAccumulator::new)
                                        .reduce(QuantityAccumulator.ZERO, QuantityAccumulator::add))))
                .exceptionally(e -> {
                    log.error("", e);
                    return Collections.emptyMap();
                });
    }

    // --------------------playground--------------------
    public void addLabelsToExistingNamespace() {
        Map<String, String> map = new HashMap<>();
        map.put("aaa", "111");
        map.put("bbb", "222");
        map.put("ccc", "333");
        map.put("ddd", "444");
        Namespace aaa = client.namespaces().withName("aaa")
                .edit()
                .editMetadata()
                .addToLabels(map)
                .and()
                .done();
        log.info("aaa={}", aaa);
    }
}
