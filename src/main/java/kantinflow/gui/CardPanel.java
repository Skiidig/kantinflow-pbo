package kantinflow.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * Kartu putih dengan sudut membulat seragam, border tipis, dan padding 20 px.
 */
public class CardPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    public static final int ARC = Ui.RADIUS_KARTU;
    private final int arc;

    public CardPanel(LayoutManager layout) {
        this(layout, ARC);
    }

    public CardPanel(LayoutManager layout, int arc) {
        super(layout);
        this.arc = arc;
        setOpaque(false);
        setBorder(new EmptyBorder(
                Ui.skala(Ui.PADDING_KARTU.top),
                Ui.skala(Ui.PADDING_KARTU.left),
                Ui.skala(Ui.PADDING_KARTU.bottom),
                Ui.skala(Ui.PADDING_KARTU.right)));
    }

    public int getArc() {
        return arc;
    }

    protected Color warnaLatarKartu() {
        return Ui.KARTU;
    }

    protected Color warnaGarisKartu() {
        return Ui.GARIS;
    }

    protected float tebalGarisKartu() {
        return 1f;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            int busur = Ui.skala(arc);
            float tebal = tebalGarisKartu();
            int offset = tebal > 1.2f ? 1 : 0;
            int w = Math.max(0, getWidth() - 1 - offset * 2);
            int h = Math.max(0, getHeight() - 1 - offset * 2);
            g2.setColor(warnaLatarKartu());
            g2.fillRoundRect(offset, offset, w, h, busur, busur);
            g2.setColor(warnaGarisKartu());
            g2.setStroke(new BasicStroke(tebal));
            g2.drawRoundRect(offset, offset, w, h, busur, busur);
        } finally {
            g2.dispose();
        }
        super.paintComponent(graphics);
    }
}
