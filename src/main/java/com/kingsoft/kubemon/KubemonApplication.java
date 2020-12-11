package com.kingsoft.kubemon;

import com.kingsoft.kubemon.metrics.KubernetesMetricsService;
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
    @Value("${schedule-task-enabled:false}")
    boolean scheduleTaskEnabled;

    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {
        };
    }

    @Scheduled(initialDelay = 30000, fixedDelayString = "${schedule-interval-milliseconds:60000}")
    public void schedule() {
        if (scheduleTaskEnabled) {
            // do something
        }
    }
}
