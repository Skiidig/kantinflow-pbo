import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import kantinflow.Barang;
import kantinflow.gui.JendelaKantinFlow;
import kantinflow.gui.Tema;
import kantinflow.penyimpanan.PenyimpananSQLite;

/**
 * Pemeriksaan lebar konten, form, tombol, tabel, dan gulir.
 * Jalankan setelah src/main/java dan tests lain dikompilasi.
 */
public class LayoutUxHarness {
    private static final String[] HALAMAN = {
        "ringkasan", "barang", "stok-masuk", "stok-keluar",
        "pemantauan", "pembuangan", "riwayat"
    };
    private static JFrame frame;
    private static JendelaKantinFlow aplikasi;
    private static int gagal;

    public static void main(String[] args) throws Exception {
        Path database = Path.of("data/layout-ux.db");
        Path tema = Path.of("data", "kantinflow-tema.properties");
        byte[] temaAwal = Files.isRegularFile(tema)
                ? Files.readAllBytes(tema) : null;
        Files.deleteIfExists(database);
        Files.deleteIfExists(database.resolveSibling(
                database.getFileName() + ".ui"));
        SwingRiwayatHarness.seed(database.toString());

        Tema.terapkan(false);
        SwingUtilities.invokeAndWait(() -> {
            try {
                aplikasi = new JendelaKantinFlow(
                        new PenyimpananSQLite(database.toString()));
                frame = aplikasi;
                frame.setSize(1366, 768);
                frame.setLocationRelativeTo(null);
                aplikasi.tampilkan();
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
        Thread.sleep(600);
        pilihBarang("Roti");

        ujiUkuran(1366, 768, "1366", true);
        ujiUkuran(960, 640, "960", true);
        ujiLebar2560();

        SwingUtilities.invokeAndWait(() -> aplikasi.tutup());
        Tema.terapkan(false);
        if (temaAwal == null) {
            Files.deleteIfExists(tema);
        } else {
            Files.write(tema, temaAwal);
        }
        Files.deleteIfExists(database);
        Files.deleteIfExists(database.resolveSibling(
                database.getFileName() + ".ui"));
        System.out.println(gagal == 0
                ? "LAYOUT_UX_HARNESS_LULUS"
                : "LAYOUT_UX_HARNESS_GAGAL=" + gagal);
        System.exit(gagal == 0 ? 0 : 1);
    }

    private static void ujiUkuran(int lebar, int tinggi, String label,
            boolean ambilScreenshot) throws Exception {
        aturTema(false);
        SwingUtilities.invokeAndWait(() -> frame.setSize(lebar, tinggi));
        Thread.sleep(450);
        for (String halaman : HALAMAN) {
            pilihHalaman(halaman);
            periksaHalaman(halaman, label);
            if (ambilScreenshot) {
                snap("ux-" + halaman + "-" + label + "-terang.png");
            }
        }
        aturTema(true);
        for (String halaman : HALAMAN) {
            pilihHalaman(halaman);
            periksaHalaman(halaman, label);
            if (ambilScreenshot) {
                snap("ux-" + halaman + "-" + label + "-gelap.png");
            }
        }
    }

    private static void ujiLebar2560() throws Exception {
        aturTema(true);
        SwingUtilities.invokeAndWait(() -> frame.setSize(2560, 1000));
        Thread.sleep(550);
        for (String halaman : HALAMAN) {
            pilihHalaman(halaman);
            periksaHalaman(halaman, "2560");
            if ("ringkasan".equals(halaman) || "stok-masuk".equals(halaman)
                    || "riwayat".equals(halaman)) {
                snap("ux-" + halaman + "-2560-gelap.png");
            }
        }
    }

    private static void aturTema(boolean mauGelap) throws Exception {
        if (Tema.gelap() == mauGelap) {
            return;
        }
        SwingUtilities.invokeAndWait(() -> ((JButton) find(
                frame.getContentPane(), "tombolTema")).doClick());
        Thread.sleep(850);
    }

    private static void periksaHalaman(String halaman, String ukuran) {
        JScrollPane gulir = halamanScroll();
        check("GULIR_" + halaman + "_" + ukuran, gulir != null,
                "scroll=" + (gulir != null));
        if (gulir == null) {
            return;
        }
        check("TANPA_GULIR_HORIZONTAL_" + halaman + "_" + ukuran,
                !gulir.getHorizontalScrollBar().isVisible(),
                "visible=" + gulir.getHorizontalScrollBar().isVisible());
        JScrollBar horizontal = gulir.getHorizontalScrollBar();
        check("TANPA_OVERFLOW_HALAMAN_" + halaman + "_" + ukuran,
                !horizontal.isVisible()
                        && horizontal.getValue() == horizontal.getMinimum(),
                "value=" + horizontal.getValue());

        Container halamanPanel = panelKantin(gulir.getViewport().getView());
        check("PANEL_" + halaman + "_" + ukuran, halamanPanel != null,
                "panel=" + (halamanPanel != null));
        if (halamanPanel == null) {
            return;
        }
        int maks = "ringkasan".equals(halaman) ? 1400 : 1200;
        check("LEBAR_MAKS_" + halaman + "_" + ukuran,
                halamanPanel.getWidth() <= maks,
                "width=" + halamanPanel.getWidth() + " max=" + maks);
        check("MULAI_KIRI_" + halaman + "_" + ukuran,
                halamanPanel.getX() == 0,
                "x=" + halamanPanel.getX());
        check("PADDING_KIRI_" + halaman + "_" + ukuran,
                halamanPanel.getInsets().left >= 20,
                "inset=" + halamanPanel.getInsets().left);

        for (JScrollPane nested : semuaGulir(halamanPanel)) {
            check("NESTED_TANPA_HORIZONTAL_" + halaman + "_" + ukuran,
                    !nested.getHorizontalScrollBar().isVisible(),
                    "visible=" + nested.getHorizontalScrollBar().isVisible());
        }

        if ("stok-masuk".equals(halaman)) {
            cekInput("kodeBatch", 460, ukuran);
            cekInput("jumlah", 460, ukuran);
            cekInput("tanggalMasuk", 460, ukuran);
            cekInput("tanggalKedaluwarsa", 460, ukuran);
            cekTombol("tombolSimpan", ukuran);
        }
        if ("pemantauan".equals(halaman)) {
            cekInput("tanggalPemantauan", 460, ukuran);
            cekTombol("tombolPeriksa", ukuran);
            JTable tabel = (JTable) find(frame.getContentPane(),
                    "tabelPemantauan");
            check("TABEL_PEMANTAUAN_MUAT_" + ukuran,
                    tabel != null && tabel.getRowCount() >= 0,
                    "rows=" + (tabel == null ? -1 : tabel.getRowCount()));
        }
        if ("riwayat".equals(halaman)) {
            cekInput("cariRiwayat", 460, ukuran);
            cekInput("tanggalAwalRiwayat", 460, ukuran);
            cekInput("tanggalAkhirRiwayat", 460, ukuran);
            cekTombol("eksporRiwayat", ukuran);
            cekTombol("eksporPdf", ukuran);
            cekTombol("detailRiwayat", ukuran);
        }
        if ("pembuangan".equals(halaman)) {
            cekTombol("tombolBuang", ukuran);
        }
        if ("barang".equals(halaman)) {
            cekTombol("tombolTambahBarang", ukuran);
        }
        if ("stok-keluar".equals(halaman)) {
            cekTombol("tombolKeluarkan", ukuran);
        }
    }

    private static void cekInput(String nama, int maks, String ukuran) {
        Component komponen = find(frame.getContentPane(), nama);
        check("INPUT_" + nama + "_" + ukuran,
                komponen != null && komponen.getWidth() <= maks,
                komponen == null ? "tidak ada"
                        : "width=" + komponen.getWidth());
    }

    private static void cekTombol(String nama, String ukuran) {
        Component komponen = find(frame.getContentPane(), nama);
        check("TOMBOL_" + nama + "_" + ukuran,
                komponen != null && komponen.getWidth()
                        >= komponen.getPreferredSize().width,
                komponen == null ? "tidak ada"
                        : "width=" + komponen.getWidth() + " pref="
                                + komponen.getPreferredSize().width);
    }

    private static void pilihHalaman(String nama) throws Exception {
        SwingUtilities.invokeAndWait(() -> ((AbstractButton) find(
                frame.getContentPane(), "nav-" + nama)).doClick());
        Thread.sleep(260);
    }

    private static void pilihBarang(String nama) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            @SuppressWarnings("unchecked")
            JComboBox<Barang> combo = (JComboBox<Barang>) find(
                    frame.getContentPane(), "barangAktif");
            for (int i = 0; i < combo.getItemCount(); i++) {
                if (nama.equals(combo.getItemAt(i).getNama())) {
                    combo.setSelectedIndex(i);
                    return;
                }
            }
            throw new IllegalStateException("Barang tidak ditemukan: " + nama);
        });
        Thread.sleep(350);
    }

    private static JScrollPane halamanScroll() {
        return cariHalaman(frame.getContentPane());
    }

    private static JScrollPane cariHalaman(Container root) {
        for (Component component : root.getComponents()) {
            if (component instanceof JScrollPane scroll
                    && scroll.isShowing()
                    && panelKantin(scroll.getViewport().getView()) != null) {
                return scroll;
            }
            if (component instanceof Container container) {
                JScrollPane hasil = cariHalaman(container);
                if (hasil != null) {
                    return hasil;
                }
            }
        }
        return null;
    }

    private static Container panelKantin(Component component) {
        if (component == null) {
            return null;
        }
        if (component.getClass().getName().startsWith(
                "kantinflow.gui.Panel")
                && component instanceof Container containerPanel) {
            return containerPanel;
        }
        if (component instanceof Container container) {
            for (Component anak : container.getComponents()) {
                Container hasil = panelKantin(anak);
                if (hasil != null) {
                    return hasil;
                }
            }
        }
        return null;
    }

    private static java.util.List<JScrollPane> semuaGulir(Container root) {
        java.util.List<JScrollPane> hasil = new java.util.ArrayList<>();
        for (Component component : root.getComponents()) {
            if (component instanceof JScrollPane scroll) {
                hasil.add(scroll);
            }
            if (component instanceof Container container) {
                hasil.addAll(semuaGulir(container));
            }
        }
        return hasil;
    }

    private static Component find(Container root, String nama) {
        for (Component component : root.getComponents()) {
            if (nama.equals(component.getName())) {
                return component;
            }
            if (component instanceof Container container) {
                Component hasil = find(container, nama);
                if (hasil != null) {
                    return hasil;
                }
            }
        }
        return null;
    }

    private static void snap(String nama) throws Exception {
        Container isi = frame.getContentPane();
        BufferedImage gambar = new BufferedImage(
                Math.max(1, isi.getWidth()), Math.max(1, isi.getHeight()),
                BufferedImage.TYPE_INT_ARGB);
        SwingUtilities.invokeAndWait(() -> {
            java.awt.Graphics2D g2 = gambar.createGraphics();
            isi.paint(g2);
            g2.dispose();
        });
        File berkas = new File("docs/bukti-gui", nama);
        ImageIO.write(gambar, "png", berkas);
        System.out.println("SCREENSHOT=" + nama + " "
                + gambar.getWidth() + "x" + gambar.getHeight());
    }

    private static void check(String nama, boolean lulus, String detail) {
        if (!lulus) {
            gagal++;
        }
        System.out.println((lulus ? "[OK] " : "[GAGAL] ") + nama
                + " :: " + detail);
    }
}
