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

    /**
     * 默认配置参考源码
     * play-ws/play-ws-standalone/src/main/scala/play/api/libs/ws/WSClientConfig.scala
     * case class WSClientConfig(
     * connectionTimeout: Duration = 2.minutes,
     * idleTimeout: Duration = 2.minutes,
     * requestTimeout: Duration = 2.minutes,
     * followRedirects: Boolean = true,
     * useProxyProperties: Boolean = true,
     * userAgent: Option[String] = None,
     * compressionEnabled: Boolean = false,
     * ssl: SSLConfigSettings = SSLConfigSettings())
     * 下载 sbt-1.3.5.msi安装，地址 https://github-releases.githubusercontent.com/279553/62f49d00-1dac-11ea-9210-9d4af1b7a5d5?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKIAIWNJYAX4CSVEH53A%2F20210209%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20210209T104757Z&X-Amz-Expires=300&X-Amz-Signature=31d37079f0656410d881c0f452a0460647f06d5ff317ffd654fa9bdad4b34db7&X-Amz-SignedHeaders=host&actor_id=12050743&key_id=0&repo_id=279553&response-content-disposition=attachment%3B%20filename%3Dsbt-1.3.5.msi&response-content-type=application%2Foctet-stream
     * sbt 提速，参考 https://www.jianshu.com/p/be367db9345c
     * 编辑 ~/.sbt/repositories
     * [repositories]
     * local
     * huaweicloud-maven: https://repo.huaweicloud.com/repository/maven/
     * maven-central: http://maven.aliyun.com/nexus/content/groups/public
     * sbt-plugin-repo: https://repo.scala-sbt.org/scalasbt/sbt-plugin-releases, [organization]/[module]/(scala_[scalaVersion]/)(sbt_[sbtVersion]/)[revision]/[type]s/[artifact](-[classifier]).[ext]
     */
    public AhcClient() {
        actorSystem = ActorSystem.create(name);
        Materializer materializer = SystemMaterializer.get(actorSystem).materializer();
        AsyncHttpClientConfig asyncHttpClientConfig =
                new DefaultAsyncHttpClientConfig.Builder()
                        .setKeepAlive(true)
                        .setMaxConnectionsPerHost(-1)
                        .setMaxConnections(-1)
                        .setFollowRedirect(true)
                        .setMaxRedirects(5)
                        .setMaxRequestRetry(0)
                        .setShutdownQuietPeriod(0)
                        .setShutdownTimeout(0)
                        .build();
        DefaultAsyncHttpClient asyncHttpClient = new DefaultAsyncHttpClient(asyncHttpClientConfig);
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