package co.edu.udistrital.acciones.m10suscripcionespagos.domain;

public record TransicionSuscripcion(EstadoSuscripcion anterior, EstadoSuscripcion nuevo) {

    public boolean huboCambio() {
        return anterior != nuevo;
    }

    public boolean veniaDePremium() {
        return anterior != null && anterior.otorgaPremium();
    }

    public boolean llegoA(EstadoSuscripcion estado) {
        return nuevo == estado;
    }
}
