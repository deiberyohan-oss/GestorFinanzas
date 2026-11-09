package vista;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import logica.GestorFinanciero;
import modelo.*;


/* La ventana: menú lateral y las 4 pantallas (Resumen, Movimientos, Deudas, Metas) */
public class VentanaPrincipal extends JFrame {

    private final GestorFinanciero gestor;
    private final String moneda;

    // Para cambiar de pantalla (CardLayout muestra un panel a la vez)
    private final CardLayout cartas = new CardLayout();
    private final JPanel contenido = new JPanel(cartas);
    private final List<JButton> botonesMenu = new ArrayList<>();

    // Pantalla Resumen
    private final JLabel lblIngresos = new JLabel();
    private final JLabel lblGastos = new JLabel();
    private final JLabel lblBalance = new JLabel();
    private final JLabel lblDeuda = new JLabel();
    private final GraficoCategorias grafico = new GraficoCategorias();
    private final JTextArea txtConsejos = new JTextArea();

    // Pantalla Movimientos (la lista "filasMov" guarda qué objeto hay en cada fila
    // de la tabla)
    private final DefaultTableModel modeloMov = modeloNoEditable("Tipo", "Nombre", "Monto", "Fecha", "Detalle");
    private final JTable tablaMov = new JTable(modeloMov);
    private final List<ElementoFinanciero> filasMov = new ArrayList<>();

    // Pantalla Deudas
    private final DefaultTableModel modeloDeuda = modeloNoEditable("Nombre", "Acreedor", "Monto",
            "Saldo", "Tasa", "Interés/mes", "Vence", "Estado");
    private final JTable tablaDeuda = new JTable(modeloDeuda);
    private final List<Deuda> filasDeuda = new ArrayList<>();

    // Pantalla Metas
    private final JPanel listaMetas = new JPanel();

    public VentanaPrincipal(GestorFinanciero gestor) {
        super("Mis Finanzas");
        this.gestor = gestor;
        this.moneda = gestor.getUsuario().getMonedaBase();

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        // Aunque se cierre la ventana de golpe, los datos quedan guardados
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                gestor.guardar();
            }
        });
        setSize(1120, 700);
        setMinimumSize(new Dimension(1000, 620));
        setLocationRelativeTo(null); // centrada en la pantalla

        contenido.add(crearPaginaResumen(), "resumen");
        contenido.add(crearPaginaMovimientos(), "movimientos");
        contenido.add(crearPaginaDeudas(), "deudas");
        contenido.add(crearPaginaMetas(), "metas");

        setLayout(new BorderLayout());
        add(crearLateral(), BorderLayout.WEST);
        add(contenido, BorderLayout.CENTER);

        actualizarTodo();
        mostrar("resumen");
    }

    /* ---------- Inicio y cierre de sesión ---------- */

    // Pide quién va a entrar y abre su ventana (si la persona elige "Salir", el programa termina)
    public static void iniciarSesion() {
        GestorFinanciero gestor = Registro.iniciarSesion();
        if (gestor == null) {
            System.exit(0);
        }
        new VentanaPrincipal(gestor).setVisible(true);
    }

    // Guarda, oculta esta ventana y deja entrar a otra persona sin cerrar el programa
    private void cerrarSesion() {
        if (!gestor.guardar()) {
            Ui.aviso(this, "No se pudo guardar el archivo de datos. Revisa los permisos de la carpeta.");
            return;
        }
        setVisible(false); // se oculta mientras se elige el siguiente usuario
        GestorFinanciero siguiente = Registro.iniciarSesion();
        if (siguiente == null) {
            System.exit(0);
        }
        dispose(); // ahora sí se cierra esta ventana y se abre la del nuevo usuario
        new VentanaPrincipal(siguiente).setVisible(true);
    }

    /* ---------- Menú lateral ---------- */
    private JPanel crearLateral() {
        JPanel lateral = Ui.panelDegradado(null, Ui.LATERAL_ARRIBA, Ui.LATERAL_ABAJO);
        lateral.setLayout(new BoxLayout(lateral, BoxLayout.Y_AXIS));
        lateral.setBorder(BorderFactory.createEmptyBorder(28, 16, 20, 16));
        lateral.setPreferredSize(new Dimension(220, 0));

        JLabel logo = new JLabel("Mis Finanzas");
        logo.setFont(new Font("SansSerif", Font.BOLD, 22));
        logo.setForeground(Color.WHITE);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel usuario = new JLabel(gestor.getUsuario().getNombre() + "  ·  " + moneda);
        usuario.setFont(Ui.F_PEQUENA);
        usuario.setForeground(new Color(0xC7D2FE));
        usuario.setAlignmentX(Component.LEFT_ALIGNMENT);

        lateral.add(logo);
        lateral.add(Box.createVerticalStrut(4));
        lateral.add(usuario);
        lateral.add(Box.createVerticalStrut(30));

        agregarBotonMenu(lateral, "Resumen", "resumen");
        agregarBotonMenu(lateral, "Movimientos", "movimientos");
        agregarBotonMenu(lateral, "Deudas", "deudas");
        agregarBotonMenu(lateral, "Metas de ahorro", "metas");

        lateral.add(Box.createVerticalGlue());

        JButton btnSalir = Ui.boton("Cerrar sesión", new Color(0x7C3AED));
        btnSalir.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnSalir.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnSalir.addActionListener(ev -> cerrarSesion());
        lateral.add(btnSalir);
        lateral.add(Box.createVerticalStrut(12));

        JLabel pie = new JLabel("Guardado automático");
        pie.setFont(Ui.F_PEQUENA);
        pie.setForeground(new Color(0xA5B4FC));
        pie.setAlignmentX(Component.LEFT_ALIGNMENT);
        lateral.add(pie);
        return lateral;
    }

    private void agregarBotonMenu(JPanel lateral, String texto, String pantalla) {
        // Botón con fondo propio: se pinta más claro si está seleccionado o con el
        // mouse encima
        JButton b = new JButton(texto) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean seleccionado = Boolean.TRUE.equals(getClientProperty("sel"));
                if (seleccionado) {
                    g2.setColor(new Color(255, 255, 255, 45)); // seleccionado: blanco translúcido
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(255, 255, 255, 22)); // mouse encima
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setActionCommand(pantalla);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setRolloverEnabled(true);
        b.setForeground(Color.WHITE);
        b.setFont(Ui.F_NORMAL);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        b.setPreferredSize(new Dimension(188, 42));
        b.addActionListener(ev -> mostrar(pantalla));
        botonesMenu.add(b);
        lateral.add(b);
        lateral.add(Box.createVerticalStrut(6));
    }

    private void mostrar(String pantalla) {
        cartas.show(contenido, pantalla);
        for (JButton b : botonesMenu) {
            b.putClientProperty("sel", pantalla.equals(b.getActionCommand()));
            b.repaint();
        }
    }

    /*
     * ---------- Estructura común de cada pantalla (título + botones + contenido)
     * ----------
     */
    private JPanel crearPagina(String titulo, String subtitulo, JComponent centro, JComponent... acciones) {
        JPanel pagina = Ui.panelDegradado(new BorderLayout(0, 18), Ui.FONDO_ARRIBA, Ui.FONDO_ABAJO);
        pagina.setBorder(BorderFactory.createEmptyBorder(26, 30, 26, 30));

        JPanel textos = new JPanel();
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        textos.setOpaque(false);
        JLabel t = new JLabel(titulo);
        t.setFont(Ui.F_TITULO);
        t.setForeground(Ui.TEXTO);
        JLabel s = new JLabel(subtitulo);
        s.setFont(Ui.F_NORMAL);
        s.setForeground(Ui.GRIS);
        textos.add(t);
        textos.add(s);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        for (JComponent a : acciones) {
            botones.add(a);
        }

        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setOpaque(false);
        encabezado.add(textos, BorderLayout.WEST);
        encabezado.add(botones, BorderLayout.EAST);

        pagina.add(encabezado, BorderLayout.NORTH);
        pagina.add(centro, BorderLayout.CENTER);
        return pagina;
    }

    /* ---------- Pantalla 1: Resumen ---------- */
    private JPanel crearPaginaResumen() {
        JPanel tarjetas = new JPanel(new GridLayout(1, 4, 16, 0));
        tarjetas.setOpaque(false);
        tarjetas.add(tarjetaResumen("Ingresos totales", lblIngresos, Ui.VERDE));
        tarjetas.add(tarjetaResumen("Gastos totales", lblGastos, Ui.ROJO));
        tarjetas.add(tarjetaResumen("Balance", lblBalance, Ui.AZUL));
        tarjetas.add(tarjetaResumen("Deuda pendiente", lblDeuda, Ui.NARANJA));
        tarjetas.setPreferredSize(new Dimension(100, 96));

        Tarjeta cajaGrafico = new Tarjeta(new BorderLayout(0, 12));
        cajaGrafico.add(Ui.titulo("Gastos por categoría"), BorderLayout.NORTH);
        cajaGrafico.add(grafico, BorderLayout.CENTER);

        txtConsejos.setEditable(false);
        txtConsejos.setLineWrap(true);
        txtConsejos.setWrapStyleWord(true);
        txtConsejos.setBackground(Color.WHITE);
        txtConsejos.setBorder(BorderFactory.createEmptyBorder());
        txtConsejos.setFont(Ui.F_NORMAL);
        txtConsejos.setForeground(Ui.TEXTO);
        Tarjeta cajaConsejos = new Tarjeta(new BorderLayout(0, 12));
        cajaConsejos.add(Ui.titulo("Consejos"), BorderLayout.NORTH);
        cajaConsejos.add(txtConsejos, BorderLayout.CENTER);

        JPanel abajo = new JPanel(new GridLayout(1, 2, 16, 0));
        abajo.setOpaque(false);
        abajo.add(cajaGrafico);
        abajo.add(cajaConsejos);

        JPanel centro = new JPanel(new BorderLayout(0, 16));
        centro.setOpaque(false);
        centro.add(tarjetas, BorderLayout.NORTH);
        centro.add(abajo, BorderLayout.CENTER);

        return crearPagina("Resumen", "Así van tus finanzas, " + gestor.getUsuario().getNombre(), centro);
    }

    private Tarjeta tarjetaResumen(String titulo, JLabel valor, Color color) {
        Tarjeta t = new Tarjeta(new BorderLayout(0, 6));
        JLabel l = new JLabel(titulo);
        l.setFont(Ui.F_NORMAL);
        l.setForeground(Ui.GRIS);
        valor.setFont(new Font("SansSerif", Font.BOLD, 17));
        valor.setForeground(color);
        t.add(l, BorderLayout.NORTH);
        t.add(valor, BorderLayout.CENTER);
        return t;
    }

    /* ---------- Pantalla 2: Movimientos (ingresos y gastos) ---------- */
    private JPanel crearPaginaMovimientos() {
        JButton btnIngreso = Ui.boton("+ Ingreso", Ui.VERDE);
        JButton btnGasto = Ui.boton("+ Gasto", Ui.ROJO);
        JButton btnEliminar = Ui.boton("Eliminar", new Color(0x6B6A8A));
        btnIngreso.addActionListener(ev -> dialogoMovimiento(true));
        btnGasto.addActionListener(ev -> dialogoMovimiento(false));
        btnEliminar.addActionListener(ev -> eliminarSeleccionado());
        return crearPagina("Movimientos", "Del más reciente al más antiguo  ·  Valores en " + moneda,
                tablaEnTarjeta(tablaMov, true, 150, 220, 140, 120, 190), btnIngreso, btnGasto, btnEliminar);
    }

    private void dialogoMovimiento(boolean esIngreso) {
        JPanel f = Ui.panelFormulario();
        JComboBox<String> cmbTipo = new JComboBox<>(new String[] { "Fijo", "Variable" });
        JTextField txtNombre = Ui.campo("");
        JTextField txtMonto = Ui.campo("");
        JTextField txtDetalle = Ui.campo("");
        JTextField txtFecha = Ui.campo(LocalDate.now().toString());
        JComboBox<Frecuencia> cmbFrecuencia = new JComboBox<>(Frecuencia.values());

        // La fila "detalle" cambia según el tipo: frecuencia (fijo) o fuente/categoría
        // (variable)
        CardLayout cl = new CardLayout();
        JPanel cartaDetalle = new JPanel(cl);
        cartaDetalle.add(cmbFrecuencia, "Fijo");
        cartaDetalle.add(txtDetalle, "Variable");
        JLabel lblDetalle = new JLabel("Frecuencia:");
        lblDetalle.setFont(Ui.F_NORMAL);
        cmbTipo.addActionListener(ev -> {
            String tipo = (String) cmbTipo.getSelectedItem();
            cl.show(cartaDetalle, tipo);
            lblDetalle.setText(tipo.equals("Fijo") ? "Frecuencia:" : (esIngreso ? "Fuente:" : "Categoría:"));
        });

        Ui.agregarFila(f, "Tipo:", cmbTipo);
        Ui.agregarFila(f, "Nombre:", txtNombre);
        Ui.agregarFila(f, "Monto:", txtMonto);
        f.add(lblDetalle);
        f.add(cartaDetalle);
        Ui.agregarFila(f, "Fecha (AAAA-MM-DD):", txtFecha);

        Ui.formulario(this, esIngreso ? "Nuevo ingreso" : "Nuevo gasto", f, () -> {
            BigDecimal monto = Ui.leerMonto(txtMonto.getText(), false);
            LocalDate fecha = Ui.leerFecha(txtFecha.getText(), true);
            boolean fijo = cmbTipo.getSelectedIndex() == 0;
            Frecuencia frecuencia = (Frecuencia) cmbFrecuencia.getSelectedItem();
            long id = gestor.getSiguienteId();
            long uid = gestor.getUsuario().getId();
            String nombre = txtNombre.getText();
            String texto = txtDetalle.getText();

            // Aquí se crea el objeto: ingreso o gasto, fijo o variable
            ElementoFinanciero nuevo;
            if (esIngreso) {
                nuevo = fijo ? new IngresoFijo(id, nombre, monto, fecha, uid, moneda, frecuencia)
                        : new IngresoVariable(id, nombre, monto, fecha, uid, moneda, texto);
            } else {
                nuevo = fijo ? new GastoFijo(id, nombre, monto, fecha, uid, moneda, frecuencia)
                        : new GastoVariable(id, nombre, monto, fecha, uid, moneda, texto);
            }
            gestor.agregar(nuevo);
            cambioRealizado();
        });
    }

    private void eliminarSeleccionado() {
        int fila = tablaMov.getSelectedRow();
        if (fila < 0) {
            Ui.aviso(this, "Selecciona primero un movimiento de la tabla.");
            return;
        }
        ElementoFinanciero e = filasMov.get(fila);
        int r = JOptionPane.showConfirmDialog(this, "¿Eliminar \"" + e.getNombre() + "\"?",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            gestor.eliminarMovimiento(e.getId());
            cambioRealizado();
        } catch (IllegalArgumentException ex) {
            Ui.aviso(this, ex.getMessage());
        }
    }

    /* ---------- Pantalla 3: Deudas ---------- */
    private JPanel crearPaginaDeudas() {
        JButton btnNueva = Ui.boton("+ Nueva deuda", Ui.AZUL);
        JButton btnPagar = Ui.boton("Pagar deuda", Ui.VERDE);
        btnNueva.addActionListener(ev -> dialogoNuevaDeuda());
        btnPagar.addActionListener(ev -> pagarDeudaSeleccionada());
        return crearPagina("Deudas", "Selecciona una deuda y pulsa \"Pagar deuda\"  ·  Valores en " + moneda,
                tablaEnTarjeta(tablaDeuda, false, 140, 100, 95, 95, 75, 115, 105, 80), btnNueva, btnPagar);
    }

    private void dialogoNuevaDeuda() {
        JPanel f = Ui.panelFormulario();
        JTextField txtNombre = Ui.campo("");
        JTextField txtMonto = Ui.campo("");
        JTextField txtAcreedor = Ui.campo("");
        JTextField txtTasa = Ui.campo("0");
        JTextField txtVence = Ui.campo(LocalDate.now().plusMonths(6).toString());
        Ui.agregarFila(f, "Nombre:", txtNombre);
        Ui.agregarFila(f, "Monto adeudado:", txtMonto);
        Ui.agregarFila(f, "Acreedor (banco, persona):", txtAcreedor);
        Ui.agregarFila(f, "Tasa anual % (0 si no tiene):", txtTasa);
        Ui.agregarFila(f, "Vence (AAAA-MM-DD):", txtVence);

        Ui.formulario(this, "Nueva deuda", f, () -> {
            BigDecimal monto = Ui.leerMonto(txtMonto.getText(), false);
            BigDecimal tasa = Ui.leerMonto(txtTasa.getText(), true);
            LocalDate vence = Ui.leerFecha(txtVence.getText(), false);
            gestor.agregar(new Deuda(gestor.getSiguienteId(), txtNombre.getText(), monto, LocalDate.now(),
                    gestor.getUsuario().getId(), moneda, txtAcreedor.getText(), tasa, vence));
            cambioRealizado();
        });
    }

    private void pagarDeudaSeleccionada() {
        int fila = tablaDeuda.getSelectedRow();
        if (fila < 0) {
            Ui.aviso(this, "Selecciona primero una deuda de la tabla.");
            return;
        }
        Deuda d = filasDeuda.get(fila);
        if (d.getSaldoPendiente().signum() == 0) {
            Ui.aviso(this, "Esa deuda ya está pagada.");
            return;
        }
        JPanel f = Ui.panelFormulario();
        JTextField txtPago = Ui.campo("");
        Ui.agregarFila(f, "Saldo pendiente:", new JLabel(Ui.dinero(d.getSaldoPendiente(), moneda)));
        Ui.agregarFila(f, "Monto a pagar:", txtPago);
        Ui.formulario(this, "Pagar: " + d.getNombre(), f, () -> {
            gestor.pagarDeuda(d, Ui.leerMonto(txtPago.getText(), false));
            cambioRealizado();
        });
    }

    /* ---------- Pantalla 4: Metas de ahorro ---------- */
    private JPanel crearPaginaMetas() {
        JButton btnNueva = Ui.boton("+ Nueva meta", Ui.AZUL);
        btnNueva.addActionListener(ev -> dialogoNuevaMeta());

        listaMetas.setLayout(new BoxLayout(listaMetas, BoxLayout.Y_AXIS));
        listaMetas.setOpaque(false);
        JPanel envoltorio = new JPanel(new BorderLayout()); // mantiene las metas pegadas arriba
        envoltorio.setOpaque(false);
        envoltorio.add(listaMetas, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(envoltorio);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setViewportBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        return crearPagina("Metas de ahorro", "Aparta dinero poco a poco hasta llegar a tu objetivo", scroll, btnNueva);
    }

    private void dialogoNuevaMeta() {
        JPanel f = Ui.panelFormulario();
        JTextField txtNombre = Ui.campo("");
        JTextField txtObjetivo = Ui.campo("");
        JTextField txtLimite = Ui.campo(LocalDate.now().plusMonths(6).toString());
        Ui.agregarFila(f, "Nombre de la meta:", txtNombre);
        Ui.agregarFila(f, "Monto objetivo:", txtObjetivo);
        Ui.agregarFila(f, "Fecha límite (AAAA-MM-DD):", txtLimite);

        Ui.formulario(this, "Nueva meta de ahorro", f, () -> {
            BigDecimal objetivo = Ui.leerMonto(txtObjetivo.getText(), false);
            LocalDate limite = Ui.leerFecha(txtLimite.getText(), false);
            gestor.agregar(new MetaAhorro(gestor.getSiguienteId(), txtNombre.getText(), objetivo,
                    LocalDate.now(), gestor.getUsuario().getId(), moneda, limite));
            cambioRealizado();
        });
    }

    private void aportar(MetaAhorro m) {
        JPanel f = Ui.panelFormulario();
        JTextField txtAporte = Ui.campo("");
        Ui.agregarFila(f, "Falta para la meta:", new JLabel(Ui.dinero(m.getFaltante(), moneda)));
        Ui.agregarFila(f, "Monto a aportar:", txtAporte);
        Ui.formulario(this, "Aportar a: " + m.getNombre(), f, () -> {
            gestor.aportarMeta(m, Ui.leerMonto(txtAporte.getText(), false));
            cambioRealizado();
        });
    }

    private Tarjeta crearTarjetaMeta(MetaAhorro m) {
        Tarjeta t = new Tarjeta(new BorderLayout(20, 0));
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        t.setPreferredSize(new Dimension(100, 100));
        t.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        JLabel nombre = new JLabel(m.getNombre());
        nombre.setFont(new Font("SansSerif", Font.BOLD, 16));
        nombre.setForeground(Ui.TEXTO);
        JLabel ahorrado = new JLabel("Ahorrado " + Ui.dinero(m.getMontoAhorrado(), moneda)
                + " de " + Ui.dinero(m.getMonto(), moneda));
        ahorrado.setFont(Ui.F_PEQUENA);
        ahorrado.setForeground(Ui.GRIS);
        JLabel limite = new JLabel("Fecha límite: " + m.getFechaLimite());
        limite.setFont(Ui.F_PEQUENA);
        limite.setForeground(Ui.GRIS);
        JPanel textos = new JPanel();
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        textos.setOpaque(false);
        textos.setPreferredSize(new Dimension(340, 60));
        textos.add(nombre);
        textos.add(ahorrado);
        textos.add(limite);

        BarraProgreso barra = new BarraProgreso(m.estaCumplida() ? Ui.VERDE : Ui.AZUL);
        barra.setPorcentaje(m.getPorcentajeAvance().doubleValue());
        JLabel pct = new JLabel(m.getPorcentajeAvance() + "% completado");
        pct.setFont(Ui.F_PEQUENA);
        pct.setForeground(Ui.GRIS);
        JPanel centro = new JPanel(new BorderLayout(0, 4));
        centro.setOpaque(false);
        centro.add(pct, BorderLayout.NORTH);
        centro.add(barra, BorderLayout.CENTER);

        JPanel este = new JPanel(new GridBagLayout()); // centra el botón en vertical
        este.setOpaque(false);
        if (m.estaCumplida()) {
            JLabel ok = new JLabel("¡Meta cumplida!");
            ok.setFont(Ui.F_NEGRITA);
            ok.setForeground(Ui.VERDE);
            este.add(ok);
        } else {
            JButton b = Ui.boton("Aportar", Ui.VERDE);
            b.addActionListener(ev -> aportar(m));
            este.add(b);
        }

        t.add(textos, BorderLayout.WEST);
        t.add(centro, BorderLayout.CENTER);
        t.add(este, BorderLayout.EAST);
        return t;
    }

    /* ---------- Actualizar lo que se ve en pantalla ---------- */
    // Se llama después de cada cambio: guarda en el archivo y vuelve a dibujar todo
    private void cambioRealizado() {
        if (!gestor.guardar()) {
            Ui.aviso(this, "No se pudo guardar el archivo finanzas.dat. Revisa los permisos de la carpeta.");
        }
        actualizarTodo();
    }

    private void actualizarTodo() {
        // Resumen
        lblIngresos.setText(Ui.dinero(gestor.getTotalIngresos(), moneda));
        lblGastos.setText(Ui.dinero(gestor.getTotalGastos(), moneda));
        BigDecimal balance = gestor.getBalance();
        lblBalance.setText(Ui.dinero(balance, moneda));
        lblBalance.setForeground(balance.signum() >= 0 ? Ui.AZUL : Ui.ROJO);
        lblDeuda.setText(Ui.dinero(gestor.getDeudaPendiente(), moneda));
        grafico.setDatos(gestor.getGastosPorCategoria());
        StringBuilder sb = new StringBuilder();
        for (String consejo : gestor.getConsejos()) {
            sb.append("•  ").append(consejo).append("\n\n");
        }
        txtConsejos.setText(sb.toString());

        // Movimientos: solo ingresos y gastos, el más nuevo primero
        filasMov.clear();
        modeloMov.setRowCount(0);
        List<ElementoFinanciero> todos = gestor.getElementos();
        for (int i = todos.size() - 1; i >= 0; i--) {
            ElementoFinanciero e = todos.get(i);
            if (e instanceof Ingreso || e instanceof Gasto) {
                filasMov.add(e);
                String signo = (e instanceof Ingreso) ? "+ " : "- ";
                modeloMov.addRow(new Object[] { e.getTipo(), e.getNombre(), signo + Ui.numero(e.getMonto()),
                        e.getFecha(), e.getDetalleEspecifico() });
            }
        }

        // Deudas
        filasDeuda.clear();
        modeloDeuda.setRowCount(0);
        LocalDate hoy = LocalDate.now();
        for (ElementoFinanciero e : todos) {
            if (e instanceof Deuda d) {
                String estado = d.getSaldoPendiente().signum() == 0 ? "Pagada"
                        : (d.estaVencida(hoy) ? "Vencida" : "Al día");
                filasDeuda.add(d);
                modeloDeuda.addRow(new Object[] { d.getNombre(), d.getAcreedor(),
                        Ui.numero(d.getMonto()), Ui.numero(d.getSaldoPendiente()),
                        d.getTasaInteresAnual() + "%", Ui.numero(d.getInteresMensualEstimado()),
                        d.getFechaVencimiento(), estado });
            }
        }

        // Metas
        listaMetas.removeAll();
        boolean hayMetas = false;
        for (ElementoFinanciero e : todos) {
            if (e instanceof MetaAhorro m) {
                listaMetas.add(crearTarjetaMeta(m));
                listaMetas.add(Box.createVerticalStrut(12));
                hayMetas = true;
            }
        }
        if (!hayMetas) {
            JLabel vacio = new JLabel("Aún no tienes metas. Crea la primera con el botón \"+ Nueva meta\".");
            vacio.setFont(Ui.F_NORMAL);
            vacio.setForeground(Ui.GRIS);
            listaMetas.add(vacio);
        }
        listaMetas.revalidate();
        listaMetas.repaint();
    }

    /* ---------- Utilidades de tablas ---------- */
    private static DefaultTableModel modeloNoEditable(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false; // la tabla es solo para mirar
            }
        };
    }

    private JComponent tablaEnTarjeta(JTable tabla, boolean colorearMonto, int... anchos) {
        tabla.setRowHeight(34);
        tabla.setShowGrid(false);
        tabla.setIntercellSpacing(new Dimension(0, 0));
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setSelectionBackground(new Color(0xE0E7FF));
        tabla.setSelectionForeground(Ui.TEXTO);
        tabla.setDefaultRenderer(Object.class, new CeldaTabla(colorearMonto));
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getTableHeader().setPreferredSize(new Dimension(100, 36));
        tabla.getTableHeader().setDefaultRenderer(new EncabezadoTabla());
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        Tarjeta t = new Tarjeta(new BorderLayout());
        t.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        t.add(scroll, BorderLayout.CENTER);
        return t;
    }

    // Cómo se dibuja cada celda: filas alternadas, montos a la derecha y colores
    // según el tipo
    private static class CeldaTabla extends DefaultTableCellRenderer {
        private final boolean colorearMonto;

        CeldaTabla(boolean colorearMonto) {
            this.colorearMonto = colorearMonto;
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object valor, boolean seleccionada,
                boolean foco, int fila, int col) {
            super.getTableCellRendererComponent(t, valor, seleccionada, false, fila, col);
            String columna = t.getColumnName(col);
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            setFont(Ui.F_NORMAL);
            boolean derecha = columna.contains("Monto") || columna.contains("Saldo") || columna.contains("Interés");
            setHorizontalAlignment(derecha ? SwingConstants.RIGHT : SwingConstants.LEFT);
            if (!seleccionada) {
                setBackground(fila % 2 == 0 ? Color.WHITE : new Color(0xF8F7FF));
                setForeground(Ui.TEXTO);
                if (colorearMonto && columna.equals("Monto")) {
                    boolean esIngreso = String.valueOf(t.getValueAt(fila, 0)).startsWith("INGRESO");
                    setForeground(esIngreso ? Ui.VERDE : Ui.ROJO);
                }
                if (columna.equals("Estado")) {
                    String estado = String.valueOf(valor);
                    if (estado.equals("Vencida"))
                        setForeground(Ui.ROJO);
                    if (estado.equals("Pagada"))
                        setForeground(Ui.VERDE);
                }
            }
            return this;
        }
    }

    private static class EncabezadoTabla extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object valor, boolean sel,
                boolean foco, int fila, int col) {
            super.getTableCellRendererComponent(t, valor, false, false, fila, col);
            setOpaque(true);
            setBackground(new Color(0xEEF0FF));
            setForeground(Ui.GRIS);
            setFont(Ui.F_NEGRITA);
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            String c = t.getColumnName(col);
            boolean derecha = c.contains("Monto") || c.contains("Saldo") || c.contains("Interés");
            setHorizontalAlignment(derecha ? SwingConstants.RIGHT : SwingConstants.LEFT);
            return this;
        }
    }
}
