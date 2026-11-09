package modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;


// HERENCIA: MetaAhorro hereda directamente de ElementoFinanciero
public class MetaAhorro extends ElementoFinanciero {
    private final LocalDate fechaLimite;
    private BigDecimal montoAhorrado = BigDecimal.ZERO.setScale(2);

    // 'monto' de la superclase representa el objetivo a alcanzar
    public MetaAhorro(long id, String nombre, BigDecimal montoObjetivo, LocalDate fecha,
            long usuarioId, String moneda, LocalDate fechaLimite) {
        super(id, nombre, montoObjetivo, fecha, usuarioId, moneda);
        Objects.requireNonNull(fechaLimite, "La fecha límite es obligatoria");
        if (!fechaLimite.isAfter(fecha)) {
            throw new IllegalArgumentException("La fecha límite debe ser posterior al inicio");
        }
        this.fechaLimite = fechaLimite;
    }

    public LocalDate getFechaLimite() {
        return fechaLimite;
    }

    public BigDecimal getMontoAhorrado() {
        return montoAhorrado;
    }

    public BigDecimal getFaltante() {
        return getMonto().subtract(montoAhorrado);
    }

    public void aportar(BigDecimal aporte) {
        if (aporte == null || aporte.signum() <= 0) {
            throw new IllegalArgumentException("El aporte debe ser mayor que cero");
        }
        if (aporte.compareTo(getFaltante()) > 0) {
            throw new IllegalArgumentException("El aporte supera lo que falta para la meta");
        }
        montoAhorrado = montoAhorrado.add(aporte).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getPorcentajeAvance() {
        return montoAhorrado.multiply(BigDecimal.valueOf(100))
                .divide(getMonto(), 1, RoundingMode.HALF_UP);
    }

    public boolean estaCumplida() {
        return montoAhorrado.compareTo(getMonto()) >= 0;
    }

    @Override
    public String getTipo() {
        return "META DE AHORRO";
    }

    @Override
    public BigDecimal getImpactoEnBalance() {
        return BigDecimal.ZERO;
    }

    @Override
    public String getDetalleEspecifico() {
        return String.format("Ahorrado: %s (%s%%) | Límite: %s%s",
                montoAhorrado.toPlainString(), getPorcentajeAvance(), fechaLimite,
                estaCumplida() ? " | ¡CUMPLIDA!" : "");
    }
}
