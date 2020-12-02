package com.kingsoft.kubemon.ahc;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author WANGJUNJIE2
 */
@SpringBootTest(classes = {AhcTestBootStrap.class}, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Slf4j
public class AhcClientTest {
    @Autowired
    AhcClient client;

    @Test
    public void testGet() {
        String author = client.getFuture("http://10.69.69.232:8080/json")
                .thenApply(Json.cast(Resp.class))
                .thenApply(Resp::getSlideshow)
                .thenApply(Slideshow::getAuthor)
                .exceptionally(e -> {
                    log.error("", e);
                    return "failover";
                })
                .toCompletableFuture()
                .join();
        log.info("author: {}", author);
    }

    @Test
    public void testPost() {
        ObjectNode data = Json.newObject().put("a", 22).put("b", "hello");
        String r = client.postFuture("http://10.69.69.232:8080/post", data)
                .thenApply(x -> x.get("json").get("b").asText())
                .exceptionally(e -> {
                    log.error("", e);
                    return "failover";
                })
                .toCompletableFuture()
                .join();
        log.info(">>>>>>>>>>>>>: {}", r);
    }

    @Test
    public void testParseSchedule() {
        Map<String, Integer> result = client.getFuture("http://10.69.57.65:3000/mock/913/ws/v1/cluster/scheduler")
                .thenApply(Json.cast(SchedulerModel.SchedulerResp.class))
                .thenApply(SchedulerModel.SchedulerResp::getScheduler)
                .thenApply(SchedulerModel.Scheduler::getSchedulerInfo)
                .thenApply(SchedulerModel.SchedulerInfo::queuesVcoresUsed)
                .exceptionally(e -> {
                    log.error("", e);
                    return Collections.emptyMap();
                })
                .toCompletableFuture()
                .join();
        result.forEach((k, v) -> log.info(">>>>>>> {}:{}", k, v));
    }

    @Test
    public void testCombine() {
        CompletionStage<String> future1 = client.getFuture("http://10.69.69.232:8080/json")
                .thenApply(Json.cast(Resp.class))
                .thenApply(Resp::getSlideshow)
                .thenApply(Slideshow::getAuthor)
                .exceptionally(e -> {
                    log.error("", e);
                    return "failover";
                });
        CompletionStage<Integer> future2 = client.getFuture("http://10.69.57.65:3000/mock/913/ws/v1/cluster/scheduler")
                .thenApply(Json.cast(SchedulerModel.SchedulerResp.class))
                .thenApply(SchedulerModel.SchedulerResp::getScheduler)
                .thenApply(SchedulerModel.Scheduler::getSchedulerInfo)
                .thenApply(SchedulerModel.SchedulerInfo::queuesVcoresUsed)
                .exceptionally(e -> {
                    log.error("", e);
                    return Collections.emptyMap();
                })
                .thenApply(Map::size);
        Foo join = future1.thenCombine(future2, Foo::of).toCompletableFuture().join();
        log.info(">>>>>>>>>>>>>>>>> {}", join);
    }

    @ToString
    @RequiredArgsConstructor(staticName = "of")
    static class Foo {
        final String s;
        final int i;
    }

    @Getter
    static class Resp {
        Slideshow slideshow;
    }

    @Getter
    static class Slideshow {
        String author;
    }

    static class SchedulerModel {

        @Getter
        public static class SchedulerResp {
            Scheduler scheduler;
        }

        @Getter
        public static class Scheduler {
            SchedulerInfo schedulerInfo;
        }

        @Getter
        public static class SchedulerInfo {
            Queues queues;

            public Stream<Queue> stream() {
                return Optional.ofNullable(this.queues)
                        .map(Queues::getQueue)
                        .map(List::stream)
                        .orElse(Stream.empty());
            }

            public Map<String, Integer> queuesVcoresUsed() {
                return this.stream()
                        .filter(x -> !"default".equals(x.getQueueName()))
                        .flatMap(x -> x.stream()
                                .flatMap(Queue::stream))
                        .collect(
                                Collectors.toMap(
                                        Queue::getQueueName,
                                        Queue::getVCores
                                )
                        );
            }
        }

        @Getter
        public static class Queue {
            String queueName;
            ResourcesUsed resourcesUsed;
            Queues queues;

            public Stream<Queue> stream() {
                return Optional.ofNullable(this.queues)
                        .map(Queues::getQueue)
                        .map(List::stream)
                        .orElse(Stream.empty());
            }

            public int getVCores() {
                return Optional.ofNullable(this.resourcesUsed)
                        .map(ResourcesUsed::getVCores)
                        .orElse(0);
            }
        }

        @Getter
        public static class ResourcesUsed {
            @JsonProperty("vCores")
            int vCores;
        }

        @Getter
        public static class Queues {
            List<Queue> queue;
        }
    }
}
