import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import kantinflow.gui.EmptyState;
import kantinflow.gui.JendelaKantinFlow;
import kantinflow.gui.PanelRingkasan;
import kantinflow.gui.Tema;
import kantinflow.penyimpanan.PenyimpananSQLite;

/**
 * Uji visual tema terang/gelap pada 1366x768 dan 1000x650.
 * Screenshot diambil dari komponen aplikasi, bukan seluruh layar.
 */
public class VisualThemeHarness {
    private static JFrame frame;
    private static JendelaKantinFlow aplikasi;
    private static int gagal;

    public static void main(String[] args) throws Exception {
        Path database = Path.of("data/visual-theme.db");
        Files.deleteIfExists(database);
        Files.deleteIfExists(Path.of("data/kantinflow-tema.properties"));
        SwingRiwayatHarness.seed(database.toString());
        buka(database);
        pilihHalaman("nav-ringkasan");
        Thread.sleep(700);

        check("MODE_AWAL_TERANG", !Tema.gelap(), "gelap=" + Tema.gelap());
        check("EMPAT_STAT_CARD", jumlahKomponen(
                frame.getContentPane(), "StatCard") >= 4,
                "cards=" + jumlahKomponen(frame.getContentPane(), "StatCard"));
        check("TABEL_SEMUA_BARANG", tabel("tabelRingkasan").getRowCount() == 3,
                "rows=" + tabel("tabelRingkasan").getRowCount());
        check("GRAFIK_XCHART", adaKelas(
                frame.getContentPane(), "XChartPanel"), "chart");
        check("STATUS_FOOTER_TERSEMBUNYI",
                !find(frame.getContentPane(), "status").isVisible(), "status");

        snap("dashboard-1366-light.png");
        gulirBawah();
        Thread.sleep(250);
        snap("dashboard-1366-light-bawah.png");

        SwingUtilities.invokeAndWait(() -> frame.setSize(1000, 650));
        Thread.sleep(400);
        gulirAtas();
        snap("dashboard-1000-light.png");
        gulirBawah();
        Thread.sleep(250);
        snap("dashboard-1000-light-bawah.png");

        SwingUtilities.invokeAndWait(() -> ((JButton) find(
                frame.getContentPane(), "tombolTema")).doClick());
        Thread.sleep(900);
        pilihHalaman("nav-ringkasan");
        Thread.sleep(450);
        check("MODE_GELAP", Tema.gelap(), "gelap=" + Tema.gelap());
        check("PREFERENSI_TEMA_TERTULIS",
                Files.isRegularFile(Path.of("data",
                        "kantinflow-tema.properties")), "file");
        gulirAtas();
        snap("dashboard-1000-dark.png");
        gulirBawah();
        Thread.sleep(250);
        snap("dashboard-1000-dark-bawah.png");

        SwingUtilities.invokeAndWait(() -> frame.setSize(1366, 768));
        Thread.sleep(450);
        gulirAtas();
        snap("dashboard-1366-dark.png");
        gulirBawah();
        Thread.sleep(250);
        snap("dashboard-1366-dark-bawah.png");

        SwingUtilities.invokeAndWait(() -> aplikasi.tutup());
        buka(database);
        Thread.sleep(500);
        check("PREFERENSI_TEMA_SETELAH_REOPEN", Tema.gelap(),
                "gelap=" + Tema.gelap());

        SwingUtilities.invokeAndWait(() -> aplikasi.tutup());
        Tema.terapkan(false);
        Files.deleteIfExists(Path.of("data/kantinflow-tema.properties"));
        Files.deleteIfExists(database);
        Files.deleteIfExists(database.resolveSibling(
                database.getFileName() + ".ui"));
        System.out.println(gagal == 0
                ? "VISUAL_THEME_HARNESS_LULUS"
                : "VISUAL_THEME_HARNESS_GAGAL=" + gagal);
        System.exit(gagal == 0 ? 0 : 1);
    }

    private static void buka(Path database) throws Exception {
        JendelaKantinFlow[] app = new JendelaKantinFlow[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                app[0] = new JendelaKantinFlow(
                        new PenyimpananSQLite(database.toString()));
                aplikasi = app[0];
                frame = app[0];
                frame.setSize(1366, 768);
                frame.setLocationRelativeTo(null);
                app[0].tampilkan();
                frame.toFront();
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
        Thread.sleep(800);
    }

    private static void pilihHalaman(String nama) throws Exception {
        SwingUtilities.invokeAndWait(() -> ((JToggleButton) find(
                frame.getContentPane(), nama)).doClick());
        Thread.sleep(250);
    }

    private static JTable tabel(String nama) {
        return (JTable) find(frame.getContentPane(), nama);
    }

    private static JScrollPane halaman() {
        return cariHalaman(frame.getContentPane());
    }

    private static JScrollPane cariHalaman(Container root) {
        for (Component component : root.getComponents()) {
            if (component instanceof JScrollPane scroll
                    && berisiRingkasan(scroll.getViewport().getView())) {
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

    private static boolean berisiRingkasan(Component component) {
        if (component instanceof PanelRingkasan) {
            return true;
        }
        if (component instanceof Container container) {
            for (Component anak : container.getComponents()) {
                if (berisiRingkasan(anak)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void gulirAtas() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JScrollPane scroll = halaman();
            if (scroll != null) {
                scroll.getVerticalScrollBar().setValue(0);
            }
        });
        Thread.sleep(150);
    }

    private static void gulirBawah() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JScrollPane scroll = halaman();
            if (scroll != null) {
                scroll.getVerticalScrollBar().setValue(
                        scroll.getVerticalScrollBar().getMaximum());
            }
        });
        Thread.sleep(200);
    }

    private static void snap(String nama) throws Exception {
        Component isi = frame.getContentPane();
        BufferedImage image = new BufferedImage(isi.getWidth(),
                isi.getHeight(), BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2 = image.createGraphics();
        try {
            isi.paint(g2);
        } finally {
            g2.dispose();
        }
        File file = new File("docs/bukti-gui/" + nama);
        ImageIO.write(image, "png", file);
        System.out.println("SCREENSHOT=" + nama + " "
                + image.getWidth() + "x" + image.getHeight());
    }

    private static int jumlahKomponen(Container root, String kelas) {
        int jumlah = 0;
        for (Component component : root.getComponents()) {
            if (component.getClass().getSimpleName().equals(kelas)) {
                jumlah++;
            }
            if (component instanceof Container container) {
                jumlah += jumlahKomponen(container, kelas);
            }
        }
        return jumlah;
    }

    private static boolean adaKelas(Container root, String kelas) {
        for (Component component : root.getComponents()) {
            if (component.getClass().getSimpleName().equals(kelas)) {
                return true;
            }
            if (component instanceof Container container
                    && adaKelas(container, kelas)) {
                return true;
            }
        }
        return false;
    }

    private static void check(String nama, boolean lulus, String detail) {
        System.out.println((lulus ? "LULUS " : "GAGAL ") + nama + "=" + detail);
        if (!lulus) {
            gagal++;
        }
    }

    private static Component find(Container root, String name) {
        for (Component component : root.getComponents()) {
            if (name.equals(component.getName())) {
                return component;
            }
            if (component instanceof Container container) {
                Component found = find(container, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
