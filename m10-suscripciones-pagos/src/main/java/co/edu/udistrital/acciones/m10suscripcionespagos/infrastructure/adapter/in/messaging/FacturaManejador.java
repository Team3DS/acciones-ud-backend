package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.adapter.in.messaging;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.ManejadorEventoStripePort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.service.SincronizadorRecibos;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.EventoStripe;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class FacturaManejador implements ManejadorEventoStripePort {

    private static final Set<String> TIPOS = Set.of("invoice.paid", "invoice.payment_failed");

    private final SincronizadorRecibos sincronizador;

    public FacturaManejador(SincronizadorRecibos sincronizador) {
        this.sincronizador = sincronizador;
    }

    @Override
    public boolean soporta(String tipo) {
        return TIPOS.contains(tipo);
    }

    @Override
    public void procesar(EventoStripe evento) {
        sincronizador.sincronizar(evento.getObjetoId());
    }
}
