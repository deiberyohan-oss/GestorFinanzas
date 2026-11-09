package modelo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Objects;


// ABSTRACCIÓN: nadie puede hacer "new ElementoFinanciero(...)", solo sus clases
// hijas.
// "implements Serializable" permite guardar estos objetos en un archivo.
public abstract class ElementoFinanciero implements Serializable {
    private final long id;
    private final String nombre;
    private final BigDecimal monto;
    private final LocalDate fecha;
    private final long usuarioId;
    private final String moneda;

    protected ElementoFinanciero(long id, String nombre, BigDecimal monto,
            LocalDate fecha, long usuarioId, String moneda) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        Objects.requireNonNull(monto, "El monto es obligatorio");
        Objects.requireNonNull(fecha, "La fecha es obligatoria");
        if (monto.signum() <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor que cero");
        }
        this.id = id;
        this.nombre = nombre.trim();
        this.monto = monto.setScale(2, RoundingMode.HALF_UP);
        this.fecha = fecha;
        this.usuarioId = usuarioId;
        // Lanza IllegalArgumentException si el código ISO no existe (COP, USD, EUR...)
        this.moneda = Currency.getInstance(moneda.toUpperCase()).getCurrencyCode();
    }

    // ENCAPSULAMIENTO: atributos private final y solo getters (el objeto es casi
    // inmutable)
    public long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public long getUsuarioId() {
        return usuarioId;
    }

    public String getMoneda() {
        return moneda;
    }

    /** Validación básica reutilizable por todas las subclases. */
    public boolean esMontoValido() {
        return monto.signum() > 0;
    }

    /** Cada subclase dice qué tipo de elemento es. */
    public abstract String getTipo();

    /**
     * Efecto sobre el balance: ingresos suman, gastos restan, deudas y metas no.
     */
    public abstract BigDecimal getImpactoEnBalance();

    /** Detalles propios de cada subclase. */
    public abstract String getDetalleEspecifico();

    /**
     * Método plantilla: la parte común vive aquí y la específica en la subclase (se
     * usa en consola).
     */
    public void mostrarDetalles() {
        System.out.printf("[%s] #%d %s | %s %s | %s | %s%n",
                getTipo(), id, nombre, monto.toPlainString(), moneda, fecha,
                getDetalleEspecifico());
    }

    // Método de ayuda para validar los textos obligatorios (fuente, categoría,
    // acreedor)
    protected static String textoObligatorio(String texto, String campo) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("El campo \"" + campo + "\" es obligatorio");
        }
        return texto.trim();
    }
}
