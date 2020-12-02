package com.kingsoft.kubemon.ahc;

import akka.actor.ActorSystem;
import akka.stream.Materializer;
import akka.stream.SystemMaterializer;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import play.libs.ws.DefaultBodyReadables;
import play.libs.ws.DefaultBodyWritables;
import play.libs.ws.StandaloneWSResponse;
import play.libs.ws.ahc.StandaloneAhcWSClient;
import play.shaded.ahc.org.asynchttpclient.AsyncHttpClient;
import play.shaded.ahc.org.asynchttpclient.AsyncHttpClientConfig;
import play.shaded.ahc.org.asynchttpclient.DefaultAsyncHttpClient;
import play.shaded.ahc.org.asynchttpclient.DefaultAsyncHttpClientConfig;

import java.util.concurrent.CompletionStage;

/**
 * AsyncHttpClient, A WS asyncHttpClient backed by an AsyncHttpClient instance.
 * <p>
 * 初始化顺序：构造方法 → @PostConstruct → afterPropertiesSet → initBean。
 * 销毁顺序   ：@PreDestroy → DisposableBean destroy → destroyBean
 *
 * @author WANGJUNJIE2
 */
@Slf4j
public class AhcClient implements DisposableBean, DefaultBodyWritables, DefaultBodyReadables {

    private static final String name = "wsclient";
    @Getter
    private final ActorSystem actorSystem;
    @Getter
    private final StandaloneAhcWSClient standaloneAhcWSClient;

    public AhcClient() {
        actorSystem = ActorSystem.create(name);
        Materializer materializer = SystemMaterializer.get(actorSystem).materializer();
        AsyncHttpClientConfig asyncHttpClientConfig =
                new DefaultAsyncHttpClientConfig.Builder()
                        .setKeepAlive(true)
                        .setMaxConnectionsPerHost(-1)
                        .setMaxConnections(-1)
                        .setMaxRedirects(5)
                        .setMaxRequestRetry(0)
                        .setShutdownQuietPeriod(0)
                        .setShutdownTimeout(0)
                        .build();
        AsyncHttpClient asyncHttpClient = new DefaultAsyncHttpClient(asyncHttpClientConfig);
        standaloneAhcWSClient = new StandaloneAhcWSClient(asyncHttpClient, materializer);
        log.info("AhcClient constructed");
    }


    public CompletionStage<JsonNode> getFuture(String url) {
        return standaloneAhcWSClient.url(url).get()
                .thenApply(StandaloneWSResponse::getBody)
                .thenApply(Json::parse);
    }

    public CompletionStage<JsonNode> postFuture(String url, JsonNode data) {
        return standaloneAhcWSClient
                .url(url)
                .setContentType("application/json")
                .post(body(Json.stringify(data)))
                .thenApply(StandaloneWSResponse::getBody)
                .thenApply(Json::parse);
    }

    @Override
    public void destroy() {
        try {
            this.standaloneAhcWSClient.close();
        } catch (Exception e) {
            log.error("", e);
        }
    }
}