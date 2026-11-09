package modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;


// HERENCIA: Deuda hereda directamente de ElementoFinanciero
public class Deuda extends ElementoFinanciero {
    private final String acreedor;
    private final BigDecimal tasaInteresAnual; // en %, ej. 18.5
    private final LocalDate fechaVencimiento;
    private BigDecimal saldoPendiente; // empieza igual al monto

    public Deuda(long id, String nombre, BigDecimal monto, LocalDate fecha,
            long usuarioId, String moneda, String acreedor,
            BigDecimal tasaInteresAnual, LocalDate fechaVencimiento) {
        super(id, nombre, monto, fecha, usuarioId, moneda);
        if (tasaInteresAnual == null || tasaInteresAnual.signum() < 0) {
            throw new IllegalArgumentException("La tasa de interés no puede ser negativa");
        }
        Objects.requireNonNull(fechaVencimiento, "La fecha de vencimiento es obligatoria");
        if (!fechaVencimiento.isAfter(fecha)) {
            throw new IllegalArgumentException("La fecha de vencimiento debe ser posterior a hoy");
        }
        this.acreedor = textoObligatorio(acreedor, "Acreedor");
        this.tasaInteresAnual = tasaInteresAnual;
        this.fechaVencimiento = fechaVencimiento;
        this.saldoPendiente = getMonto();
    }

    public String getAcreedor() {
        return acreedor;
    }

    public BigDecimal getTasaInteresAnual() {
        return tasaInteresAnual;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public BigDecimal getSaldoPendiente() {
        return saldoPendiente;
    }

    /** Cuánto interés se pagaría aproximadamente en un mes con el saldo actual. */
    public BigDecimal getInteresMensualEstimado() {
        return saldoPendiente.multiply(tasaInteresAnual)
                .divide(BigDecimal.valueOf(1200), 2, RoundingMode.HALF_UP);
    }

    public boolean estaVencida(LocalDate hoy) {
        return saldoPendiente.signum() > 0 && hoy.isAfter(fechaVencimiento);
    }

    public void registrarPago(BigDecimal pago) {
        if (pago == null || pago.signum() <= 0) {
            throw new IllegalArgumentException("El pago debe ser mayor que cero");
        }
        if (pago.compareTo(saldoPendiente) > 0) {
            throw new IllegalArgumentException("El pago supera el saldo pendiente");
        }
        saldoPendiente = saldoPendiente.subtract(pago).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getTipo() {
        return "DEUDA";
    }

    /**
     * La deuda es un pasivo; solo afecta el balance cuando se registra un pago como
     * gasto.
     */
    @Override
    public BigDecimal getImpactoEnBalance() {
        return BigDecimal.ZERO;
    }

    @Override
    public String getDetalleEspecifico() {
        return String.format("Acreedor: %s | Tasa: %s%% | Vence: %s | Saldo: %s",
                acreedor, tasaInteresAnual, fechaVencimiento, saldoPendiente.toPlainString());
    }
}
