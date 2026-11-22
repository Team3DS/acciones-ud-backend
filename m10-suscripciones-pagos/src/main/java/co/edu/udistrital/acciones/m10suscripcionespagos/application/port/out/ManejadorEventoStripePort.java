package co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out;

import co.edu.udistrital.acciones.m10suscripcionespagos.domain.EventoStripe;

public interface ManejadorEventoStripePort {

    boolean soporta(String tipo);

    void procesar(EventoStripe evento);
}
