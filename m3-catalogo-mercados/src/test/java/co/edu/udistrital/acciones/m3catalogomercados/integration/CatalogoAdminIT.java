package co.edu.udistrital.acciones.m3catalogomercados.integration;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import co.edu.udistrital.acciones.m3catalogomercados.infrastructure.adapter.in.scheduling.CatalogoOutboxRelay;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

class CatalogoAdminIT extends CatalogoIntegrationSupport {
    @Autowired ObjectMapper mapper;
    @Autowired PlatformTransactionManager manager;

    String admin() {
        return "Bearer "
                + Jwts.builder()
                        .subject("99")
                        .claim("roles", List.of("ROLE_ADMINISTRADOR"))
                        .expiration(Date.from(Instant.now().plusSeconds(600)))
                        .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                        .compact();
    }

    String instrument(String symbol, String type) {
        return "{\"symbol\":\""
                + symbol
                + "\",\"name\":\"Instrumento de"
                + " prueba\",\"market\":\"XNAS\",\"sector\":\"TECNOLOGIA\",\"type\":\""
                + type
                + "\",\"lotSize\":1,\"tickSize\":0.01}";
    }

    @Test
    void creaMercadoConIdempotenciaYEventoAtomico() throws Exception {
        String body =
                "{\"code\":\"TEST\",\"mic\":\"XTST\",\"name\":\"Mercado de"
                    + " prueba\",\"timeZone\":\"America/Bogota\",\"country\":\"Colombia\",\"tradingHours\":{\"open\":\"09:00\",\"close\":\"15:00\"}}";
        String id = UUID.randomUUID().toString();
        for (int i = 0; i < 2; i++)
            mvc.perform(
                            post("/api/v1/catalog/markets")
                                    .header("Authorization", admin())
                                    .header("Idempotency-Key", id)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Idempotency-Replayed", Boolean.toString(i == 1)))
                    .andExpect(jsonPath("$.code").value("TEST"))
                    .andExpect(jsonPath("$.tradingHours.open").value("09:00:00"));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM mercado WHERE codigo='TEST'", Integer.class))
                .isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM outbox_evento WHERE"
                                        + " routing_key='m3.mercado.creado'",
                                Integer.class))
                .isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACCION", "ETF", "OPCION", "FUTURO", "BONO", "CRYPTO"})
    void creaTiposDeWikiSinCambiarLasAccionesIniciales(String type) throws Exception {
        mvc.perform(
                        post("/api/v1/instruments")
                                .header("Authorization", admin())
                                .header("Idempotency-Key", UUID.randomUUID().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(instrument("TST", type)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value(type))
                .andExpect(jsonPath("$.lotSize").value(1));
        mvc.perform(get("/api/v1/instruments").param("filter[type]", type))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(type.equals("ACCION") ? 11 : 1));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM accion WHERE ticker='AAPL'", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void actualizaEstadoYMetadatosYLaElegibilidadRespondeAlCambio() throws Exception {
        String key = UUID.randomUUID().toString();
        String patch =
                "{\"name\":\"Apple"
                        + " revisada\",\"state\":\"SUSPENDIDO\",\"lotSize\":2,\"tickSize\":0.001}";
        for (int i = 0; i < 2; i++)
            mvc.perform(
                            patch("/api/v1/instruments/AAPL")
                                    .param("market", "NASDAQ")
                                    .header("Authorization", admin())
                                    .header("Idempotency-Key", key)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(patch))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Idempotency-Replayed", Boolean.toString(i == 1)))
                    .andExpect(jsonPath("$.state").value("SUSPENDIDO"))
                    .andExpect(jsonPath("$.enabled").value(false));
        mvc.perform(get("/api/v1/instruments/AAPL/tradable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM outbox_evento WHERE"
                                        + " routing_key='m3.instrumento.actualizado'",
                                Integer.class))
                .isEqualTo(1);
        mvc.perform(
                        patch("/api/v1/instruments/AAPL")
                                .header("Authorization", admin())
                                .header("Idempotency-Key", UUID.randomUUID().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"state\":\"ACTIVO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void actualizaMercadoYBloqueaInstrumentosEnUnMercadoDeshabilitado() throws Exception {
        mvc.perform(
                        patch("/api/v1/catalog/markets/XNAS")
                                .header("Authorization", admin())
                                .header("Idempotency-Key", UUID.randomUUID().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"enabled\":false,\"name\":\"Nasdaq"
                                            + " revisado\",\"country\":\"USA\",\"tradingHours\":{\"open\":\"10:00\",\"close\":\"16:00\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
        mvc.perform(get("/api/v1/instruments/AAPL/tradable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reason").value("MERCADO_DESHABILITADO"));
    }

    @Test
    void noPermiteEscriturasSinAdminNiSinClaveIdempotente() throws Exception {
        mvc.perform(
                        post("/api/v1/instruments")
                                .header("Idempotency-Key", "role-test")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(instrument("TST", "ETF")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESO_DENEGADO"));
        mvc.perform(
                        post("/api/v1/instruments")
                                .header("Authorization", admin())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(instrument("TST", "ETF")))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        post("/api/v1/instruments")
                                .header("Authorization", admin())
                                .header("Idempotency-Key", "invalid key")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(instrument("TST", "ETF")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaReutilizarLaClaveConOtroContenido() throws Exception {
        String key = UUID.randomUUID().toString();
        mvc.perform(
                        post("/api/v1/instruments")
                                .header("Authorization", admin())
                                .header("Idempotency-Key", key)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(instrument("TST", "ETF")))
                .andExpect(status().isCreated());
        mvc.perform(
                        post("/api/v1/instruments")
                                .header("Authorization", admin())
                                .header("Idempotency-Key", key)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(instrument("OTHER", "ETF")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCIA_CONFLICTO"));
    }

    @Test
    void noPermiteParcheVacioNiReferenciasDesconocidas() throws Exception {
        mvc.perform(
                        patch("/api/v1/instruments/AAPL")
                                .header("Authorization", admin())
                                .header("Idempotency-Key", UUID.randomUUID().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        patch("/api/v1/catalog/markets/NASDAQ")
                                .header("Authorization", admin())
                                .header("Idempotency-Key", UUID.randomUUID().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        post("/api/v1/instruments")
                                .header("Authorization", admin())
                                .header("Idempotency-Key", UUID.randomUUID().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(instrument("TST", "ETF").replace("XNAS", "UNKNOWN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void validaLotesYTicksYNoFiltraDetallesSql() throws Exception {
        mvc.perform(
                        post("/api/v1/instruments")
                                .header("Authorization", admin())
                                .header("Idempotency-Key", UUID.randomUUID().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        instrument("TST", "ETF")
                                                .replace("\"lotSize\":1", "\"lotSize\":0")))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        post("/api/v1/instruments")
                                .header("Authorization", admin())
                                .header("Idempotency-Key", UUID.randomUUID().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(instrument("AAPL", "ACCION")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATALOGO_CONFLICTO"));
    }

    @Test
    void outboxConservaEventoSiFallaElBrokerYLoMarcaSoloTrasConfirmacion() throws Exception {
        mvc.perform(
                        post("/api/v1/instruments")
                                .header("Authorization", admin())
                                .header("Idempotency-Key", UUID.randomUUID().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(instrument("TST", "ETF")))
                .andExpect(status().isCreated());
        new CatalogoOutboxRelay(
                        jdbc,
                        (id, key, json) -> {
                            throw new IllegalStateException("broker offline");
                        },
                        manager)
                .publicarPendientes();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM outbox_evento WHERE publicado_en IS NULL AND"
                                        + " intentos=1",
                                Integer.class))
                .isEqualTo(1);
        jdbc.update("UPDATE outbox_evento SET proximo_intento=CURRENT_TIMESTAMP");
        new CatalogoOutboxRelay(
                        jdbc,
                        (id, key, json) -> {
                            assertThat(json).contains("m3.instrumento.creado").contains("eventId");
                        },
                        manager)
                .publicarPendientes();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM outbox_evento WHERE publicado_en IS NOT NULL"
                                        + " AND intentos=2",
                                Integer.class))
                .isEqualTo(1);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void reintentosConcurrentesProducenUnSoloInstrumentoYUnSoloEvento() throws Exception {
        String symbol =
                "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        String key = UUID.randomUUID().toString();
        String authorization = admin();
        String body = instrument(symbol, "ETF");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Callable<String> request =
                    () -> {
                        start.await();
                        return mvc.perform(
                                        post("/api/v1/instruments")
                                                .header("Authorization", authorization)
                                                .header("Idempotency-Key", key)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(body))
                                .andExpect(status().isCreated())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();
                    };
            Future<String> a = executor.submit(request);
            Future<String> b = executor.submit(request);
            start.countDown();
            long id = mapper.readTree(a.get(15, TimeUnit.SECONDS)).get("id").asLong();
            assertThat(mapper.readTree(b.get(15, TimeUnit.SECONDS)).get("id").asLong())
                    .isEqualTo(id);
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT count(*) FROM accion WHERE ticker=?",
                                    Integer.class,
                                    symbol))
                    .isEqualTo(1);
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT count(*) FROM outbox_evento WHERE"
                                            + " body->>'aggregateId'=?",
                                    Integer.class,
                                    Long.toString(id)))
                    .isEqualTo(1);
        } finally {
            executor.shutdownNow();
            jdbc.update("DELETE FROM outbox_evento WHERE body->'payload'->>'symbol'=?", symbol);
            jdbc.update("DELETE FROM comando_catalogo WHERE idempotency_key=?", key);
            jdbc.update("DELETE FROM accion WHERE ticker=?", symbol);
        }
    }
}
