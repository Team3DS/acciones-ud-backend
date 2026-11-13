package co.edu.udistrital.acciones.m3catalogomercados.integration;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import co.edu.udistrital.acciones.m3catalogomercados.domain.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;

import java.util.List;

class CatalogoApiIT extends CatalogoIntegrationSupport {
    @Test
    void migracionesCarganElModeloYLasDiezAcciones() throws Exception {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM mercado", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM sector", Integer.class)).isEqualTo(6);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM accion", Integer.class)).isEqualTo(10);
        assertThat(jdbc.queryForList("SELECT ticker FROM accion ORDER BY ticker", String.class))
                .containsExactly(
                        "AAPL", "AMZN", "GOOGL", "JNJ", "JPM", "META", "MSFT", "NVDA", "TSLA",
                        "XOM");
        mvc.perform(get("/api/v1/instruments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(10))
                .andExpect(jsonPath("$.content[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$.content[0].price").doesNotExist());
    }

    @Test
    void mercadosYDetallePublicanZonaMonedaYMic() throws Exception {
        mvc.perform(get("/api/v1/markets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/v1/markets/nasdaq"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("NASDAQ"))
                .andExpect(jsonPath("$.mic").value("XNAS"))
                .andExpect(jsonPath("$.timeZone").value("America/New_York"))
                .andExpect(jsonPath("$.currency").value("USD"));
        mvc.perform(get("/api/v1/markets/UNKNOWN"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void sectoresYDetallePorId() throws Exception {
        mvc.perform(get("/api/v1/instruments/sectors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6));
        long id = jdbc.queryForObject("SELECT id FROM accion WHERE ticker='AAPL'", Long.class);
        mvc.perform(get("/api/v1/instruments/id/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("AAPL"));
        mvc.perform(get("/api/v1/instruments/id/999999")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/instruments/id/0")).andExpect(status().isBadRequest());
    }

    @Test
    void filtraPorMercadoSectorYBusquedaSinImportarMayusculas() throws Exception {
        mvc.perform(
                        get("/api/v1/instruments")
                                .param("market", "nasdaq")
                                .param("sector", "tecnologia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(get("/api/v1/instruments").param("query", "apple"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/v1/instruments").param("query", "mSf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].symbol").value("MSFT"));
    }

    @Test
    void paginaConOrdenDeterministaYDevuelvePaginaVacia() throws Exception {
        mvc.perform(get("/api/v1/instruments").param("page", "1").param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.content[0].symbol").value("JNJ"))
                .andExpect(jsonPath("$.totalPages").value(4));
        mvc.perform(get("/api/v1/instruments").param("page", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(10));
        mvc.perform(get("/api/v1/instruments").param("sort", "symbol").param("direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].symbol").value("XOM"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"COMPANY_NAME", "MARKET", "SECTOR"})
    void ordenaPorMetadatosPermitidos(String sort) throws Exception {
        mvc.perform(get("/api/v1/instruments").param("sort", sort))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(10));
    }

    @Test
    void escapaComodinesYNoInterpretaLaBusquedaComoSql() throws Exception {
        for (String query : List.of("%", "_", "!", "' OR 1=1 --")) {
            mvc.perform(get("/api/v1/instruments").param("query", query))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    @Test
    void validaLoteConocidoDesconocidoYDuplicadosSinMutarCatalogo() throws Exception {
        mvc.perform(
                        post("/api/v1/instruments/validate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"symbols\":[\"aapl\",\"ZZZZ\",\"AAPL\"],\"market\":\"nasdaq\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].valid").value(true))
                .andExpect(jsonPath("$[0].instrument.market.code").value("NASDAQ"))
                .andExpect(jsonPath("$[1].valid").value(false))
                .andExpect(jsonPath("$[1].reason").value("NO_ENCONTRADO"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM accion", Integer.class)).isEqualTo(10);
    }

    @Test
    void accionDeshabilitadaNoSeListaComoOperable() throws Exception {
        jdbc.update("UPDATE accion SET habilitada=false WHERE ticker='AAPL'");
        mvc.perform(get("/api/v1/instruments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(9));
        mvc.perform(get("/api/v1/instruments").param("enabled", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/v1/instruments/AAPL/validation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.reason").value("ACCION_DESHABILITADA"));
        mvc.perform(get("/api/v1/instruments/AAPL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void mercadoDeshabilitadoImpideOperarTodasSusAcciones() throws Exception {
        jdbc.update("UPDATE mercado SET habilitado=false WHERE codigo='NASDAQ'");
        mvc.perform(get("/api/v1/markets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/v1/markets").param("enabled", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("NASDAQ"));
        mvc.perform(get("/api/v1/instruments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(get("/api/v1/instruments/AAPL/validation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.reason").value("MERCADO_DESHABILITADO"));
    }

    @Test
    void tickerRepetidoEntreMercadosExigeDesambiguar() throws Exception {
        jdbc.update(
                "INSERT INTO accion(mercado_id,sector_id,ticker,nombre_empresa) SELECT"
                        + " m.id,s.id,'AAPL','Otra cotización' FROM mercado m,sector s WHERE"
                        + " m.codigo='NYSE' AND s.codigo='TECNOLOGIA'");
        mvc.perform(get("/api/v1/instruments/AAPL"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SIMBOLO_AMBIGUO"));
        mvc.perform(get("/api/v1/instruments/AAPL/validation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
        mvc.perform(get("/api/v1/instruments/AAPL").param("market", "NASDAQ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Apple Inc."));
    }

    @Test
    void parametrosInvalidosSon400ConErrorEstructurado() throws Exception {
        for (String uri :
                List.of(
                        "/api/v1/instruments?page=-1",
                        "/api/v1/instruments?size=101",
                        "/api/v1/instruments?size=no",
                        "/api/v1/instruments?sort=price",
                        "/api/v1/instruments?direction=bad",
                        "/api/v1/instruments?market=BAD!")) {
            mvc.perform(get(uri))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").exists());
        }
        mvc.perform(get("/api/v1/instruments/ZZZZ")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/instruments/ZZZZ/validation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{}",
                "{\"symbols\":[]}",
                "{\"symbols\":[null]}",
                "{\"symbols\":[\"\"]}",
                "{\"symbols\":[\"SYMBOL_TOO_LONG_FOR_M3_LIMIT\"]}",
                "not-json"
            })
    void rechazaCuerposInvalidos(String body) throws Exception {
        mvc.perform(
                        post("/api/v1/instruments/validate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SOLICITUD_INVALIDA"));
    }

    @Test
    void noExponeEscrituraAdministrativa() throws Exception {
        mvc.perform(
                        post("/api/v1/instruments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/instruments/AAPL")).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void saludYContratoOpenApiDisponibles() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/instruments/validate']").exists());
    }

    @Test
    void adaptadorPermiteListadosCompletosSinFiltrosYListaVacia() {
        assertThat(repository.listarMercados(null)).hasSize(2);
        assertThat(repository.buscarPorSimbolos(List.of(), null)).isEmpty();
        var page =
                repository.buscar(
                        new FiltroInstrumentos(
                                " ", null, null, null, 0, 20, OrdenCatalogo.SYMBOL, false));
        assertThat(page.totalElements()).isEqualTo(10);
    }

    @Test
    void exigeTokenYRechazaTokensVencidosAlteradosYEsquemasDistintos() throws Exception {
        for (String header :
                List.of(
                        "",
                        "Bearer invalid",
                        "Bearer ",
                        "Basic abc",
                        "Bearer " + token(java.time.Instant.now().minusSeconds(60), jwtSecret),
                        "Bearer "
                                + token(
                                        java.time.Instant.now().plusSeconds(600),
                                        "another-test-key-for-invalid-signature-1234567890123456789012345678"))) {
            mvc.perform(get("/api/v1/instruments").header("Authorization", header))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.traceId").isNotEmpty())
                    .andExpect(jsonPath("$.path").value("/api/v1/instruments"));
        }
    }

    @Test
    void documentacionYSaludPermanecenPublicas() throws Exception {
        mvc.perform(get("/actuator/health").header("Authorization", "")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs").header("Authorization", "Bearer invalid"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
    }

    @Test
    void propagaCorrelacionEnRespuestaYErroresSinDejarMdc() throws Exception {
        mvc.perform(get("/api/v1/instruments/ZZZZ").header("X-Correlation-ID", "m3-review-123"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("X-Correlation-ID", "m3-review-123"))
                .andExpect(jsonPath("$.traceId").value("m3-review-123"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.error").value("Not Found"));
        assertThat(org.slf4j.MDC.get("traceId")).isNull();
        mvc.perform(get("/api/v1/markets").header("X-Correlation-ID", "invalid value"))
                .andExpect(status().isOk())
                .andExpect(
                        header().string(
                                        "X-Correlation-ID",
                                        org.hamcrest.Matchers.matchesPattern("[a-f0-9-]{36}")));
    }

    @Test
    void ofreceLasRutasDeWikiSinRomperLasActuales() throws Exception {
        mvc.perform(get("/api/v1/catalog/markets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/v1/catalog/markets/XNAS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("NASDAQ"));
        mvc.perform(get("/api/v1/instruments/AAPL/tradable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void interpretaFiltrosDeWikiOrdenYPaginacion() throws Exception {
        mvc.perform(
                        get("/api/v1/instruments")
                                .param("filter[market]", "XNAS")
                                .param("filter[sector]", "TECNOLOGIA")
                                .param("sort", "symbol,desc")
                                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].symbol").value("NVDA"))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.numberOfElements").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false))
                .andExpect(jsonPath("$.empty").value(false));
        mvc.perform(
                        get("/api/v1/instruments")
                                .param("filter[symbol]", "aapl")
                                .param("filter[type]", "ACCION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/v1/instruments").param("symbol", "AAP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(
                        get("/api/v1/instruments")
                                .param("filter[query]", "Apple")
                                .param("sort", "companyName,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void compruebaFiltrosDeshabilitadosYConflictosSinAmpliarElCatalogo() throws Exception {
        jdbc.update("UPDATE accion SET habilitada=false WHERE ticker='AAPL'");
        mvc.perform(get("/api/v1/instruments").param("filter[enabled]", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        for (String uri :
                List.of(
                        "/api/v1/instruments?market=NYSE&filter[market]=NASDAQ",
                        "/api/v1/instruments?filter[enabled]=maybe",
                        "/api/v1/instruments?type=UNKNOWN",
                        "/api/v1/instruments?sort=symbol,asc,extra",
                        "/api/v1/instruments?sort=symbol,asc&direction=desc")) {
            mvc.perform(get(uri)).andExpect(status().isBadRequest());
        }
    }
}
