package kantinflow.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Locale;

import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;

/**
 * Label pill untuk status, jenis transaksi, atau kategori.
 */
public class Badge extends JLabel {
    private final Color warnaTeks;
    private final Color warnaLatar;

    public Badge(String teks, Color warnaTeks, Color warnaLatar) {
        super(teks == null ? "" : teks);
        this.warnaTeks = warnaTeks;
        this.warnaLatar = warnaLatar;
        setFont(Ui.font(java.awt.Font.BOLD, Ui.FONT_KECIL));
        setForeground(warnaTeks);
        setOpaque(false);
        setBorder(new EmptyBorder(Ui.skala(3), Ui.skala(8),
                Ui.skala(3), Ui.skala(8)));
    }

    public static Badge status(String teks) {
        Color[] warna = warnaUntuk(teks);
        return new Badge(teks, warna[0], warna[1]);
    }

    public static Color[] warnaUntuk(String teks) {
        String nilai = teks == null ? "" : teks.toLowerCase(Locale.ROOT);
        if (nilai.contains("kedaluwarsa") && !nilai.contains("segera")) {
            return new Color[]{Ui.MERAH_BADGE, Ui.MERAH_LEMBUT};
        }
        if (nilai.contains("segera") || nilai.contains("perlu restok")
                || nilai.contains("peringatan")) {
            return new Color[]{Ui.KUNING_BADGE, Ui.KUNING_LEMBUT};
        }
        if (nilai.contains("cukup") || nilai.contains("aman")
                || nilai.contains("tersedia") || nilai.contains("berhasil")
                || nilai.contains("selesai")) {
            return new Color[]{Ui.HIJAU_BADGE, Ui.HIJAU_LEMBUT};
        }
        if (nilai.contains("pembuangan") || nilai.contains("dibuang")) {
            return new Color[]{Ui.MERAH_BADGE, Ui.MERAH_LEMBUT};
        }
        if (nilai.contains("pengeluaran") || nilai.contains("keluar")
                || nilai.contains("manual") || nilai.contains("fefo")) {
            return new Color[]{Ui.ORANYE_BADGE, Ui.ORANYE_LEMBUT};
        }
        if (nilai.contains("penerimaan") || nilai.contains("masuk")) {
            return new Color[]{Ui.HIJAU_BADGE, Ui.HIJAU_LEMBUT};
        }
        return new Color[]{Ui.TEKS_LEMBUT, Ui.NETRAL_LEMBUT};
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension dasar = super.getPreferredSize();
        return new Dimension(dasar.width + 2, Math.max(dasar.height,
                Ui.skala(24)));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(warnaLatar);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(),
                    Ui.skala(999), Ui.skala(999));
        } finally {
            g2.dispose();
        }
        super.paintComponent(graphics);
    }

    @Override
    public void setText(String text) {
        super.setText(text);
        repaint();
    }

    public Color getWarnaTeks() {
        return warnaTeks;
    }

    static int lebarTeks(Graphics graphics, String teks) {
        FontMetrics metrics = graphics.getFontMetrics();
        return metrics.stringWidth(teks == null ? "" : teks);
    }
}
