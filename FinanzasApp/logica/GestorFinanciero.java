package logica;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

import modelo.*;


/*
 * La lógica del programa. La ventana solo pide y muestra datos,
 * y el trabajo real (totales, consejos, guardado) lo hace esta clase.
 */
public class GestorFinanciero {

    // Cada persona tiene su propio archivo dentro de la carpeta "datos"
    private static final String CARPETA = "datos";
    private static final String ARCHIVO_ANTIGUO = "finanzas.dat"; // versión anterior (un solo usuario)

    private final Usuario usuario;
    private final File archivo; // donde se guardan los datos de este usuario
    private final List<ElementoFinanciero> elementos = new ArrayList<>();
    private long siguienteId = 1;

    public GestorFinanciero(Usuario usuario) {
        this.usuario = usuario;
        this.archivo = archivoDe(usuario.getNombre());
    }

    /* ----- Usuarios guardados ----- */

    // Archivo de una persona: datos/<nombre>.dat (sin tildes raras ni símbolos en el nombre del archivo)
    private static File archivoDe(String nombre) {
        String limpio = nombre.trim().toLowerCase().replaceAll("[^a-z0-9_-]", "_");
        return new File(CARPETA, limpio + ".dat");
    }

    public static boolean existeUsuario(String nombre) {
        return archivoDe(nombre).exists();
    }

    // Nombres de todas las personas que ya tienen datos guardados
    public static List<String> listarUsuarios() {
        List<String> nombres = new ArrayList<>();
        File[] archivos = new File(CARPETA).listFiles((carpeta, nombre) -> nombre.endsWith(".dat"));
        if (archivos != null) {
            Arrays.sort(archivos);
            for (File f : archivos) {
                try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(f))) {
                    nombres.add(((Usuario) in.readObject()).getNombre());
                } catch (IOException | ClassNotFoundException | ClassCastException ex) {
                    // archivo dañado: simplemente no aparece en la lista
                }
            }
        }
        return nombres;
    }

    // Si quedó el finanzas.dat de la versión anterior, lo pasa a la carpeta "datos" para no perder nada
    public static void migrarArchivoAntiguo() {
        File viejo = new File(ARCHIVO_ANTIGUO);
        if (!viejo.exists()) {
            return;
        }
        GestorFinanciero g = leer(viejo);
        if (g != null && !g.archivo.exists() && g.guardar()) {
            viejo.renameTo(new File(ARCHIVO_ANTIGUO + ".viejo")); // se conserva una copia por seguridad
        }
    }

    public Usuario getUsuario() {
        return usuario;
    }

    // Lista de solo lectura: desde afuera nadie puede agregar o quitar cosas sin
    // pasar por el gestor
    public List<ElementoFinanciero> getElementos() {
        return Collections.unmodifiableList(elementos);
    }

    public long getSiguienteId() {
        return siguienteId;
    }

    public void agregar(ElementoFinanciero e) {
        Objects.requireNonNull(e, "No se puede agregar un elemento vacío");
        elementos.add(e);
        siguienteId = Math.max(siguienteId, e.getId() + 1);
    }

    public void eliminarMovimiento(long id) {
        ElementoFinanciero objetivo = null;
        for (ElementoFinanciero e : elementos) {
            if (e.getId() == id) {
                objetivo = e;
            }
        }
        if (objetivo == null) {
            throw new IllegalArgumentException("No existe un movimiento con ese id.");
        }
        if (!(objetivo instanceof Ingreso || objetivo instanceof Gasto)) {
            throw new IllegalArgumentException("Solo se pueden eliminar ingresos y gastos.");
        }
        if (objetivo instanceof GastoVariable g && g.esAutomatico()) {
            throw new IllegalArgumentException("Este gasto se creó al pagar una deuda o aportar a una meta, "
                    + "por eso no se puede eliminar.");
        }
        elementos.remove(objetivo);
    }

    public void pagarDeuda(Deuda deuda, BigDecimal pago) {
        deuda.registrarPago(pago); // si el pago es inválido lanza excepción y no se registra el gasto
        // El pago sale de tu dinero, por eso también se registra como gasto
        agregar(new GastoVariable(siguienteId, "Pago deuda: " + deuda.getNombre(), pago, LocalDate.now(),
                usuario.getId(), usuario.getMonedaBase(), "Deudas", true));
    }

    public void aportarMeta(MetaAhorro meta, BigDecimal aporte) {
        meta.aportar(aporte);
        // El dinero apartado deja de estar disponible, así que se registra como gasto
        agregar(new GastoVariable(siguienteId, "Aporte a meta: " + meta.getNombre(), aporte, LocalDate.now(),
                usuario.getId(), usuario.getMonedaBase(), "Ahorro", true));
    }

    /* ----- Totales ----- */
    public BigDecimal getTotalIngresos() {
        BigDecimal total = BigDecimal.ZERO;
        for (ElementoFinanciero e : elementos) {
            if (e instanceof Ingreso)
                total = total.add(e.getMonto());
        }
        return total;
    }

    public BigDecimal getTotalGastos() {
        BigDecimal total = BigDecimal.ZERO;
        for (ElementoFinanciero e : elementos) {
            if (e instanceof Gasto)
                total = total.add(e.getMonto());
        }
        return total;
    }

    public BigDecimal getBalance() {
        BigDecimal balance = BigDecimal.ZERO;
        for (ElementoFinanciero e : elementos) {
            balance = balance.add(e.getImpactoEnBalance()); // POLIMORFISMO: cada clase sabe si suma, resta o nada
        }
        return balance;
    }

    public BigDecimal getDeudaPendiente() {
        BigDecimal total = BigDecimal.ZERO;
        for (ElementoFinanciero e : elementos) {
            if (e instanceof Deuda d)
                total = total.add(d.getSaldoPendiente());
        }
        return total;
    }

    // Suma de gastos por categoría, ordenados de mayor a menor
    public Map<String, BigDecimal> getGastosPorCategoria() {
        Map<String, BigDecimal> sumas = new LinkedHashMap<>();
        for (ElementoFinanciero e : elementos) {
            if (e instanceof Gasto g) {
                sumas.merge(g.getCategoria(), g.getMonto(), BigDecimal::add);
            }
        }
        List<Map.Entry<String, BigDecimal>> lista = new ArrayList<>(sumas.entrySet());
        lista.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        Map<String, BigDecimal> ordenado = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> e : lista) {
            ordenado.put(e.getKey(), e.getValue());
        }
        return ordenado;
    }

    public List<String> getConsejos() {
        List<String> consejos = new ArrayList<>();
        BigDecimal ingresos = getTotalIngresos();
        BigDecimal gastos = getTotalGastos();

        if (ingresos.signum() > 0) {
            BigDecimal pct = gastos.multiply(BigDecimal.valueOf(100)).divide(ingresos, 1, RoundingMode.HALF_UP);
            consejos.add("Estás gastando el " + pct + "% de tus ingresos.");
            if (pct.compareTo(BigDecimal.valueOf(100)) > 0) {
                consejos.add("Gastas más de lo que ganas. Revisa primero tus gastos variables.");
            } else if (pct.compareTo(BigDecimal.valueOf(80)) > 0) {
                consejos.add("Te queda poco margen. Una guía común es 50% necesidades, 30% gustos y 20% ahorro.");
            } else {
                consejos.add("Tienes margen para ahorrar. Considera aportar a una meta.");
            }
        } else {
            consejos.add("Registra tus ingresos para recibir consejos.");
        }

        Map<String, BigDecimal> porCategoria = getGastosPorCategoria();
        if (!porCategoria.isEmpty() && gastos.signum() > 0) {
            Map.Entry<String, BigDecimal> mayor = porCategoria.entrySet().iterator().next();
            BigDecimal parte = mayor.getValue().multiply(BigDecimal.valueOf(100))
                    .divide(gastos, 1, RoundingMode.HALF_UP);
            consejos.add("Tu mayor gasto es \"" + mayor.getKey() + "\" (" + parte + "% de tus gastos).");
        }

        LocalDate hoy = LocalDate.now();
        for (ElementoFinanciero e : elementos) {
            if (e instanceof Deuda d && d.estaVencida(hoy)) {
                consejos.add("La deuda \"" + d.getNombre() + "\" está vencida. Intenta pagarla pronto.");
            }
        }
        return consejos;
    }

    /*
     * ----- Guardar y cargar (serialización: Java guarda los objetos tal cual en un
     * archivo) -----
     */
    public boolean guardar() {
        archivo.getParentFile().mkdirs(); // crea la carpeta "datos" si no existe
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(archivo))) {
            out.writeObject(usuario);
            out.writeLong(siguienteId);
            out.writeObject(new ArrayList<>(elementos));
            return true;
        } catch (IOException ex) {
            System.err.println("No se pudo guardar: " + ex.getMessage());
            return false;
        }
    }

    // Carga los datos de una persona por su nombre (null si no existe o no se pudo leer)
    public static GestorFinanciero cargar(String nombre) {
        return leer(archivoDe(nombre));
    }

    @SuppressWarnings("unchecked")
    private static GestorFinanciero leer(File archivo) {
        if (!archivo.exists()) {
            return null;
        }
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(archivo))) {
            Usuario u = (Usuario) in.readObject();
            long sig = in.readLong();
            List<ElementoFinanciero> lista = (List<ElementoFinanciero>) in.readObject();
            GestorFinanciero g = new GestorFinanciero(u);
            g.elementos.addAll(lista);
            g.siguienteId = sig;
            return g;
        } catch (IOException | ClassNotFoundException | ClassCastException ex) {
            System.err.println("No se pudo leer " + archivo + " (¿cambiaste las clases del modelo?): " + ex.getMessage());
            return null;
        }
    }
}
