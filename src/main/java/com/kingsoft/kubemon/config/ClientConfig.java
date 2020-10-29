package com.kingsoft.kubemon.config;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Base64;
import java.util.Objects;

@Configuration
public class ClientConfig {

    @Value("${k8s.api-server}")
    String apiUrl;

    @Value("${k8s.cacrt}")
    String caCert;

    @Value("${k8s.token}")
    String token;

    @Bean
    public DefaultKubernetesClient defaultKubernetesClient() {
        Objects.requireNonNull(apiUrl);
        Objects.requireNonNull(caCert);
        Objects.requireNonNull(token);

        Config config = new ConfigBuilder()
                .withMasterUrl(apiUrl)
                .withTrustCerts(true)
                .withCaCertData(caCert)
                .withOauthToken(new String(Base64.getDecoder().decode(token)))
                .build();
        return new DefaultKubernetesClient(config);
    }

}
