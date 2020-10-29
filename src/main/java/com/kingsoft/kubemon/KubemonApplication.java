package com.kingsoft.kubemon;

import com.kingsoft.kubemon.metrics.NodeMetricsService;
import com.kingsoft.kubemon.metrics.QuantityAccumulator;
import io.fabric8.kubernetes.api.model.Node;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

@EnableScheduling
@Slf4j
@SpringBootApplication
public class KubemonApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplicationBuilder(KubemonApplication.class).build();
        application.setWebApplicationType(WebApplicationType.NONE);
        application.run(args);
    }

    @Autowired
    NodeMetricsService nodeMetricsService;
    @Value("${schedule-enabled:false}")
    boolean scheduleEnabled;

    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {
            List<Node> nodes = nodeMetricsService.nodes();
            QuantityAccumulator capacity = nodeMetricsService.capacity(nodes);
            QuantityAccumulator allocatable = nodeMetricsService.allocatable(nodes);
            QuantityAccumulator requests = nodeMetricsService.allPodsRequests();
            int cpuPercent = requests.cpuProportion(allocatable);
            int memPercent = requests.memoryProportion(allocatable);
            log.info("Node count: {}", nodes.size());
            log.info("Capacity: {}", capacity);
            log.info("Allocatable: {}", allocatable);
            log.info("CPU Percent: {}", cpuPercent);
            log.info("Memory Percent: {}", memPercent);
        };
    }

    @Scheduled(fixedDelayString = "${scrape-interval-milliseconds:300000}")
    public void schedule() {
        if (scheduleEnabled) {
            List<Node> nodes = nodeMetricsService.nodes();
            QuantityAccumulator usage = nodeMetricsService.usage(nodes);
            log.info("usage total: {}", usage);
        }
    }
}
