package co.edu.udistrital.acciones.m10suscripcionespagos.integration;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase.ProcesarEventosStripeUseCase;
import co.edu.udistrital.acciones.m10suscripcionespagos.support.FirmaStripeTestFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class M10WebhookIntegrationTest extends AbstractIntegrationTest {

    private static final String SECRETO = "whsec_integracion";
    private static final String RUTA = "/api/v1/payments/webhook/stripe";
    private static final String PREFIJO = "evt_test_";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ProcesarEventosStripeUseCase procesarEventos;

    @BeforeEach
    void preparar() {
        limpiar();
    }

    @AfterEach
    void limpiar() {
        jdbc.update("DELETE FROM evento_stripe WHERE id LIKE ?", PREFIJO + "%");
    }

    @Test
    void flywayAplicaLaMigracionDelInbox() {
        Integer aplicadas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '3' AND success = true",
                Integer.class);

        assertThat(aplicadas).isEqualTo(1);
    }

    @Test
    void unaFirmaValidaGuardaElEvento() {
        String id = PREFIJO + "valido";
        String cuerpo = FirmaStripeTestFactory.evento(id, "checkout.session.completed", "cs_test_1", false);

        ResponseEntity<String> respuesta = enviar(cuerpo, firmar(cuerpo, Instant.now()));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> fila = jdbc.queryForMap("SELECT * FROM evento_stripe WHERE id = ?", id);
        assertThat(fila.get("tipo")).isEqualTo("checkout.session.completed");
        assertThat(fila.get("objeto_id")).isEqualTo("cs_test_1");
        assertThat(fila.get("payload")).isEqualTo(cuerpo);
        assertThat(fila.get("recibido_en")).isNotNull();
        assertThat(fila.get("siguiente_intento_en")).isNotNull();
        assertThat(fila.get("procesado_en")).isNull();
        assertThat(((Number) fila.get("intentos")).intValue()).isZero();
    }

    @Test
    void elMismoEventoDosVecesDejaUnaSolaFila() {
        String id = PREFIJO + "repetido";
        String cuerpo = FirmaStripeTestFactory.evento(id, "invoice.paid", "in_test_1", false);
        String firma = firmar(cuerpo, Instant.now());

        assertThat(enviar(cuerpo, firma).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(enviar(cuerpo, firma).getStatusCode()).isEqualTo(HttpStatus.OK);

        Integer filas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM evento_stripe WHERE id = ?", Integer.class, id);
        assertThat(filas).isEqualTo(1);
    }

    @Test
    void unaFirmaAlteradaResponde400YNoGuardaNada() {
        String id = PREFIJO + "firma_alterada";
        String cuerpo = FirmaStripeTestFactory.evento(id, "invoice.paid", "in_test_2", false);
        String firma = firmar(cuerpo, Instant.now());
        String alterada = firma.substring(0, firma.length() - 1)
                + (firma.endsWith("0") ? "1" : "0");

        ResponseEntity<String> respuesta = enviar(cuerpo, alterada);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(contar(id)).isZero();
    }

    @Test
    void unCuerpoAlteradoResponde400YNoGuardaNada() {
        String id = PREFIJO + "cuerpo_alterado";
        String original = FirmaStripeTestFactory.evento(id, "invoice.paid", "in_test_3", false);
        String alterado = FirmaStripeTestFactory.evento(id, "invoice.paid", "in_test_4", false);
        String firma = firmar(original, Instant.now());

        ResponseEntity<String> respuesta = enviar(alterado, firma);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(contar(id)).isZero();
    }

    @Test
    void sinHeaderDeFirmaResponde400() {
        String id = PREFIJO + "sin_header";
        String cuerpo = FirmaStripeTestFactory.evento(id, "invoice.paid", "in_test_5", false);

        ResponseEntity<String> respuesta = enviar(cuerpo, null);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(contar(id)).isZero();
    }

    @Test
    void unaMarcaFueraDeToleranciaResponde400() {
        String id = PREFIJO + "fuera_tolerancia";
        String cuerpo = FirmaStripeTestFactory.evento(id, "invoice.paid", "in_test_6", false);
        String firma = firmar(cuerpo, Instant.now().minusSeconds(600));

        ResponseEntity<String> respuesta = enviar(cuerpo, firma);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(contar(id)).isZero();
    }

    @Test
    void unEventoLiveResponde200SinGuardar() {
        String id = PREFIJO + "live";
        String cuerpo = FirmaStripeTestFactory.evento(id, "invoice.paid", "in_test_7", true);

        ResponseEntity<String> respuesta = enviar(cuerpo, firmar(cuerpo, Instant.now()));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(contar(id)).isZero();
    }

    @Test
    void elProcesadorMarcaProcesadoUnEventoAjeno() {
        String id = PREFIJO + "ajeno";
        String cuerpo = FirmaStripeTestFactory.evento(id, "customer.subscription.updated", "sub_test_1",
                false);
        assertThat(enviar(cuerpo, firmar(cuerpo, Instant.now())).getStatusCode()).isEqualTo(HttpStatus.OK);

        procesarEventos.procesarPendientes();

        Map<String, Object> fila = jdbc.queryForMap(
                "SELECT procesado_en, intentos FROM evento_stripe WHERE id = ?", id);
        assertThat(fila.get("procesado_en")).isNotNull();
        assertThat(((Number) fila.get("intentos")).intValue()).isZero();
    }

    @Test
    void elProcesadorRespetaElTopeDeIntentos() {
        String id = PREFIJO + "tope";
        jdbc.update("""
                INSERT INTO evento_stripe (id, tipo, objeto_id, payload, recibido_en,
                                           intentos, siguiente_intento_en)
                VALUES (?, 'customer.subscription.updated', 'sub_tope', '{}',
                        now() - interval '2 hours', 10, now() - interval '1 hour')
                """, id);

        procesarEventos.procesarPendientes();

        Map<String, Object> fila = jdbc.queryForMap(
                "SELECT procesado_en, intentos FROM evento_stripe WHERE id = ?", id);
        assertThat(fila.get("procesado_en")).isNull();
        assertThat(((Number) fila.get("intentos")).intValue()).isEqualTo(10);
    }

    @Test
    void elProcesadorRespetaElSiguienteIntentoFuturo() {
        String id = PREFIJO + "futuro";
        jdbc.update("""
                INSERT INTO evento_stripe (id, tipo, objeto_id, payload, recibido_en,
                                           intentos, siguiente_intento_en)
                VALUES (?, 'customer.subscription.updated', 'sub_futuro', '{}',
                        now(), 0, now() + interval '1 hour')
                """, id);

        procesarEventos.procesarPendientes();

        Map<String, Object> fila = jdbc.queryForMap(
                "SELECT procesado_en FROM evento_stripe WHERE id = ?", id);
        assertThat(fila.get("procesado_en")).isNull();
    }

    private ResponseEntity<String> enviar(String cuerpo, String firma) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (firma != null) {
            headers.set("Stripe-Signature", firma);
        }
        return rest.exchange(RUTA, HttpMethod.POST,
                new HttpEntity<>(cuerpo.getBytes(StandardCharsets.UTF_8), headers), String.class);
    }

    private Integer contar(String id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM evento_stripe WHERE id = ?", Integer.class, id);
    }

    private static String firmar(String cuerpo, Instant instante) {
        return FirmaStripeTestFactory.firmar(SECRETO, cuerpo, instante);
    }
}
