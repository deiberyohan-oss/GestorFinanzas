package modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

// HERENCIA: IngresoFijo hereda de Ingreso, que a su vez hereda de ElementoFinanciero
public class IngresoFijo extends Ingreso {
    private final Frecuencia frecuencia;

    public IngresoFijo(long id, String nombre, BigDecimal monto, LocalDate fecha,
            long usuarioId, String moneda, Frecuencia frecuencia) {
        super(id, nombre, monto, fecha, usuarioId, moneda);
        this.frecuencia = Objects.requireNonNull(frecuencia);
    }

    @Override
    public String getTipo() {
        return "INGRESO FIJO";
    }

    @Override
    public String getDetalleEspecifico() {
        return "Frecuencia: " + frecuencia;
    }
}
