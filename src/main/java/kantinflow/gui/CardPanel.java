package kantinflow.gui;

import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * Kartu putih dengan sudut membulat, border tipis, dan padding 20 px.
 */
public class CardPanel extends JPanel {
    public static final int ARC = 16;
    private final int arc;

    public CardPanel(LayoutManager layout) {
        this(layout, ARC);
    }

    public CardPanel(LayoutManager layout, int arc) {
        super(layout);
        this.arc = arc;
        setOpaque(false);
        setBorder(new EmptyBorder(Ui.skala(20), Ui.skala(20),
                Ui.skala(20), Ui.skala(20)));
    }

    public int getArc() {
        return arc;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Ui.KARTU);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                    arc, arc);
            g2.setColor(Ui.GARIS);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                    arc, arc);
        } finally {
            g2.dispose();
        }
        super.paintComponent(graphics);
    }
}
