package kantinflow.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

/**
 * Kartu statistik ringkas dengan ikon kecil, angka utama, dan satu keterangan.
 */
public class StatCard extends CardPanel {
    private final JLabel nilai = new JLabel("-");
    private final JLabel judul;
    private final JLabel keterangan;
    private final JLabel ikon;
    private final JPanel strip;
    private final String ikonNama;

    public StatCard(String judul, String ikonNama, Color aksen) {
        super(new BorderLayout(0, 7));
        this.ikonNama = ikonNama;
        setBorder(new EmptyBorder(Ui.skala(16), Ui.skala(16),
                Ui.skala(16), Ui.skala(16)));
        this.judul = new JLabel(judul);
        this.judul.setFont(Ui.font(Font.BOLD, Ui.FONT_LABEL));
        this.judul.setForeground(Ui.TEKS_LEMBUT);
        this.keterangan = new JLabel(" ");
        this.keterangan.setFont(Ui.font(Font.PLAIN, Ui.FONT_KECIL));
        this.keterangan.setForeground(Ui.TEKS_LEMBUT);
        this.nilai.setFont(Ui.font(Font.BOLD, 28f));
        this.nilai.setForeground(Ui.TEKS);
        this.ikon = new JLabel(Ui.ikonAksi(ikonNama, 18, aksen)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                Color warna = getForeground();
                g2.setColor(new Color(warna.getRed(), warna.getGreen(),
                        warna.getBlue(), 20));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(),
                        Ui.skala(10), Ui.skala(10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        this.ikon.setForeground(aksen);
        this.ikon.setHorizontalAlignment(SwingConstants.CENTER);
        this.ikon.setVerticalAlignment(SwingConstants.CENTER);
        this.ikon.setPreferredSize(new Dimension(Ui.skala(32), Ui.skala(32)));
        this.strip = new JPanel();
        this.strip.setOpaque(false);

        JPanel atas = new JPanel(new BorderLayout(8, 0));
        atas.setOpaque(false);
        atas.add(this.judul, BorderLayout.CENTER);
        atas.add(this.ikon, BorderLayout.EAST);

        JPanel bawah = new JPanel(new BorderLayout(0, 4));
        bawah.setOpaque(false);
        bawah.add(this.keterangan, BorderLayout.NORTH);
        bawah.add(this.strip, BorderLayout.SOUTH);
        add(atas, BorderLayout.NORTH);
        add(this.nilai, BorderLayout.CENTER);
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
            strip.setPreferredSize(new Dimension(0, 0));
            return;
        }
        Badge badge = Badge.status(teks);
        strip.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        strip.add(badge);
        strip.setPreferredSize(null);
    }

    public void setAksen(Color warna) {
        ikon.setIcon(Ui.ikonAksi(ikonNama, 18, warna));
        ikon.setForeground(warna);
        nilai.setForeground(warna.equals(Ui.TEKS_LEMBUT) ? Ui.TEKS : warna);
        ikon.repaint();
    }
}
