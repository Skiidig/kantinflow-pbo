package kantinflow.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

/**
 * Kartu statistik dengan ikon berwarna, angka besar, label, dan strip status.
 */
public class StatCard extends CardPanel {
    private final JLabel nilai = new JLabel("-");
    private final JLabel judul;
    private final JLabel keterangan;
    private final JPanel strip;
    private final IconCircle ikon;

    public StatCard(String judul, String ikonNama, Color aksen) {
        super(new BorderLayout(0, 10));
        setBorder(new EmptyBorder(Ui.skala(16), Ui.skala(18),
                Ui.skala(14), Ui.skala(18)));
        this.judul = new JLabel(judul);
        this.judul.setFont(Ui.font(Font.BOLD, Ui.FONT_LABEL));
        this.judul.setForeground(Ui.TEKS_LEMBUT);
        this.keterangan = new JLabel(" ");
        this.keterangan.setFont(Ui.font(Font.PLAIN, Ui.FONT_KECIL));
        this.keterangan.setForeground(Ui.TEKS_LEMBUT);
        this.nilai.setFont(Ui.font(Font.BOLD, 30f));
        this.nilai.setForeground(Ui.TEKS);
        this.ikon = new IconCircle(Ui.ikonAksi(ikonNama, 20, Color.WHITE),
                aksen);
        this.strip = new JPanel();
        this.strip.setOpaque(false);

        JPanel atas = new JPanel(new BorderLayout(10, 0));
        atas.setOpaque(false);
        JPanel teks = new JPanel();
        teks.setOpaque(false);
        teks.setLayout(new BoxLayout(teks, BoxLayout.Y_AXIS));
        teks.add(this.judul);
        teks.add(Box.createVerticalStrut(6));
        teks.add(this.nilai);
        atas.add(teks, BorderLayout.CENTER);
        JPanel kanan = new JPanel(new java.awt.FlowLayout(
                java.awt.FlowLayout.RIGHT, 0, 0));
        kanan.setOpaque(false);
        kanan.add(this.ikon);
        kanan.setPreferredSize(new Dimension(Ui.skala(48), Ui.skala(48)));
        kanan.setMaximumSize(new Dimension(Ui.skala(48), Ui.skala(48)));
        atas.add(kanan, BorderLayout.EAST);

        JPanel bawah = new JPanel(new BorderLayout(0, 4));
        bawah.setOpaque(false);
        bawah.add(this.keterangan, BorderLayout.NORTH);
        bawah.add(this.strip, BorderLayout.SOUTH);
        add(atas, BorderLayout.CENTER);
        add(bawah, BorderLayout.SOUTH);
    }

    public JLabel getNilai() {
        return nilai;
    }

    public JLabel getKeterangan() {
        return keterangan;
    }

    public void setNilai(String teks) {
        nilai.setText(teks);
    }

    public void setKeterangan(String teks) {
        keterangan.setText(teks == null || teks.isBlank() ? " " : teks);
    }

    public void setStatus(String teks) {
        strip.removeAll();
        if (teks == null || teks.isBlank()) {
            strip.setPreferredSize(new Dimension(0, Ui.skala(6)));
            return;
        }
        Badge badge = Badge.status(teks);
        strip.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        strip.add(badge);
        strip.setPreferredSize(null);
    }

    public void setAksen(Color warna) {
        ikon.setWarna(warna);
    }

    private static final class IconCircle extends JPanel {
        private final javax.swing.Icon ikon;
        private Color warna;

        private IconCircle(javax.swing.Icon ikon, Color warna) {
            this.ikon = ikon;
            this.warna = warna;
            setOpaque(false);
            setPreferredSize(new Dimension(Ui.skala(48), Ui.skala(48)));
            setMinimumSize(getPreferredSize());
        }

        void setWarna(Color warna) {
            this.warna = warna;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(warna);
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
