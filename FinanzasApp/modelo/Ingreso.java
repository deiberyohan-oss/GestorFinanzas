package modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

// HERENCIA: Ingreso hereda de ElementoFinanciero (usa super para reutilizar el constructor)
public abstract class Ingreso extends ElementoFinanciero {

    protected Ingreso(long id, String nombre, BigDecimal monto,
            LocalDate fecha, long usuarioId, String moneda) {
        super(id, nombre, monto, fecha, usuarioId, moneda);
    }

    @Override
    public BigDecimal getImpactoEnBalance() {
        return getMonto(); // suma
    }
}
