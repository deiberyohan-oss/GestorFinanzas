package vista;

import javax.swing.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;

import logica.GestorFinanciero;
import modelo.*;


// Inicio de sesión: elegir un usuario guardado o crear uno nuevo
public class Registro {

    // Devuelve el gestor del usuario que entró, o null si la persona decidió salir
    public static GestorFinanciero iniciarSesion() {
        GestorFinanciero.migrarArchivoAntiguo();
        while (true) {
            List<String> usuarios = GestorFinanciero.listarUsuarios();
            if (usuarios.isEmpty()) {
                return pedirDatosIniciales(); // todavía no hay nadie registrado
            }

            JComboBox<String> cmbUsuarios = new JComboBox<>(usuarios.toArray(new String[0]));
            cmbUsuarios.setFont(Ui.F_NORMAL);
            JPanel f = Ui.panelFormulario();
            Ui.agregarFila(f, "¿Quién va a entrar?", cmbUsuarios);

            String[] opciones = { "Entrar", "Usuario nuevo", "Salir" };
            int r = JOptionPane.showOptionDialog(null, f, "Mis Finanzas", JOptionPane.DEFAULT_OPTION,
                    JOptionPane.PLAIN_MESSAGE, null, opciones, opciones[0]);

            if (r == 0) {
                GestorFinanciero g = GestorFinanciero.cargar((String) cmbUsuarios.getSelectedItem());
                if (g != null) {
                    return g;
                }
                Ui.aviso(null, "No se pudieron leer los datos de ese usuario.");
            } else if (r == 1) {
                GestorFinanciero g = pedirDatosIniciales();
                if (g != null) {
                    return g;
                }
                // si canceló la creación, vuelve a mostrar la lista
            } else {
                return null; // Salir (o cerró la ventana)
            }
        }
    }

    // Formulario para crear un usuario nuevo: nombre, moneda y salario
    public static GestorFinanciero pedirDatosIniciales() {
        JPanel f = Ui.panelFormulario();
        JTextField txtNombre = Ui.campo("");
        JComboBox<String> cmbMoneda = new JComboBox<>(new String[] { "COP", "USD", "EUR", "MXN" });
        cmbMoneda.setEditable(true); // permite escribir otro código ISO
        JTextField txtSalario = Ui.campo("");
        Ui.agregarFila(f, "Tu nombre:", txtNombre);
        Ui.agregarFila(f, "Moneda (código ISO):", cmbMoneda);
        Ui.agregarFila(f, "Salario base mensual:", txtSalario);

        GestorFinanciero[] resultado = new GestorFinanciero[1]; // arreglo de 1 para poder usarlo dentro de la lambda
        Ui.formulario(null, "Crear usuario", f, () -> {
            String moneda;
            try {
                moneda = Currency.getInstance(String.valueOf(cmbMoneda.getSelectedItem()).trim().toUpperCase())
                        .getCurrencyCode();
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(
                        "Código de moneda no válido. Usa el formato ISO, ej. COP, USD, EUR.");
            }
            BigDecimal salario = Ui.leerMonto(txtSalario.getText(), false);
            Usuario u = new Usuario(1L, txtNombre.getText(), moneda);
            if (GestorFinanciero.existeUsuario(u.getNombre())) {
                throw new IllegalArgumentException("Ya existe un usuario con ese nombre. "
                        + "Usa otro o entra con ese usuario desde la lista.");
            }
            GestorFinanciero g = new GestorFinanciero(u);
            g.agregar(new IngresoFijo(g.getSiguienteId(), "Salario base", salario, LocalDate.now(),
                    u.getId(), moneda, Frecuencia.MENSUAL));
            g.guardar();
            resultado[0] = g;
        });
        return resultado[0];
    }
}
