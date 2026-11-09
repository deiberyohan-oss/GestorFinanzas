package modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;


// HERENCIA: GastoFijo hereda de Gasto, que a su vez hereda de ElementoFinanciero
public class GastoFijo extends Gasto {
    private final Frecuencia frecuencia;

    public GastoFijo(long id, String nombre, BigDecimal monto, LocalDate fecha,
            long usuarioId, String moneda, Frecuencia frecuencia) {
        super(id, nombre, monto, fecha, usuarioId, moneda);
        this.frecuencia = Objects.requireNonNull(frecuencia);
    }

    @Override
    public String getTipo() {
        return "GASTO FIJO";
    }

    @Override
    public String getCategoria() {
        return "Gastos fijos";
    }

    @Override
    public String getDetalleEspecifico() {
        return "Frecuencia: " + frecuencia;
    }
}
