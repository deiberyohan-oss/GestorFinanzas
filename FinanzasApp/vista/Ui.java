package vista;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

// Colores, fuentes y métodos de ayuda que se usan en toda la interfaz
public class Ui {
    // Fondo de las pantallas: un degradado suave de lavanda a azul claro
    public static final Color FONDCoO_ARRIBA = new Color(0xF5F3FF);
    public static final Color FONDO_ABAJO = new Color(0xDDE5FF);
    // Menú lateral: degradado de azul noche a violeta
    public static final Color LATERAL_ARRIBA = new Color(0x1E1B4B);
    public static final Color LATERAL_ABAJO = new Color(0x5B21B6);
    public static final Color BORDE = new Color(0xE0E7FF);

    public static final Color TEXTO = new Color(0x1E1B4B);
    public static final Color GRIS = new Color(0x6B6A8A);
    public static final Color VERDE = new Color(0x059669);
    public static final Color ROJO = new Color(0xE11D48);
    public static final Color AZUL = new Color(0x4F46E5);
    public static final Color NARANJA = new Color(0xD97706);

    public static final Font F_TITULO = new Font("SansSerif", Font.BOLD, 26);
    public static final Font F_NORMAL = new Font("SansSerif", Font.PLAIN, 14);
    public static final Font F_NEGRITA = new Font("SansSerif", Font.BOLD, 13);
    public static final Font F_PEQUENA = new Font("SansSerif", Font.PLAIN, 12);

    // Panel con un degradado vertical de un color a otro (se usa de fondo)
    public static JPanel panelDegradado(LayoutManager layout, Color arriba, Color abajo) {
        JPanel p = new JPanel(layout) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, arriba, 0, getHeight(), abajo));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        p.setOpaque(false); // si fuera opaco, Swing taparía el degradado con un color plano
        return p;
    }

    // Botón redondeado de color
    public static JButton boton(String texto, Color color) {
        JButton b = new JButton(texto) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? color.darker() : color);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setRolloverEnabled(true);
        b.setForeground(Color.WHITE);
        b.setFont(F_NEGRITA);
        b.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static JLabel titulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font("SansSerif", Font.BOLD, 16));
        l.setForeground(TEXTO);
        return l;
    }

    // Número con separador de miles. Si es un valor entero no muestra decimales:
    // 1.500.000
    public static String numero(BigDecimal valor) {
        BigDecimal v = valor.setScale(2, RoundingMode.HALF_UP);
        boolean entero = v.stripTrailingZeros().scale() <= 0;
        return String.format(entero ? "%,.0f" : "%,.2f", v);
    }

    // Número + moneda, ej: 1.500.000 COP
    public static String dinero(BigDecimal valor, String moneda) {
        return numero(valor) + " " + moneda;
    }

    /* ----- Formularios ----- */
    public static JPanel panelFormulario() {
        JPanel p = new JPanel(new GridLayout(0, 2, 12, 12));
        p.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        return p;
    }

    public static JTextField campo(String textoInicial) {
        JTextField c = new JTextField(textoInicial, 20);
        c.setFont(F_NORMAL);
        return c;
    }

    public static void agregarFila(JPanel panel, String etiqueta, JComponent componente) {
        JLabel l = new JLabel(etiqueta);
        l.setFont(F_NORMAL);
        panel.add(l);
        panel.add(componente);
    }

    /**
     * Muestra el formulario en una ventana emergente. Si el usuario pulsa Aceptar
     * se ejecuta
     * la acción; si esa acción lanza IllegalArgumentException (dato inválido), se
     * muestra
     * el mensaje y el formulario vuelve a abrirse con lo que ya se había escrito.
     * Devuelve false si la persona canceló.
     */
    public static boolean formulario(Component padre, String titulo, JPanel panel, Runnable accion) {
        while (true) {
            int r = JOptionPane.showConfirmDialog(padre, panel, titulo,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (r != JOptionPane.OK_OPTION) {
                return false;
            }
            try {
                accion.run();
                return true;
            } catch (IllegalArgumentException ex) {
                aviso(padre, ex.getMessage());
            }
        }
    }

    public static void aviso(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, "Revisa esto", JOptionPane.WARNING_MESSAGE);
    }

    /*
     * ----- Lectura segura de datos escritos (lanzan IllegalArgumentException con
     * mensaje claro) -----
     */
    public static BigDecimal leerMonto(String texto, boolean permitirCero) {
        String t = texto.trim().replace(",", ".");
        if (t.isEmpty()) {
            throw new IllegalArgumentException("Escribe un valor numérico.");
        }
        BigDecimal n;
        try {
            n = new BigDecimal(t);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Número no válido. Escríbelo sin puntos de miles "
                    + "(ej. 1500000 o 1500000.50).");
        }
        if (n.signum() < 0 || (n.signum() == 0 && !permitirCero)) {
            throw new IllegalArgumentException(permitirCero
                    ? "El valor no puede ser negativo."
                    : "El valor debe ser mayor que cero.");
        }
        return n;
    }

    public static LocalDate leerFecha(String texto, boolean vacioEsHoy) {
        String t = texto.trim();
        if (t.isEmpty() && vacioEsHoy) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(t);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Fecha no válida. Usa el formato AAAA-MM-DD, ej. 2026-12-31.");
        }
    }
}
