package modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

// HERENCIA: Gasto hereda de ElementoFinanciero (usa super para reutilizar el constructor)
public abstract class Gasto extends ElementoFinanciero {

    protected Gasto(long id, String nombre, BigDecimal monto,
            LocalDate fecha, long usuarioId, String moneda) {
        super(id, nombre, monto, fecha, usuarioId, moneda);
    }

    @Override
    public BigDecimal getImpactoEnBalance() {
        return getMonto().negate(); // resta
    }

    /** Categoría del gasto (se usa para el gráfico del resumen). */
    public abstract String getCategoria();
}
