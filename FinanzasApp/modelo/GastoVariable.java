package modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;


// HERENCIA: GastoVariable hereda de Gasto
public class GastoVariable extends Gasto {
    private final String categoria;
    private final boolean automatico; // true si lo creó el programa (pago de deuda o aporte a meta)

    public GastoVariable(long id, String nombre, BigDecimal monto, LocalDate fecha,
            long usuarioId, String moneda, String categoria) {
        this(id, nombre, monto, fecha, usuarioId, moneda, categoria, false);
    }

    public GastoVariable(long id, String nombre, BigDecimal monto, LocalDate fecha,
            long usuarioId, String moneda, String categoria, boolean automatico) {
        super(id, nombre, monto, fecha, usuarioId, moneda);
        this.categoria = textoObligatorio(categoria, "Categoría");
        this.automatico = automatico;
    }

    public boolean esAutomatico() {
        return automatico;
    }

    @Override
    public String getTipo() {
        return "GASTO VARIABLE";
    }

    @Override
    public String getCategoria() {
        return categoria;
    }

    @Override
    public String getDetalleEspecifico() {
        return "Categoría: " + categoria;
    }
}
