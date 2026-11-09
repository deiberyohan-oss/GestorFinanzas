package vista;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;


// Gráfico de barras horizontales: cuánto se gastó en cada categoría
public class GraficoCategorias extends JComponent {
    private static final Color[] COLORES = { Ui.AZUL, Ui.NARANJA, Ui.VERDE, new Color(0x7C3AED),
            Ui.ROJO, new Color(0x0891B2) };
    private Map<String, BigDecimal> datos = new LinkedHashMap<>();

    public GraficoCategorias() {
        setPreferredSize(new Dimension(300, 220));
    }

    public void setDatos(Map<String, BigDecimal> nuevosDatos) {
        datos = nuevosDatos;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(Ui.F_NORMAL);
        FontMetrics fm = g2.getFontMetrics();

        if (datos.isEmpty()) {
            g2.setColor(Ui.GRIS);
            g2.drawString("Aún no hay gastos registrados.", 0, 20);
            g2.dispose();
            return;
        }

        BigDecimal maximo = BigDecimal.ZERO;
        for (BigDecimal v : datos.values()) {
            if (v.compareTo(maximo) > 0)
                maximo = v;
        }
        int anchoEtiqueta = 120;
        int anchoValor = 95;
        int altoFila = 34;
        int anchoMaxBarra = Math.max(20, getWidth() - anchoEtiqueta - anchoValor);

        int i = 0;
        for (Map.Entry<String, BigDecimal> e : datos.entrySet()) {
            if (i == 6)
                break; // solo las 6 categorías con más gasto
            int y = i * altoFila;
            g2.setColor(Ui.TEXTO);
            g2.drawString(recortar(e.getKey(), fm, anchoEtiqueta - 10), 0, y + 20);
            int ancho = (int) Math.max(6, anchoMaxBarra * e.getValue().doubleValue() / maximo.doubleValue());
            g2.setColor(COLORES[i % COLORES.length]);
            g2.fillRoundRect(anchoEtiqueta, y + 6, ancho, 16, 8, 8);
            g2.setColor(Ui.GRIS);
            g2.drawString(String.format("%,.0f", e.getValue()), anchoEtiqueta + ancho + 8, y + 20);
            i++;
        }
        g2.dispose();
    }

    private String recortar(String texto, FontMetrics fm, int max) {
        if (fm.stringWidth(texto) <= max)
            return texto;
        while (texto.length() > 1 && fm.stringWidth(texto + "...") > max) {
            texto = texto.substring(0, texto.length() - 1);
        }
        return texto + "...";
    }
}
