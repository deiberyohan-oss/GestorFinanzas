package modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;


// HERENCIA: IngresoVariable hereda de Ingreso
public class IngresoVariable extends Ingreso {
    private final String fuente;

    public IngresoVariable(long id, String nombre, BigDecimal monto, LocalDate fecha,
            long usuarioId, String moneda, String fuente) {
        super(id, nombre, monto, fecha, usuarioId, moneda);
        this.fuente = textoObligatorio(fuente, "Fuente");
    }

    @Override
    public String getTipo() {
        return "INGRESO VARIABLE";
    }

    @Override
    public String getDetalleEspecifico() {
        return "Fuente: " + fuente;
    }
}
