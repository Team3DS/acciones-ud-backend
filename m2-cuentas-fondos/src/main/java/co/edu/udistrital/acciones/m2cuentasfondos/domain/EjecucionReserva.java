package co.edu.udistrital.acciones.m2cuentasfondos.domain;

import co.edu.udistrital.acciones.m2cuentasfondos.domain.excepcion.TransicionReservaInvalidaException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/** Ejecución aplicada por REST o por eventos, compartida por los dos caminos de entrega. */
@Entity
@Table(name = "reserva_ejecucion")
public class EjecucionReserva {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "reserva_id", nullable = false)
    private Long reservaId;
    @Column(name = "execution_id", nullable = false, length = 100)
    private String executionId;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monto;
    @Column(name = "final_execution", nullable = false)
    private boolean finalExecution;
    @Column(name = "expected_execution_count")
    private Integer expectedExecutionCount;
    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    protected EjecucionReserva() {
    }

    public static EjecucionReserva crear(Long reservaId, String executionId, BigDecimal monto,
                                        boolean finalExecution, Integer expectedExecutionCount, Instant ahora) {
        var ejecucion = new EjecucionReserva();
        ejecucion.reservaId = reservaId;
        ejecucion.executionId = executionId;
        ejecucion.monto = MonetaryUtil.validarPositivo(monto);
        ejecucion.finalExecution = finalExecution;
        ejecucion.expectedExecutionCount = expectedExecutionCount;
        ejecucion.creadoEn = ahora;
        return ejecucion;
    }

    public void verificarRepeticion(BigDecimal monto, boolean finalExecution, Integer expectedExecutionCount) {
        if (this.monto.compareTo(monto) != 0 || this.finalExecution != finalExecution
                || !java.util.Objects.equals(this.expectedExecutionCount, expectedExecutionCount)) {
            throw new TransicionReservaInvalidaException(
                    "La ejecución " + executionId + " ya fue aplicada con otros datos");
        }
    }

    public Long getId() { return id; }
}
