package modelo;

import java.io.Serializable;


public class Usuario implements Serializable {
    private final long id;
    private final String nombre;
    private final String monedaBase;

    public Usuario(long id, String nombre, String monedaBase) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        this.id = id;
        this.nombre = nombre.trim();
        this.monedaBase = monedaBase;
    }

    public long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getMonedaBase() {
        return monedaBase;
    }
}
