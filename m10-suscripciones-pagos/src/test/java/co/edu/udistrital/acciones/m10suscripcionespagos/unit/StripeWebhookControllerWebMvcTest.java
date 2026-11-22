package co.edu.udistrital.acciones.m10suscripcionespagos.unit;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.EventoStripeRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase.RegistrarEventoStripeUseCase;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.EventoStripe;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.adapter.in.rest.StripeWebhookController;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config.GlobalExceptionHandler;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config.StripeProperties;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.EscritorProblemas;
import co.edu.udistrital.acciones.m10suscripcionespagos.support.FirmaStripeTestFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StripeWebhookControllerWebMvcTest {

    private static final String SECRETO = "whsec_prueba";
    private static final String RUTA = "/api/v1/payments/webhook/stripe";
    private static final Instant AHORA = Instant.parse("2026-05-01T10:00:00Z");
    private static final long MARCA = Instant.now().getEpochSecond();

    private final EventoStripeRepositoryPort eventos = mock(EventoStripeRepositoryPort.class);
    private final ObjectMapper json = Jackson2ObjectMapperBuilder.json()
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();
    private final Clock clock = Clock.fixed(AHORA, ZoneOffset.UTC);

    private final MockMvc mockMvc = construir(propiedades(SECRETO));
    private final MockMvc mockMvcSinSecreto = construir(propiedades(""));

    @Test
    void unaFirmaValidaRegistraElEventoYResponde200() throws Exception {
        String cuerpo = FirmaStripeTestFactory.evento("evt_1", "checkout.session.completed",
                "cs_test_1", false);
        when(eventos.registrarSiNuevo(any())).thenReturn(true);

        mockMvc.perform(post(RUTA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", FirmaStripeTestFactory.firmar(SECRETO, cuerpo, MARCA))
                        .content(cuerpo))
                .andExpect(status().isOk());

        ArgumentCaptor<EventoStripe> captor = ArgumentCaptor.forClass(EventoStripe.class);
        verify(eventos).registrarSiNuevo(captor.capture());
        EventoStripe registrado = captor.getValue();
        assertThat(registrado.getId()).isEqualTo("evt_1");
        assertThat(registrado.getTipo()).isEqualTo("checkout.session.completed");
        assertThat(registrado.getObjetoId()).isEqualTo("cs_test_1");
        assertThat(registrado.getRecibidoEn()).isEqualTo(AHORA);
    }

    @Test
    void unEventoRepetidoTambienResponde200() throws Exception {
        String cuerpo = FirmaStripeTestFactory.evento("evt_1", "invoice.paid", "in_1", false);
        when(eventos.registrarSiNuevo(any())).thenReturn(true, false);
        String firma = FirmaStripeTestFactory.firmar(SECRETO, cuerpo, MARCA);

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", firma).content(cuerpo))
                .andExpect(status().isOk());
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", firma).content(cuerpo))
                .andExpect(status().isOk());

        verify(eventos, org.mockito.Mockito.times(2)).registrarSiNuevo(any());
    }

    @Test
    void unaFirmaAlteradaResponde400SinRegistrar() throws Exception {
        String cuerpo = FirmaStripeTestFactory.evento("evt_1", "invoice.paid", "in_1", false);
        String firma = FirmaStripeTestFactory.firmar(SECRETO, cuerpo, MARCA);
        String alterada = firma.substring(0, firma.length() - 1)
                + (firma.endsWith("0") ? "1" : "0");

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", alterada).content(cuerpo))
                .andExpect(status().isBadRequest());

        verify(eventos, never()).registrarSiNuevo(any());
    }

    @Test
    void unCuerpoAlteradoResponde400SinRegistrar() throws Exception {
        String original = FirmaStripeTestFactory.evento("evt_1", "invoice.paid", "in_1", false);
        String alterado = FirmaStripeTestFactory.evento("evt_1", "invoice.paid", "in_2", false);
        String firma = FirmaStripeTestFactory.firmar(SECRETO, original, MARCA);

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", firma).content(alterado))
                .andExpect(status().isBadRequest());

        verify(eventos, never()).registrarSiNuevo(any());
    }

    @Test
    void sinHeaderDeFirmaResponde400() throws Exception {
        String cuerpo = FirmaStripeTestFactory.evento("evt_1", "invoice.paid", "in_1", false);

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest());

        verify(eventos, never()).registrarSiNuevo(any());
    }

    @Test
    void unaMarcaFueraDeToleranciaResponde400() throws Exception {
        String cuerpo = FirmaStripeTestFactory.evento("evt_1", "invoice.paid", "in_1", false);
        String firma = FirmaStripeTestFactory.firmar(SECRETO, cuerpo, MARCA - 600);

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", firma).content(cuerpo))
                .andExpect(status().isBadRequest());

        verify(eventos, never()).registrarSiNuevo(any());
    }

    @Test
    void unEventoLiveResponde200SinRegistrar() throws Exception {
        String cuerpo = FirmaStripeTestFactory.evento("evt_live", "invoice.paid", "in_1", true);

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", FirmaStripeTestFactory.firmar(SECRETO, cuerpo, MARCA))
                        .content(cuerpo))
                .andExpect(status().isOk());

        verify(eventos, never()).registrarSiNuevo(any());
    }

    @Test
    void unSecretoVacioResponde503() throws Exception {
        String cuerpo = FirmaStripeTestFactory.evento("evt_1", "invoice.paid", "in_1", false);

        mockMvcSinSecreto.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", FirmaStripeTestFactory.firmar(SECRETO, cuerpo, MARCA))
                        .content(cuerpo))
                .andExpect(status().isServiceUnavailable());

        verify(eventos, never()).registrarSiNuevo(any());
    }

    private MockMvc construir(StripeProperties propiedades) {
        RegistrarEventoStripeUseCase useCase = new RegistrarEventoStripeUseCase(
                propiedades, eventos, json, clock);
        return MockMvcBuilders
                .standaloneSetup(new StripeWebhookController(useCase))
                .setControllerAdvice(new GlobalExceptionHandler(new EscritorProblemas(json, clock)))
                .setMessageConverters(new ByteArrayHttpMessageConverter(),
                        new MappingJackson2HttpMessageConverter(json))
                .build();
    }

    private static StripeProperties propiedades(String secreto) {
        return new StripeProperties("sk_test_clave", secreto, null, null, null, 2,
                null, null, null, null, null);
    }
}
