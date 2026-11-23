package co.edu.udistrital.acciones.m4datosmercado.unit;

import co.edu.udistrital.acciones.m4datosmercado.infrastructure.adapter.out.rest.CatalogoRestClient;
import co.edu.udistrital.acciones.m4datosmercado.infrastructure.security.EmisorTokenServicio;
import co.edu.udistrital.acciones.m4datosmercado.infrastructure.security.JwtProperties;
import co.edu.udistrital.acciones.m4datosmercado.infrastructure.security.JwtTokenValidator;
import co.edu.udistrital.acciones.m4datosmercado.infrastructure.security.UsuarioAutenticado;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class EmisorTokenServicioTest {

    private static final JwtProperties PROPIEDADES = new JwtProperties("clave-de-prueba-de-al-menos-32-caracteres");
    private final EmisorTokenServicio emisor = new EmisorTokenServicio(PROPIEDADES);

    @Test
    void elTokenEsValidoConRolSistemaYSeReutiliza() {
        String token = emisor.obtenerToken();

        UsuarioAutenticado usuario = new JwtTokenValidator(PROPIEDADES).autenticar(token);
        assertEquals(900000004L, usuario.id());
        assertTrue(usuario.roles().contains("ROLE_SISTEMA"));
        assertSame(token, emisor.obtenerToken());
    }

    @Test
    void lasPeticionesAM3LlevanElToken() {
        AtomicReference<ClientRequest> enviada = new AtomicReference<>();
        WebClient cliente = WebClient.builder()
                .filter(CatalogoRestClient.conToken(emisor))
                .exchangeFunction(request -> {
                    enviada.set(request);
                    return Mono.just(ClientResponse.create(org.springframework.http.HttpStatus.OK).build());
                })
                .build();

        cliente.method(HttpMethod.GET).uri(URI.create("http://m3/api/v1/instruments/validate")).retrieve()
                .toBodilessEntity().block();

        assertEquals("Bearer " + emisor.obtenerToken(), enviada.get().headers().getFirst(HttpHeaders.AUTHORIZATION));
    }
}
