package co.edu.udistrital.acciones.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.embedded.netty.NettyWebServer;
import org.springframework.boot.web.reactive.context.ReactiveWebServerApplicationContext;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;

import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

@SpringBootTest(
        classes = {GatewayApplication.class, GatewayTransportSmokeTest.BackendConfiguration.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "management.health.redis.enabled=false",
            "management.health.rabbit.enabled=false",
            "debug=false",
            "logging.level.root=WARN"
        })
class GatewayTransportSmokeTest {
    private static final DisposableServer BACKEND =
            HttpServer.create()
                    .host("127.0.0.1")
                    .port(0)
                    .route(
                            routes ->
                                    routes.get(
                                                    "/market/ws/quotes/info",
                                                    (request, response) ->
                                                            response.header(
                                                                            "Content-Type",
                                                                            "application/json")
                                                                    .sendString(
                                                                            Mono.just(
                                                                                    "{\"websocket\":true}")))
                                            .ws(
                                                    "/market/ws/quotes/000/test/websocket",
                                                    (inbound, outbound) ->
                                                            outbound.sendString(
                                                                    inbound.receive().asString())))
                    .bindNow();

    @LocalServerPort int port;
    @Autowired ReactiveWebServerApplicationContext context;
    @Autowired GatewayProperties properties;

    @AfterAll
    static void stopBackend() {
        BACKEND.disposeNow();
    }

    @Test
    void arrancaConNettyYRespondeSaludSinTomcat() {
        assertThat(context.getWebServer()).isInstanceOf(NettyWebServer.class);
        client().get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.status")
                .isEqualTo("UP");
    }

    @Test
    void sockJsInfoUsaHttpYConservaLaReescrituraDeRuta() {
        client().get()
                .uri("/api/v1/market/ws/quotes/info")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.websocket")
                .isEqualTo(true);
    }

    @Test
    void proxyWebsocketCompletaHandshakeYTransmiteMensajes() {
        assertThat(
                        properties.getRoutes().stream()
                                .filter(route -> "m4-datos-mercado-ws".equals(route.getId()))
                                .findFirst()
                                .orElseThrow()
                                .getUri()
                                .getScheme())
                .isEqualTo("ws");
        AtomicReference<String> received = new AtomicReference<>();
        URI uri =
                URI.create(
                        "ws://127.0.0.1:" + port + "/api/v1/market/ws/quotes/000/test/websocket");

        new ReactorNettyWebSocketClient()
                .execute(
                        uri,
                        session ->
                                session.send(Mono.just(session.textMessage("cotizacion")))
                                        .then(
                                                session.receive()
                                                        .take(1)
                                                        .doOnNext(
                                                                message ->
                                                                        received.set(
                                                                                message
                                                                                        .getPayloadAsText()))
                                                        .then()))
                .block(Duration.ofSeconds(10));

        assertThat(received.get()).isEqualTo("cotizacion");
    }

    private WebTestClient client() {
        return WebTestClient.bindToServer()
                .baseUrl("http://127.0.0.1:" + port)
                .responseTimeout(Duration.ofSeconds(10))
                .build();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class BackendConfiguration {
        @Bean
        static BeanPostProcessor localBackendForConfiguredRoutes() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String name) {
                    if (bean instanceof GatewayProperties gateway) {
                        gateway.getRoutes().stream()
                                .filter(
                                        route ->
                                                "m4-datos-mercado-ws".equals(route.getId())
                                                        || "m4-datos-mercado-sockjs"
                                                                .equals(route.getId()))
                                .forEach(
                                        route ->
                                                route.setUri(
                                                        URI.create(
                                                                route.getUri().getScheme()
                                                                        + "://127.0.0.1:"
                                                                        + BACKEND.port())));
                    }
                    return bean;
                }
            };
        }
    }
}
