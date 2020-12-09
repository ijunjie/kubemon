package com.kingsoft.kubemon;

import com.kingsoft.kubemon.metrics.KubernetesMetricsService;
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
    KubernetesMetricsService nodeMetricsService;
    @Value("${node-inspect-enabled:false}")
    boolean nodeInspectEnabled;

    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {
            /*String masterUrl = nodeMetricsService.masterUrl();
            List<Node> nodes = nodeMetricsService.nodes();
            QuantityAccumulator capacity = nodeMetricsService.capacity(nodes);
            QuantityAccumulator allocatable = nodeMetricsService.allocatable(nodes);
            QuantityAccumulator requests = nodeMetricsService.allPodsRequests();
            int cpuPercent = requests.cpuProportion(allocatable);
            int memPercent = requests.memoryProportion(allocatable);
            log.info("\n\tcluster={}, \n\tcluster-node-count={}, \n\tcluster-capacity={}, \n\tcluster-allocatable={}, " +
                            "\n\tcluster-requests={}, \n\tcluster-cpu-request-percent={}, \n\tcluster-mem-request-percent={}",
                    masterUrl, nodes.size(), capacity, allocatable, requests, cpuPercent, memPercent);*/
            //nodeMetricsService.podInspect();
            //nodeMetricsService.podMetrics();
        };
    }

    @Scheduled(initialDelay = 30000, fixedDelayString = "${scrape-interval-milliseconds:300000}")
    public void schedule() {
        if (nodeInspectEnabled) {
            List<Node> nodes = nodeMetricsService.nodes();
            for (Node node : nodes) {
                nodeMetricsService.inspectNode(node);
            }
        }
    }
}
