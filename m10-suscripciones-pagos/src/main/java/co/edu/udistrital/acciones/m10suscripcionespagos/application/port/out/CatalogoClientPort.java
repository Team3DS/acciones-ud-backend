package co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out;

import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Simbolo;

public interface CatalogoClientPort {

    boolean esValido(Simbolo simbolo);
}
