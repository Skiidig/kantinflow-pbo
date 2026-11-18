package kantinflow.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Empty state dengan ikon besar, judul, deskripsi, dan aksi opsional.
 */
public class EmptyState extends JPanel {
    private final JLabel judul = new JLabel();
    private final JLabel deskripsi = new JLabel();
    private final JPanel aksi = new JPanel();

    public EmptyState(String ikonNama, String judul, String deskripsi) {
        super(new GridBagLayout());
        setOpaque(false);
        setBorder(new javax.swing.border.EmptyBorder(
                Ui.skala(18), Ui.skala(18), Ui.skala(18), Ui.skala(18)));
        setMinimumSize(new Dimension(Ui.skala(200), Ui.skala(112)));
        setPreferredSize(new Dimension(Ui.skala(420), Ui.skala(124)));
        JPanel isi = new JPanel();
        isi.setOpaque(false);
        isi.setLayout(new BoxLayout(isi, BoxLayout.Y_AXIS));
        JPanel lingkaran = new LingkaranIkOn(
                Ui.ikonAksi(ikonNama, 26, Ui.TEKS_LEMBUT));
        lingkaran.setAlignmentX(CENTER_ALIGNMENT);
        this.judul.setText(judul);
        this.judul.setFont(Ui.font(Font.BOLD, Ui.FONT_KARTU));
        this.judul.setForeground(Ui.TEKS);
        this.judul.setHorizontalAlignment(SwingConstants.CENTER);
        this.judul.setAlignmentX(CENTER_ALIGNMENT);
        this.deskripsi.setText(deskripsi);
        this.deskripsi.setFont(Ui.font(Font.PLAIN, Ui.FONT_CATATAN));
        this.deskripsi.setForeground(Ui.TEKS_LEMBUT);
        this.deskripsi.setHorizontalAlignment(SwingConstants.CENTER);
        this.deskripsi.setAlignmentX(CENTER_ALIGNMENT);
        this.aksi.setOpaque(false);
        this.aksi.setLayout(new BoxLayout(aksi, BoxLayout.X_AXIS));
        this.aksi.setAlignmentX(CENTER_ALIGNMENT);
        isi.add(lingkaran);
        isi.add(Box.createVerticalStrut(Ui.skala(10)));
        isi.add(this.judul);
        isi.add(Box.createVerticalStrut(Ui.skala(3)));
        isi.add(this.deskripsi);
        isi.add(Box.createVerticalStrut(Ui.skala(8)));
        isi.add(this.aksi);
        add(isi);
    }

    public EmptyState setAction(JButton tombol) {
        aksi.removeAll();
        if (tombol != null) {
            tombol.setAlignmentX(CENTER_ALIGNMENT);
            aksi.add(tombol);
        }
        return this;
    }

    public EmptyState setDescription(String teks) {
        deskripsi.setText(teks);
        return this;
    }

    public String getText() {
        return judul.getText() + " " + deskripsi.getText();
    }

    public EmptyState setTitle(String teks) {
        judul.setText(teks);
        return this;
    }

    private static final class LingkaranIkOn extends JPanel {
        private final javax.swing.Icon ikon;

        private LingkaranIkOn(javax.swing.Icon ikon) {
            this.ikon = ikon;
            setOpaque(false);
            setPreferredSize(new Dimension(Ui.skala(64), Ui.skala(64)));
            setMinimumSize(getPreferredSize());
            setMaximumSize(getPreferredSize());
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Ui.NETRAL_LEMBUT);
                g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
            } finally {
                g2.dispose();
            }
            int x = (getWidth() - ikon.getIconWidth()) / 2;
            int y = (getHeight() - ikon.getIconHeight()) / 2;
            ikon.paintIcon(this, graphics, x, y);
            super.paintComponent(graphics);
        }
    }
}
