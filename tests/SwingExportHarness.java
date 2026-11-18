import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
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
 * Pemeriksaan UI Ekspor CSV: snapshot urutan tabel, filter, overwrite,
 * pembatalan, status, dan bukti screenshot.
 */
public class SwingExportHarness {
    private static JFrame frame;
    private static JendelaKantinFlow aplikasi;
    private static int gagal;

    public static void main(String[] args) throws Exception {
        Path database = Path.of("data/csv-export-feature.db");
        Path tanpaFilter = Path.of("data/csv-export-no-filter.csv");
        Path filterFefo = Path.of("data/csv-export-fefo.csv");
        Path filterBatch = Path.of("data/csv-export-batch.csv");
        Path batal = Path.of("data/csv-export-cancel.csv");
        Path timpa = Path.of("data/csv-export-overwrite.csv");
        for (Path path : List.of(database, tanpaFilter, filterFefo,
                filterBatch, batal, timpa)) {
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

        JButton ekspor = (JButton) find(
                frame.getContentPane(), "eksporRiwayat");
        check("TOMBOL_EKSPOR_ADA", ekspor != null, "button");
        check("TOMBOL_EKSPOR_AKTIF", ekspor.isEnabled(), "enabled");
        check("TANPA_FILTER_BARIS", rows() == 6, "rows=" + rows());
        int saldoAwal = aplikasi.getBarangAktif().hitungSaldo();
        int riwayatAwal = aplikasi.getRiwayat().size();

        eksporDenganPilihan(tanpaFilter, false, false);
        tungguFile(tanpaFilter);
        check("EKSPOR_TANPA_FILTER_FILE", Files.isRegularFile(tanpaFilter),
                "file");
        check("EKSPOR_TANPA_FILTER_BARIS", csvRows(tanpaFilter) == 6,
                "data rows=" + csvRows(tanpaFilter));
        String isiTanpaFilter = Files.readString(
                tanpaFilter, StandardCharsets.UTF_8);
        check("BARANG_LAIN_TIDAK_IKUT",
                !isiTanpaFilter.contains("SUSU-A")
                && isiTanpaFilter.startsWith(
                        "\uFEFFBarang,Satuan,Tanggal transaksi,"
                        + "Jenis transaksi,Kode batch,Jumlah,Alasan\r\n"),
                "header/barang");
        check("EKSPOR_TANPA_FILTER_STATUS",
                status().contains("Ekspor CSV berhasil")
                && status().contains("6 rincian"), status());
        tangkap("docs/bukti-gui/gui-flatlaf-ekspor-csv.png");

        pilihJenis("Pengeluaran FEFO");
        terapkan();
        check("FEFO_DUA_BARIS", rows() == 2, "rows=" + rows());
        eksporDenganPilihan(filterFefo, false, false);
        tungguFile(filterFefo);
        check("EKSPOR_FEFO_DUA_BARIS", csvRows(filterFefo) == 2,
                "data rows=" + csvRows(filterFefo));

        isi("cariRiwayat", "ROTI-A");
        terapkan();
        check("FEFO_FILTER_SATU_BARIS", rows() == 1, "rows=" + rows());
        eksporDenganPilihan(filterBatch, false, false);
        tungguFile(filterBatch);
        check("EKSPOR_FEFO_FILTER_SATU_BARIS", csvRows(filterBatch) == 1,
                "data rows=" + csvRows(filterBatch));

        pilihJenis("Semua");
        isi("cariRiwayat", "");
        terapkan();
        SwingUtilities.invokeAndWait(() -> tabel().getRowSorter()
                .toggleSortOrder(0));
        Thread.sleep(100);
        eksporDenganPilihan(timpa, false, false);
        tungguFile(timpa);
        String[] barisCsv = Files.readString(timpa, StandardCharsets.UTF_8)
                .replace("\r\n", "\n").split("\n");
        boolean urutanSesuai = true;
        StringBuilder urutan = new StringBuilder();
        for (int i = 0; i < rows(); i++) {
            String batchTabel = String.valueOf(tabel().getValueAt(i, 2));
            String[] kolom = barisCsv[i + 1].split(",", -1);
            urutan.append(batchTabel).append('/')
                    .append(kolom.length < 5 ? "?" : kolom[4]).append(' ');
            if (kolom.length < 5 || !batchTabel.equals(kolom[4])) {
                urutanSesuai = false;
            }
        }
        check("EKSPOR_URUTAN_TABEL", urutanSesuai, urutan.toString());

        Files.writeString(timpa, "JANGAN-DITIMPA",
                StandardCharsets.UTF_8);
        eksporDenganPilihan(timpa, true, false);
        Thread.sleep(500);
        check("BATAL_TIMPA_TIDAK_MENGGANTI",
                "JANGAN-DITIMPA".equals(Files.readString(timpa)),
                "isi=" + Files.readString(timpa));
        check("BATAL_TIMPA_BUKAN_GAGAL",
                !status().contains("gagal"), status());

        eksporDenganPilihan(timpa, true, true);
        tungguFile(timpa);
        check("TIMPA_BERHASIL",
                Files.readString(timpa, StandardCharsets.UTF_8)
                        .contains("Barang,Satuan"), "header");
        check("STATUS_TIMPA", status().contains("Ekspor CSV berhasil"),
                status());

        eksporDenganPilihan(batal, false, false, true);
        Thread.sleep(400);
        check("BATAL_PILIH_TIDAK_MEMBUAT_FILE",
                !Files.exists(batal), "exists=" + Files.exists(batal));
        check("BATAL_PILIH_BUKAN_GAGAL", !status().contains("gagal"),
                status());

        pilihJenis("Semua");
        isi("cariRiwayat", "zzz");
        terapkan();
        check("TANPA_HASIL_BUTTON_NONAKTIF",
                !ekspor.isEnabled() && rows() == 0,
                "enabled=" + ekspor.isEnabled() + " rows=" + rows());

        check("SALDO_TIDAK_UBAH",
                aplikasi.getBarangAktif().hitungSaldo() == saldoAwal,
                "saldo=" + aplikasi.getBarangAktif().hitungSaldo());
        check("RIWAYAT_TIDAK_UBAH",
                aplikasi.getRiwayat().size() == riwayatAwal,
                "riwayat=" + aplikasi.getRiwayat().size());

        SwingUtilities.invokeAndWait(() -> {
            frame.setAlwaysOnTop(false);
            aplikasi.tutup();
        });
        for (Path path : List.of(tanpaFilter, filterFefo, filterBatch,
                batal, timpa)) {
            Files.deleteIfExists(path);
        }
        Files.deleteIfExists(database);
        Files.deleteIfExists(database.resolveSibling(
                database.getFileName() + ".ui"));
        System.out.println(gagal == 0
                ? "SWING_EXPORT_HARNESS_LULUS"
                : "SWING_EXPORT_HARNESS_GAGAL=" + gagal);
        System.exit(gagal == 0 ? 0 : 1);
    }

    private static void eksporDenganPilihan(
            Path tujuan, boolean adaKonfirmasi, boolean setuju)
            throws Exception {
        eksporDenganPilihan(tujuan, adaKonfirmasi, setuju, false);
    }

    private static void eksporDenganPilihan(
            Path tujuan, boolean adaKonfirmasi, boolean setuju,
            boolean batal) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JButton button = (JButton) find(
                    frame.getContentPane(), "eksporRiwayat");
            final boolean[] chooserSelesai = {false};
            Timer timer = new Timer(80, event -> {
                JFileChooser chooser = cariChooser();
                if (!chooserSelesai[0]) {
                    if (chooser != null) {
                        chooserSelesai[0] = true;
                        if (batal) {
                            chooser.cancelSelection();
                        } else {
                            chooser.setSelectedFile(tujuan.toFile());
                            chooser.approveSelection();
                        }
                    }
                    return;
                }
                if (adaKonfirmasi) {
                    JButton jawab = cariTombolDialog(setuju);
                    if (jawab != null) {
                        jawab.doClick();
                        ((Timer) event.getSource()).stop();
                    }
                } else {
                    ((Timer) event.getSource()).stop();
                }
            });
            timer.start();
            button.doClick();
            timer.stop();
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

    private static void tungguFile(Path path) throws Exception {
        for (int i = 0; i < 30 && !Files.isRegularFile(path); i++) {
            Thread.sleep(100);
        }
    }

    private static int csvRows(Path path) throws Exception {
        String text = Files.readString(path, StandardCharsets.UTF_8);
        String[] lines = text.replace("\r\n", "\n").split("\n");
        return Math.max(0, lines.length - 1);
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

    private static void pilihJenis(String value) throws Exception {
        SwingUtilities.invokeAndWait(() -> ((JComboBox<?>) find(
                frame.getContentPane(), "jenisRiwayat"))
                .setSelectedItem(value));
    }

    private static void isi(String name, String value) throws Exception {
        SwingUtilities.invokeAndWait(() -> ((JTextField) find(
                frame.getContentPane(), name)).setText(value));
    }

    private static void terapkan() throws Exception {
        SwingUtilities.invokeAndWait(() -> ((JButton) find(
                frame.getContentPane(), "terapkanFilterRiwayat")).doClick());
        Thread.sleep(150);
    }

    private static JTable tabel() {
        return (JTable) find(frame.getContentPane(), "tabelRiwayat");
    }

    private static int rows() {
        return tabel().getRowCount();
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
