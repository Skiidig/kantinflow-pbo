import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import kantinflow.Barang;
import kantinflow.gui.JendelaKantinFlow;
import kantinflow.penyimpanan.PenyimpananSQLite;

/**
 * Pemeriksaan UI Ekspor PDF: tombol, chooser, overwrite, batal, status, dan
 * screenshot keberhasilan. Dialog Buka PDF selalu ditutup tanpa membuka file.
 */
public class SwingPdfExportHarness {
    private static JFrame frame;
    private static JendelaKantinFlow aplikasi;
    private static int gagal;

    public static void main(String[] args) throws Exception {
        Path database = Path.of("data/pdf-ui-feature.db");
        Path sukses = Path.of("data/pdf-ui-success.pdf");
        Path timpa = Path.of("data/pdf-ui-overwrite.pdf");
        Path batal = Path.of("data/pdf-ui-cancel.pdf");
        for (Path path : List.of(database, sukses, timpa, batal)) {
            Files.deleteIfExists(path);
            Files.deleteIfExists(path.resolveSibling(
                    path.getFileName() + ".ui"));
        }
        SwingRiwayatHarness.seed(database.toString());

        JendelaKantinFlow[] app = new JendelaKantinFlow[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                app[0] = new JendelaKantinFlow(
                        new PenyimpananSQLite(database.toString()));
                aplikasi = app[0];
                frame = app[0];
                frame.setSize(1180, 720);
                frame.setLocationRelativeTo(null);
                app[0].tampilkan();
                frame.toFront();
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
        Thread.sleep(500);
        frame.setAlwaysOnTop(true);
        pilihBarang("Roti");
        SwingUtilities.invokeAndWait(() -> ((JToggleButton) find(
                frame.getContentPane(), "nav-riwayat")).doClick());
        Thread.sleep(200);

        JButton pdf = (JButton) find(frame.getContentPane(), "eksporPdf");
        JButton csv = (JButton) find(frame.getContentPane(), "eksporRiwayat");
        check("TOMBOL_PDF_ADA", pdf != null, "button");
        check("TOMBOL_PDF_AKTIF", pdf.isEnabled(), "enabled");
        check("TOMBOL_CSV_TETAP_ADA", csv != null && csv.isEnabled(), "csv");
        int saldoAwal = aplikasi.getBarangAktif().hitungSaldo();
        int riwayatAwal = aplikasi.getRiwayat().size();

        eksporPdf(sukses, false, true, false);
        tungguFile(sukses);
        check("PDF_SUKSES_FILE", Files.isRegularFile(sukses), "file");
        check("PDF_SUKSES_STATUS", status().contains("Ekspor PDF berhasil")
                && status().contains("6 rincian"), status());
        tungguSelesaiPdf(pdf);
        tangkap("docs/bukti-gui/gui-flatlaf-ekspor-pdf.png");

        Files.writeString(timpa, "FILE-LAMA");
        eksporPdf(timpa, true, false, false);
        Thread.sleep(500);
        check("PDF_BATAL_TIMPA_TIDAK_MENGGANTI",
                "FILE-LAMA".equals(Files.readString(timpa)),
                "isi=" + Files.readString(timpa));
        check("PDF_BATAL_TIMPA_BUKAN_GAGAL",
                !status().contains("gagal"), status());

        eksporPdf(timpa, true, true, false);
        tungguFile(timpa);
        check("PDF_TIMPA_BERHASIL", Files.size(timpa) > 100,
                "size=" + Files.size(timpa));

        eksporPdf(batal, false, true, true);
        Thread.sleep(500);
        check("PDF_BATAL_PILIH_TIDAK_MEMBUAT",
                !Files.exists(batal), "exists=" + Files.exists(batal));
        check("PDF_BATAL_PILIH_BUKAN_GAGAL", !status().contains("gagal"),
                status());

        SwingUtilities.invokeAndWait(() -> {
            JTextField cari = (JTextField) find(
                    frame.getContentPane(), "cariRiwayat");
            cari.setText("zzz");
            ((JButton) find(frame.getContentPane(),
                    "terapkanFilterRiwayat")).doClick();
        });
        Thread.sleep(200);
        JButton pdfSetelahKosong = (JButton) find(
                frame.getContentPane(), "eksporPdf");
        check("PDF_TANPA_HASIL_NONAKTIF",
                !pdfSetelahKosong.isEnabled(), "enabled="
                + pdfSetelahKosong.isEnabled());
        check("SALDO_TIDAK_UBAH",
                aplikasi.getBarangAktif().hitungSaldo() == saldoAwal,
                "saldo=" + aplikasi.getBarangAktif().hitungSaldo());
        check("RIWAYAT_TIDAK_UBAH",
                aplikasi.getRiwayat().size() == riwayatAwal,
                "riwayat=" + aplikasi.getRiwayat().size());
        SwingUtilities.invokeAndWait(() -> {
            frame.setSize(960, 640);
            frame.validate();
        });
        Thread.sleep(250);
        check("KONTROL_EKSPOR_960",
                pdfSetelahKosong.isShowing()
                && ((JButton) find(frame.getContentPane(),
                        "eksporRiwayat")).isShowing()
                && ((JButton) find(frame.getContentPane(),
                        "detailRiwayat")).isShowing(),
                "pdf/csv/detail showing");

        SwingUtilities.invokeAndWait(() -> {
            frame.setAlwaysOnTop(false);
            aplikasi.tutup();
        });
        for (Path path : List.of(sukses, timpa, batal)) {
            Files.deleteIfExists(path);
        }
        Files.deleteIfExists(database);
        Files.deleteIfExists(database.resolveSibling(
                database.getFileName() + ".ui"));
        System.out.println(gagal == 0
                ? "SWING_PDF_HARNESS_LULUS"
                : "SWING_PDF_HARNESS_GAGAL=" + gagal);
        System.exit(gagal == 0 ? 0 : 1);
    }

    private static void eksporPdf(Path tujuan, boolean adaTimpa,
            boolean setuju, boolean batalPilih) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JButton button = (JButton) find(
                    frame.getContentPane(), "eksporPdf");
            final boolean[] chooserSelesai = {false};
            final boolean[] konfirmasiSelesai = {false};
            Timer timer = new Timer(80, event -> {
                JFileChooser chooser = cariChooser();
                if (!chooserSelesai[0]) {
                    if (chooser != null) {
                        chooserSelesai[0] = true;
                        if (batalPilih) {
                            chooser.cancelSelection();
                            ((Timer) event.getSource()).stop();
                        } else {
                            chooser.setSelectedFile(tujuan.toFile());
                            chooser.approveSelection();
                        }
                    }
                    return;
                }
                if (batalPilih) {
                    return;
                }
                if (adaTimpa && !konfirmasiSelesai[0]) {
                    JButton timpa = cariTombolDialog(setuju);
                    if (timpa != null) {
                        konfirmasiSelesai[0] = true;
                        timpa.doClick();
                        if (!setuju) {
                            ((Timer) event.getSource()).stop();
                        }
                    }
                    return;
                }
                JButton tutup = cariTombolTeks("Tutup");
                if (tutup != null) {
                    tutup.doClick();
                    ((Timer) event.getSource()).stop();
                }
            });
            timer.start();
            button.doClick();
        });
        Thread.sleep(200);
    }

    private static JFileChooser cariChooser() {
        for (Window window : Window.getWindows()) {
            if (window instanceof JDialog dialog && dialog.isShowing()) {
                JFileChooser chooser = cariChooser(dialog.getContentPane());
                if (chooser != null) {
                    return chooser;
                }
            }
        }
        return null;
    }

    private static JFileChooser cariChooser(Container root) {
        for (Component component : root.getComponents()) {
            if (component instanceof JFileChooser chooser) {
                return chooser;
            }
            if (component instanceof Container container) {
                JFileChooser found = cariChooser(container);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static JButton cariTombolDialog(boolean setuju) {
        for (Window window : Window.getWindows()) {
            if (window instanceof JDialog dialog && dialog.isShowing()) {
                JButton button = cariTombol(dialog.getContentPane(), setuju);
                if (button != null) {
                    return button;
                }
            }
        }
        return null;
    }

    private static JButton cariTombol(Container root, boolean setuju) {
        for (Component component : root.getComponents()) {
            if (component instanceof JButton button) {
                String text = button.getText() == null ? ""
                        : button.getText().toLowerCase();
                if (setuju && (text.contains("yes") || text.contains("ya"))) {
                    return button;
                }
                if (!setuju && (text.contains("no") || text.contains("tidak"))) {
                    return button;
                }
            }
            if (component instanceof Container container) {
                JButton found = cariTombol(container, setuju);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static JButton cariTombolTeks(String teks) {
        for (Window window : Window.getWindows()) {
            if (window instanceof JDialog dialog && dialog.isShowing()) {
                JButton button = cariTombolTeks(dialog.getContentPane(), teks);
                if (button != null) {
                    return button;
                }
            }
        }
        return null;
    }

    private static JButton cariTombolTeks(Container root, String teks) {
        for (Component component : root.getComponents()) {
            if (component instanceof JButton button
                    && teks.equalsIgnoreCase(button.getText())) {
                return button;
            }
            if (component instanceof Container container) {
                JButton found = cariTombolTeks(container, teks);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void tungguSelesaiPdf(JButton pdf) throws Exception {
        for (int i = 0; i < 30; i++) {
            if (pdf.isEnabled() && status().contains("Ekspor PDF berhasil")) {
                Thread.sleep(300);
                frame.repaint();
                return;
            }
            Thread.sleep(100);
        }
    }

    private static void tungguFile(Path path) throws Exception {
        for (int i = 0; i < 50; i++) {
            if (Files.isRegularFile(path) && Files.size(path) > 100
                    && status().contains("Ekspor PDF berhasil")) {
                return;
            }
            Thread.sleep(100);
        }
    }

    private static String status() {
        return ((JTextArea) find(frame.getContentPane(), "status")).getText();
    }

    private static void pilihBarang(String nama) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JComboBox<?> combo = (JComboBox<?>) find(
                    frame.getContentPane(), "barangAktif");
            for (int i = 0; i < combo.getItemCount(); i++) {
                Object item = combo.getItemAt(i);
                if (item instanceof Barang barang
                        && nama.equals(barang.getNama())) {
                    combo.setSelectedIndex(i);
                    return;
                }
            }
        });
        Thread.sleep(200);
    }

    private static void tangkap(String path) throws Exception {
        Component content = frame.getContentPane();
        Rectangle bounds = new Rectangle(content.getLocationOnScreen(),
                content.getSize());
        BufferedImage image = new Robot().createScreenCapture(bounds);
        ImageIO.write(image, "png", new java.io.File(path));
        System.out.println("SCREENSHOT=" + path);
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
