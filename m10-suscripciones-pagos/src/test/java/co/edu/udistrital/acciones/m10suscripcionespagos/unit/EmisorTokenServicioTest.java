package co.edu.udistrital.acciones.m10suscripcionespagos.unit;

import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.EmisorTokenServicio;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.JwtProperties;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.ServicioProperties;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.UsuarioAutenticado;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.VerificadorJwt;
import co.edu.udistrital.acciones.m10suscripcionespagos.support.RelojControlado;
import co.edu.udistrital.acciones.m10suscripcionespagos.support.TokenTestFactory;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class EmisorTokenServicioTest {

    private static final Instant INICIO = Instant.now().plusSeconds(300);
    private static final long USUARIO_SERVICIO = 900000010L;

    private final VerificadorJwt verificador = new VerificadorJwt(TokenTestFactory.propiedades());

    @Test
    void elTokenIncluyeLasClaimsEsperadasYEsAceptadoPorElVerificador() {
        EmisorTokenServicio emisor = emisor(new RelojControlado(INICIO));

        UsuarioAutenticado usuario = verificador.validar(emisor.obtenerToken());

        assertThat(usuario.userId()).isEqualTo(USUARIO_SERVICIO);
        assertThat(usuario.roles()).containsExactly(UsuarioAutenticado.ROL_SISTEMA);
    }

    @Test
    void reutilizaElTokenAntesDelMargenDeRenovacion() {
        RelojControlado reloj = new RelojControlado(INICIO);
        EmisorTokenServicio emisor = emisor(reloj);
        String primero = emisor.obtenerToken();
        reloj.avanzar(30);

        assertThat(emisor.obtenerToken()).isEqualTo(primero);
    }

    @Test
    void renuevaElTokenAlAcercarseAlVencimiento() {
        RelojControlado reloj = new RelojControlado(INICIO);
        EmisorTokenServicio emisor = emisor(reloj);
        String primero = emisor.obtenerToken();
        reloj.avanzar(55);

        String segundo = emisor.obtenerToken();

        assertThat(segundo).isNotEqualTo(primero);
        assertThat(verificador.validar(segundo).userId()).isEqualTo(USUARIO_SERVICIO);
    }

    private static EmisorTokenServicio emisor(RelojControlado reloj) {
        return new EmisorTokenServicio(new JwtProperties(TokenTestFactory.SECRETO),
                new ServicioProperties(USUARIO_SERVICIO, Duration.ofMinutes(1)), reloj);
    }
}
