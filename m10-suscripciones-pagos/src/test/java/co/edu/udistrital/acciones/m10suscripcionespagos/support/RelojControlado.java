package co.edu.udistrital.acciones.m10suscripcionespagos.support;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

public final class RelojControlado extends Clock {

    private Instant ahora;
    private ZoneId zona = ZoneOffset.UTC;

    public RelojControlado(Instant ahora) {
        this.ahora = ahora;
    }

    public void avanzar(long segundos) {
        ahora = ahora.plusSeconds(segundos);
    }

    @Override
    public ZoneId getZone() {
        return zona;
    }

    @Override
    public Clock withZone(ZoneId zona) {
        this.zona = zona;
        return this;
    }

    @Override
    public Instant instant() {
        return ahora;
    }
}
