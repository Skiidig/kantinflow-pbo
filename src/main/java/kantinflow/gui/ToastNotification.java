package kantinflow.gui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

/**
 * Komponen Toast ringan non-blocking yang tampil di pojok kanan bawah jendela
 * pada {@link JLayeredPane#POPUP_LAYER} dengan animasi fade-in, durasi tampil
 * 3 detik, dan fade-out otomatis tanpa mengambil fokus pengguna.
 */
public final class ToastNotification {
    public enum Tipe {
        SUKSES("Berhasil", "circle-check", Ui.HIJAU, Ui.HIJAU_LEMBUT),
        GAGAL("Gagal", "alert-triangle", Ui.MERAH, Ui.MERAH_LEMBUT),
        PERINGATAN("Peringatan", "alert-triangle", Ui.KUNING, Ui.KUNING_LEMBUT),
        INFO("Informasi", "circle-check", Ui.BIRU, Ui.BIRU_LEMBUT);

        private final String judul;
        private final String ikon;
        private final Color aksen;
        private final Color latarIkon;

        Tipe(String judul, String ikon, Color aksen, Color latarIkon) {
            this.judul = judul;
            this.ikon = ikon;
            this.aksen = aksen;
            this.latarIkon = latarIkon;
        }
    }

    private static final int DURASI_TAMPIL_MS = 3000;
    private static final int INTERVAL_ANIMASI_MS = 16;
    private static final float LANGKAH_MASUK = 0.10f;
    private static final float LANGKAH_KELUAR = 0.08f;
    private static final int MAKS_TOAST = 3;

    private static JFrame frameAktif;
    private static final List<KartuToast> aktif = new ArrayList<>();
    private static final ComponentAdapter pendengarResize = new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent event) {
            tataUlang();
        }
    };

    private ToastNotification() {
    }

    public static void pasang(JFrame jendela) {
        if (frameAktif != null && frameAktif.getLayeredPane() != null) {
            frameAktif.getLayeredPane().removeComponentListener(pendengarResize);
        }
        bersihkan();
        frameAktif = jendela;
        if (jendela != null && jendela.getLayeredPane() != null) {
            jendela.getLayeredPane().addComponentListener(pendengarResize);
        }
    }

    public static void tampilkan(Tipe tipe, String pesan) {
        if (frameAktif == null || pesan == null || pesan.isBlank()) {
            return;
        }
        Runnable aksi = () -> tampilkanDiEdt(
                tipe == null ? Tipe.INFO : tipe, pesan.trim());
        if (SwingUtilities.isEventDispatchThread()) {
            aksi.run();
        } else {
            SwingUtilities.invokeLater(aksi);
        }
    }

    public static void bersihkan() {
        Runnable aksi = () -> {
            List<KartuToast> salinan = new ArrayList<>(aktif);
            for (KartuToast toast : salinan) {
                toast.hapusSegera();
            }
            aktif.clear();
        };
        if (SwingUtilities.isEventDispatchThread()) {
            aksi.run();
        } else {
            SwingUtilities.invokeLater(aksi);
        }
    }

    private static void tampilkanDiEdt(Tipe tipe, String pesan) {
        if (frameAktif == null || frameAktif.getLayeredPane() == null) {
            return;
        }
        JLayeredPane lapisan = frameAktif.getLayeredPane();
        while (aktif.size() >= MAKS_TOAST) {
            aktif.get(0).hapusSegera();
        }
        KartuToast kartu = new KartuToast(tipe, pesan);
        aktif.add(kartu);
        lapisan.add(kartu, JLayeredPane.POPUP_LAYER);
        tataUlang();
        kartu.mulai();
    }

    private static void tataUlang() {
        if (frameAktif == null || frameAktif.getLayeredPane() == null) {
            return;
        }
        JLayeredPane lapisan = frameAktif.getLayeredPane();
        int lebarArea = lapisan.getWidth();
        int tinggiArea = lapisan.getHeight();
        if (lebarArea <= 0 || tinggiArea <= 0) {
            return;
        }
        int margin = Ui.skala(20);
        int jarak = Ui.skala(10);
        int lebarMaks = Math.min(Ui.skala(380), Math.max(Ui.skala(240),
                lebarArea - margin * 2));
        int bawah = tinggiArea - margin;
        for (int i = aktif.size() - 1; i >= 0; i--) {
            KartuToast toast = aktif.get(i);
            toast.aturLebar(lebarMaks);
            Dimension ukuran = toast.getPreferredSize();
            int w = Math.min(lebarMaks, Math.max(Ui.skala(260), ukuran.width));
            int h = ukuran.height;
            int x = Math.max(margin, lebarArea - margin - w);
            int y = Math.max(margin, bawah - h);
            toast.setBounds(x, y, w, h);
            toast.revalidate();
            bawah = y - jarak;
        }
        lapisan.repaint();
    }

    private static final class KartuToast extends JPanel {
        private static final long serialVersionUID = 1L;
        private final Tipe tipe;
        private final String pesanAsli;
        private final JLabel teksPesan;
        private float alpha = 0f;
        private boolean keluar;
        private boolean sudahDihapus;
        private Timer timerAnimasi;
        private Timer timerTahan;

        private KartuToast(Tipe tipe, String pesan) {
            super(new BorderLayout(Ui.skala(10), 0));
            this.tipe = tipe;
            this.pesanAsli = pesan;
            setOpaque(false);
            setFocusable(false);
            setBorder(new EmptyBorder(
                    Ui.skala(12), Ui.skala(16), Ui.skala(14), Ui.skala(12)));

            JLabel ikon = buatIkon(tipe);
            JPanel wadahIkon = new JPanel(new BorderLayout());
            wadahIkon.setOpaque(false);
            wadahIkon.add(ikon, BorderLayout.NORTH);
            add(wadahIkon, BorderLayout.WEST);

            JPanel tengah = new JPanel();
            tengah.setOpaque(false);
            tengah.setLayout(new BoxLayout(tengah, BoxLayout.Y_AXIS));
            JLabel labelJudul = new JLabel(tipe.judul);
            labelJudul.setFont(Ui.fontSemiBold(Ui.FONT_CATATAN));
            labelJudul.setForeground(Ui.TEKS);
            labelJudul.setAlignmentX(LEFT_ALIGNMENT);
            teksPesan = new JLabel(formatHtml(pesanAsli, Ui.skala(270)));
            teksPesan.setFont(Ui.font(Font.PLAIN, Ui.FONT_KECIL));
            teksPesan.setForeground(Ui.TEKS_LEMBUT);
            teksPesan.setAlignmentX(LEFT_ALIGNMENT);
            tengah.add(labelJudul);
            tengah.add(Box.createVerticalStrut(Ui.skala(2)));
            tengah.add(teksPesan);
            add(tengah, BorderLayout.CENTER);

            JButton tutup = buatTombolTutup();
            JPanel wadahTutup = new JPanel(new BorderLayout());
            wadahTutup.setOpaque(false);
            wadahTutup.add(tutup, BorderLayout.NORTH);
            add(wadahTutup, BorderLayout.EAST);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent event) {
                    if (timerTahan != null && timerTahan.isRunning()) {
                        timerTahan.stop();
                    }
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    if (!keluar && !sudahDihapus && timerTahan != null) {
                        timerTahan.restart();
                    }
                }
            });
        }

        private void aturLebar(int lebarKartu) {
            int lebarTeks = Math.max(Ui.skala(160), lebarKartu - Ui.skala(108));
            teksPesan.setText(formatHtml(pesanAsli, lebarTeks));
        }

        private static String formatHtml(String pesan, int lebarPx) {
            String aman = pesan.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
            return "<html><body style='width:" + lebarPx + "px'>"
                    + aman + "</body></html>";
        }

        private JLabel buatIkon(Tipe tipe) {
            Icon svg = Ui.ikonAksi(tipe.ikon, 16, tipe.aksen);
            JLabel label = new JLabel(svg) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    try {
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(tipe.latarIkon);
                        g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                    } finally {
                        g2.dispose();
                    }
                    super.paintComponent(g);
                }
            };
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setVerticalAlignment(SwingConstants.CENTER);
            Dimension ukuran = new Dimension(Ui.skala(28), Ui.skala(28));
            label.setPreferredSize(ukuran);
            label.setMinimumSize(ukuran);
            label.setMaximumSize(ukuran);
            return label;
        }

        private JButton buatTombolTutup() {
            JButton tombol = new JButton();
            Icon ikonTutup = Ui.ikonAksi("close", 14, Ui.TEKS_LEMBUT);
            if (ikonTutup != null) {
                tombol.setIcon(ikonTutup);
            } else {
                tombol.setText("\u00D7");
            }
            tombol.setFocusable(false);
            tombol.setContentAreaFilled(false);
            tombol.setBorderPainted(false);
            tombol.setOpaque(false);
            tombol.setBorder(new EmptyBorder(Ui.skala(2), Ui.skala(2),
                    Ui.skala(2), Ui.skala(2)));
            tombol.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            tombol.setToolTipText("Tutup");
            tombol.getAccessibleContext().setAccessibleName("Tutup notifikasi");
            tombol.addActionListener(event -> mulaiKeluar());
            return tombol;
        }

        private void mulai() {
            timerAnimasi = new Timer(INTERVAL_ANIMASI_MS, event -> {
                if (!keluar) {
                    alpha = Math.min(1f, alpha + LANGKAH_MASUK);
                    if (alpha >= 1f) {
                        timerAnimasi.stop();
                        mulaiTahan();
                    }
                } else {
                    alpha = Math.max(0f, alpha - LANGKAH_KELUAR);
                    if (alpha <= 0f) {
                        hapusSegera();
                        return;
                    }
                }
                repaint();
            });
            timerAnimasi.start();
        }

        private void mulaiTahan() {
            if (sudahDihapus) {
                return;
            }
            timerTahan = new Timer(DURASI_TAMPIL_MS, event -> mulaiKeluar());
            timerTahan.setRepeats(false);
            timerTahan.start();
        }

        private void mulaiKeluar() {
            if (keluar || sudahDihapus) {
                return;
            }
            keluar = true;
            if (timerTahan != null) {
                timerTahan.stop();
            }
            if (timerAnimasi != null && !timerAnimasi.isRunning()) {
                timerAnimasi.start();
            }
        }

        private void hapusSegera() {
            if (sudahDihapus) {
                return;
            }
            sudahDihapus = true;
            if (timerAnimasi != null) {
                timerAnimasi.stop();
            }
            if (timerTahan != null) {
                timerTahan.stop();
            }
            aktif.remove(this);
            if (getParent() != null) {
                java.awt.Container induk = getParent();
                induk.remove(this);
                induk.revalidate();
                induk.repaint();
            }
            tataUlang();
        }

        @Override
        public void paint(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                g2.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, alpha))));
                super.paint(g2);
            } finally {
                g2.dispose();
            }
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                int bayangan = Ui.skala(3);
                int busur = Ui.skala(Ui.RADIUS_KONTROL + 2);
                int w = Math.max(0, getWidth() - bayangan);
                int h = Math.max(0, getHeight() - bayangan);

                g2.setColor(new Color(15, 23, 42, 24));
                g2.fillRoundRect(Ui.skala(1), Ui.skala(2), w, h, busur, busur);

                g2.setColor(Ui.KARTU);
                g2.fillRoundRect(0, 0, w - 1, h - 1, busur, busur);

                g2.setColor(tipe.aksen);
                g2.fillRoundRect(Ui.skala(4), Ui.skala(8),
                        Ui.skala(3), Math.max(Ui.skala(16), h - Ui.skala(16)),
                        Ui.skala(3), Ui.skala(3));

                g2.setColor(Ui.GARIS);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, busur, busur);
            } finally {
                g2.dispose();
            }
            super.paintComponent(graphics);
        }
    }
}
