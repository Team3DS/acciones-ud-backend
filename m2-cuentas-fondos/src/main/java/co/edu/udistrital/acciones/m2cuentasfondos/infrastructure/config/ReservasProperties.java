package co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "app.reservas")
public class ReservasProperties {

    private Duration ttl = Duration.ofHours(24);
    private Expiracion expiracion = new Expiracion();

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }

    public Expiracion getExpiracion() {
        return expiracion;
    }

    public void setExpiracion(Expiracion expiracion) {
        this.expiracion = expiracion;
    }

    public static class Expiracion {

        private boolean habilitada = true;
        private long intervaloMs = 60000;
        private int lote = 100;

        public boolean isHabilitada() {
            return habilitada;
        }

        public void setHabilitada(boolean habilitada) {
            this.habilitada = habilitada;
        }

        public long getIntervaloMs() {
            return intervaloMs;
        }

        public void setIntervaloMs(long intervaloMs) {
            this.intervaloMs = intervaloMs;
        }

        public int getLote() {
            return lote;
        }

        public void setLote(int lote) {
            this.lote = lote;
        }
    }
}
