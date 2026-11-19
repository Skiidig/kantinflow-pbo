package kantinflow.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.Box;
import javax.swing.BoxLayout;
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
        setBorder(new EmptyBorder(Ui.skala(12), Ui.skala(14),
                Ui.skala(12), Ui.skala(14)));
        this.judul = new JLabel(judul);
        this.judul.setFont(Ui.font(Font.BOLD, Ui.FONT_LABEL));
        this.judul.setForeground(Ui.TEKS_LEMBUT);
        this.keterangan = new JLabel(" ");
        this.keterangan.setFont(Ui.font(Font.PLAIN, Ui.FONT_KECIL));
        this.keterangan.setForeground(Ui.TEKS_LEMBUT);
        this.nilai.setFont(Ui.font(Font.BOLD, 26f));
        this.nilai.setForeground(Ui.TEKS);
        this.ikon = new JLabel(Ui.ikonAksi(ikonNama, 16, aksen));
        this.ikon.setVerticalAlignment(SwingConstants.TOP);
        this.strip = new JPanel();
        this.strip.setOpaque(false);

        JPanel atas = new JPanel(new BorderLayout(8, 0));
        atas.setOpaque(false);
        JPanel teks = new JPanel();
        teks.setOpaque(false);
        teks.setLayout(new BoxLayout(teks, BoxLayout.Y_AXIS));
        teks.add(this.judul);
        teks.add(Box.createVerticalStrut(4));
        teks.add(this.nilai);
        atas.add(teks, BorderLayout.CENTER);
        JPanel kanan = new JPanel(new FlowLayout(
                FlowLayout.RIGHT, 0, 0));
        kanan.setOpaque(false);
        kanan.add(this.ikon);
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
            strip.setPreferredSize(new Dimension(0, 0));
            return;
        }
        Badge badge = Badge.status(teks);
        strip.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        strip.add(badge);
        strip.setPreferredSize(null);
    }

    public void setAksen(Color warna) {
        ikon.setIcon(Ui.ikonAksi(ikonNama, 16, warna));
    }
}
