package vista;

import javax.swing.*;
import java.awt.*;


// Barra de progreso dibujada a mano (así se ve igual en cualquier computador)
public class BarraProgreso extends JComponent {
    private double porcentaje = 0;
    private final Color color;

    public BarraProgreso(Color color) {
        this.color = color;
        setPreferredSize(new Dimension(100, 20));
    }

    public void setPorcentaje(double p) {
        porcentaje = Math.max(0, Math.min(100, p));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int alto = 12;
        int y = (getHeight() - alto) / 2;
        g2.setColor(Ui.BORDE);
        g2.fillRoundRect(0, y, getWidth(), alto, alto, alto);
        int relleno = (int) (getWidth() * porcentaje / 100.0);
        if (relleno > 0) {
            g2.setColor(color);
            g2.fillRoundRect(0, y, Math.max(relleno, alto), alto, alto, alto);
        }
        g2.dispose();
    }
}
