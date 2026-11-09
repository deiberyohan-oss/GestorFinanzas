package vista;

import javax.swing.*;
import java.awt.*;


// Panel blanco con esquinas redondeadas y borde suave
public class Tarjeta extends JPanel {
    public Tarjeta(LayoutManager layout) {
        super(layout);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int ancho = getWidth() - 4;
        int alto = getHeight() - 5;
        g2.setColor(new Color(60, 40, 140, 22)); // sombra suave debajo de la tarjeta
        g2.fillRoundRect(1, 4, ancho, alto, 20, 20);
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(0, 0, ancho, alto, 20, 20);
        g2.setColor(Ui.BORDE);
        g2.drawRoundRect(0, 0, ancho, alto, 20, 20);
        g2.dispose();
        super.paintComponent(g);
    }
}
