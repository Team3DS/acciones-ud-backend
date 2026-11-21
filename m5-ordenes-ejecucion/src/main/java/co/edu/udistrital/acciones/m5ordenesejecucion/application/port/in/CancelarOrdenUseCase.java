package co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in;

public interface CancelarOrdenUseCase {
    void ejecutar(Long ordenId, Long usuarioId, String motivo);
}
