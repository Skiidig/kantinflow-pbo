import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import kantinflow.Barang;
import kantinflow.gui.JendelaKantinFlow;
import kantinflow.penyimpanan.PenyimpananSQLite;

/**
 * Pemeriksaan halaman Barang: dialog tambah/minimum, validasi, pilihan baris,
 * pencarian, sorting, persistensi, dan ukuran/dialog.
 */
public class SwingBarangHarness {
    private static JFrame frame;
    private static JendelaKantinFlow aplikasi;
    private static int gagal;

    public static void main(String[] args) throws Exception {
        Path database = Path.of("data/barang-feature.db");
        Files.deleteIfExists(database);
        Files.deleteIfExists(database.resolveSibling(
                database.getFileName() + ".ui"));
        PenyimpananSQLite storage = new PenyimpananSQLite(database.toString());
        try (Connection connection = DriverManager.getConnection(
                "jdbc:sqlite:" + database);
                Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys=OFF");
            statement.executeUpdate("DELETE FROM rincian_transaksi");
            statement.executeUpdate("DELETE FROM transaksi");
            statement.executeUpdate("DELETE FROM batch");
            statement.executeUpdate("DELETE FROM barang");
        }
        JendelaKantinFlow[] app = new JendelaKantinFlow[1];
        SwingUtilities.invokeAndWait(() -> {
            app[0] = new JendelaKantinFlow(storage);
            aplikasi = app[0];
            frame = app[0];
            frame.setSize(1180, 720);
            frame.setLocationRelativeTo(null);
            app[0].tampilkan();
            frame.toFront();
        });
        Thread.sleep(500);
        frame.setAlwaysOnTop(true);
        SwingUtilities.invokeAndWait(() -> ((JToggleButton) find(
                frame.getContentPane(), "nav-barang")).doClick());
        Thread.sleep(250);

        check("AWAL_TANPA_BARANG", rows() == 0
                && label("kosongBarang").contains("Belum ada barang"),
                label("kosongBarang"));
        check("AWAL_TOMBOL_AKSI_NONAKTIF",
                !((JButton) find(frame.getContentPane(),
                        "jadikanBarangAktif")).isEnabled()
                && !((JButton) find(frame.getContentPane(),
                        "aturMinimum")).isEnabled(), "actions");

        tambah("Susu Kotak", "kotak", "");
        check("TAMBAH_PERTAMA", rows() == 1
                && "Susu Kotak".equals(activeName()),
                "rows=" + rows() + " aktif=" + activeName());
        check("BARANG_BARU_STOK_NOL",
                barangs().size() == 1 && barangs().get(0).hitungSaldo() == 0
                && aplikasi.getRiwayat().isEmpty(),
                "stok/riwayat");

        tambah("Ompreng", "buah", "");
        check("TAMBAH_KEDUA_AKTIF_TETAP", rows() == 2
                && "Susu Kotak".equals(activeName()),
                "aktif=" + activeName());
        tambah("Barang Custom", "Lainnya", "porsi");
        check("TAMBAH_LAINNYA", rows() == 3
                && "Barang Custom (porsi)".equals(cell(2, 0) + " ("
                        + cell(2, 1) + ")"), "rows=" + rows());
        check("TOMBOL_AKSI_NONAKTIF_TANPA_PILIHAN",
                !((JButton) find(frame.getContentPane(),
                        "jadikanBarangAktif")).isEnabled()
                && !((JButton) find(frame.getContentPane(),
                        "aturMinimum")).isEnabled(), "actions");

        int barisSebelumBatal = rows();
        batalTambah("Barang Batal");
        check("BATAL_TAMBAH_TIDAK_MENAMBAH", rows() == barisSebelumBatal,
                "rows=" + rows());
        String errorSatuan = tambahDenganError("Barang 20", "Lainnya", "20");
        check("SATUAN_ANGKA_DITOLAK", errorSatuan.contains("jenis hitungan"),
                errorSatuan);
        check("SATUAN_ANGKA_TIDAK_MENAMBAH", rows() == 3, "rows=" + rows());

        String minSebelumBatal = minimumCell("Ompreng");
        batalMinimum("Ompreng", "7");
        check("BATAL_MINIMUM_TIDAK_MENGUBAH",
                minSebelumBatal.equals(minimumCell("Ompreng")),
                "before=" + minSebelumBatal + " after="
                + minimumCell("Ompreng"));
        String errorKosong = minimum("Ompreng", "", false);
        check("MINIMUM_KOSONG_DITOLAK", errorKosong.contains("wajib diisi"),
                errorKosong);
        String errorAbc = minimum("Ompreng", "abc", false);
        check("MINIMUM_ABC_DITOLAK", errorAbc.contains("bilangan bulat"),
                errorAbc);
        String errorNegatif = minimum("Ompreng", "-1", false);
        check("MINIMUM_NEGATIF_DITOLAK", errorNegatif.contains("negatif"),
                errorNegatif);
        String errorPecahan = minimum("Ompreng", "1.5", false);
        check("MINIMUM_PECAHAN_DITOLAK", errorPecahan.contains("pecahan"),
                errorPecahan);
        String errorBesar = minimum("Ompreng", "99999999999999999999", false);
        check("MINIMUM_BESAR_DITOLAK", errorBesar.contains("bilangan bulat"),
                errorBesar);

        pilihBaris("Ompreng");
        check("KLIK_BARIS_TIDAK_GANTI_AKTIF",
                "Susu Kotak".equals(activeName()), "aktif=" + activeName());
        minimum("Ompreng", "0", true);
        pilihBaris("Ompreng");
        check("MINIMUM_NOL_VALID", minimumCell("Ompreng").startsWith("0 "),
                minimumCell("Ompreng"));
        minimum("Ompreng", " 5 ", true);
        pilihBaris("Ompreng");
        check("MINIMUM_SPASI_DAN_5", minimumCell("Ompreng").startsWith("5 "),
                minimumCell("Ompreng"));
        check("MINIMUM_TIDAK_GANTI_AKTIF",
                "Susu Kotak".equals(activeName()), "aktif=" + activeName());

        String errorSetelah = minimum("Ompreng", "abc", false);
        minimum("Ompreng", "0", true);
        pilihBaris("Ompreng");
        check("ERROR_MINIMUM_DIPERBAIKI",
                !errorSetelah.isBlank() && minimumCell("Ompreng")
                        .startsWith("0 "), "error=" + errorSetelah);

        setSearch("ompreng");
        check("PENCARIAN_CASE_INSENSITIVE", rows() == 1
                && "Ompreng".equals(cell(0, 0)), "rows=" + rows());
        pilihBaris("Ompreng");
        SwingUtilities.invokeAndWait(() -> ((JButton) find(
                frame.getContentPane(), "jadikanBarangAktif")).doClick());
        Thread.sleep(200);
        check("JADIKAN_AKTIF_BARIS_FILTER",
                "Ompreng".equals(activeName()), "aktif=" + activeName());

        setSearch("zzz");
        check("PENCARIAN_TANPA_HASIL", rows() == 0
                && label("kosongBarang").contains("Tidak ada barang"),
                label("kosongBarang"));
        setSearch("");
        pilihBaris("Barang Custom");
        SwingUtilities.invokeAndWait(() -> tabel().getRowSorter()
                .toggleSortOrder(0));
        Thread.sleep(100);
        pilihBaris("Barang Custom");
        minimum("Barang Custom", "2", true);
        pilihBaris("Barang Custom");
        check("SORTING_AKSI_BENAR", minimumCell("Barang Custom")
                .startsWith("2 "), minimumCell("Barang Custom"));

        SwingUtilities.invokeAndWait(() -> {
            frame.setSize(960, 640);
            frame.validate();
        });
        Thread.sleep(250);
        check("LAYOUT_960_KONTROL",
                ((JButton) find(frame.getContentPane(),
                        "tombolTambahBarang")).isShowing()
                && ((JButton) find(frame.getContentPane(),
                        "jadikanBarangAktif")).isShowing()
                && ((JButton) find(frame.getContentPane(),
                        "aturMinimum")).isShowing(),
                "controls");
        tangkap("docs/bukti-gui/gui-flatlaf-barang-kecil.png");
        SwingUtilities.invokeAndWait(() -> {
            frame.setSize(1366, 768);
            frame.validate();
        });
        Thread.sleep(250);
        check("LAYOUT_1366_KONTROL",
                ((JButton) find(frame.getContentPane(),
                        "tombolTambahBarang")).isShowing()
                && ((JButton) find(frame.getContentPane(),
                        "jadikanBarangAktif")).isShowing()
                && ((JButton) find(frame.getContentPane(),
                        "aturMinimum")).isShowing(),
                "controls");
        tangkap("docs/bukti-gui/gui-flatlaf-barang.png");

        SwingUtilities.invokeAndWait(() -> aplikasi.tutup());
        PenyimpananSQLite lagi = new PenyimpananSQLite(database.toString());
        JendelaKantinFlow dibuka = new JendelaKantinFlow(lagi);
        dibuka.setSize(1180, 720);
        dibuka.tampilkan();
        Thread.sleep(300);
        Barang ompreng = null;
        Barang custom = null;
        for (Barang barang : dibuka.getDaftarBarang()) {
            if ("Ompreng".equals(barang.getNama())) {
                ompreng = barang;
            }
            if ("Barang Custom".equals(barang.getNama())) {
                custom = barang;
            }
        }
        check("MINIMUM_TETAP_SETELAH_REOPEN",
                ompreng != null && ompreng.getStokMinimum() == 0
                && custom != null && custom.getStokMinimum() == 2,
                (ompreng == null ? "ompreng tidak ada"
                        : "ompreng=" + ompreng.getStokMinimum())
                + " " + (custom == null ? "custom tidak ada"
                        : "custom=" + custom.getStokMinimum()));
        SwingUtilities.invokeAndWait(dibuka::tutup);
        Files.deleteIfExists(database);
        Files.deleteIfExists(database.resolveSibling(
                database.getFileName() + ".ui"));
        System.out.println(gagal == 0
                ? "BARANG_HARNESS_LULUS" : "BARANG_HARNESS_GAGAL=" + gagal);
        System.exit(gagal == 0 ? 0 : 1);
    }

    private static void tambah(String nama, String satuan, String khusus)
            throws Exception {
        String error = tambahDenganError(nama, satuan, khusus);
        if (!error.isBlank()) {
            throw new AssertionError("Tambah gagal: " + nama + " " + error);
        }
    }

    private static String tambahDenganError(String nama, String satuan,
            String khusus) throws Exception {
        String[] hasil = {""};
        SwingUtilities.invokeAndWait(() -> {
            Timer timer = new Timer(80, event -> {
                JDialog dialog = dialog("dialogTambahBarang");
                if (dialog == null) {
                    return;
                }
                ((JTextField) find(dialog.getContentPane(),
                        "namaBarangBaru")).setText(nama);
                JComboBox<?> combo = (JComboBox<?>) find(
                        dialog.getContentPane(), "satuanBarangBaru");
                combo.setSelectedItem(satuan);
                if ("Lainnya".equals(satuan)) {
                    ((JTextField) find(dialog.getContentPane(),
                            "satuanKhususBarang")).setText(khusus);
                }
                ((JButton) find(dialog.getContentPane(),
                        "simpanTambahBarang")).doClick();
                if (dialog.isShowing()) {
                    hasil[0] = teksPesan(dialog);
                    ((JButton) find(dialog.getContentPane(),
                            "batalTambahBarang")).doClick();
                }
                ((Timer) event.getSource()).stop();
            });
            timer.start();
            ((JButton) find(frame.getContentPane(),
                    "tombolTambahBarang")).doClick();
        });
        Thread.sleep(250);
        return hasil[0];
    }

    private static void batalTambah(String nama) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Timer timer = new Timer(80, event -> {
                JDialog dialog = dialog("dialogTambahBarang");
                if (dialog == null) {
                    return;
                }
                ((JTextField) find(dialog.getContentPane(),
                        "namaBarangBaru")).setText(nama);
                ((JButton) find(dialog.getContentPane(),
                        "batalTambahBarang")).doClick();
                ((Timer) event.getSource()).stop();
            });
            timer.start();
            ((JButton) find(frame.getContentPane(),
                    "tombolTambahBarang")).doClick();
        });
        Thread.sleep(250);
    }

    private static void batalMinimum(String nama, String nilai)
            throws Exception {
        pilihBaris(nama);
        SwingUtilities.invokeAndWait(() -> {
            Timer timer = new Timer(80, event -> {
                JDialog dialog = dialog("dialogAturMinimum");
                if (dialog == null) {
                    return;
                }
                ((JTextField) find(dialog.getContentPane(),
                        "minimumBarang")).setText(nilai);
                ((JButton) find(dialog.getContentPane(),
                        "batalMinimumBarang")).doClick();
                ((Timer) event.getSource()).stop();
            });
            timer.start();
            ((JButton) find(frame.getContentPane(),
                    "aturMinimum")).doClick();
        });
        Thread.sleep(250);
    }

    private static String minimum(String nama, String nilai, boolean valid)
            throws Exception {
        pilihBaris(nama);
        String[] hasil = {""};
        SwingUtilities.invokeAndWait(() -> {
            Timer timer = new Timer(80, event -> {
                JDialog dialog = dialog("dialogAturMinimum");
                if (dialog == null) {
                    return;
                }
                JTextField field = (JTextField) find(
                        dialog.getContentPane(), "minimumBarang");
                field.setText(nilai);
                ((JButton) find(dialog.getContentPane(),
                        "simpanMinimumBarang")).doClick();
                if (dialog.isShowing()) {
                    hasil[0] = ((JLabel) find(dialog.getContentPane(),
                            "pesanMinimumBarang")).getText();
                    ((JButton) find(dialog.getContentPane(),
                            "batalMinimumBarang")).doClick();
                }
                ((Timer) event.getSource()).stop();
            });
            timer.start();
            ((JButton) find(frame.getContentPane(),
                    "aturMinimum")).doClick();
        });
        Thread.sleep(250);
        if (valid && !hasil[0].isBlank()) {
            throw new AssertionError("Minimum valid gagal: " + hasil[0]);
        }
        return hasil[0];
    }

    private static String teksPesan(JDialog dialog) {
        for (String nama : List.of("pesanNamaBarang", "pesanSatuanBarang",
                "pesanSatuanLain", "pesanTambahBarang")) {
            Component komponen = find(dialog.getContentPane(), nama);
            if (komponen instanceof JLabel label && !label.getText().isBlank()
                    && !" ".equals(label.getText())) {
                return label.getText();
            }
        }
        return "";
    }

    private static void pilihBaris(String nama) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (int i = 0; i < tabel().getRowCount(); i++) {
                if (nama.equals(String.valueOf(tabel().getValueAt(i, 0)))) {
                    tabel().setRowSelectionInterval(i, i);
                    return;
                }
            }
        });
        Thread.sleep(100);
    }

    private static void setSearch(String nilai) throws Exception {
        SwingUtilities.invokeAndWait(() -> ((JTextField) find(
                frame.getContentPane(), "cariBarang")).setText(nilai));
        Thread.sleep(200);
    }

    private static String minimumCell(String nama) {
        for (int i = 0; i < tabel().getRowCount(); i++) {
            if (nama.equals(String.valueOf(tabel().getValueAt(i, 0)))) {
                return String.valueOf(tabel().getValueAt(i, 3));
            }
        }
        return "";
    }

    private static String cell(int row, int col) {
        return String.valueOf(tabel().getValueAt(row, col));
    }

    private static JTable tabel() {
        return (JTable) find(frame.getContentPane(), "tabelBarang");
    }

    private static int rows() {
        return tabel().getRowCount();
    }

    private static String activeName() {
        return aplikasi.getBarangAktif() == null
                ? "" : aplikasi.getBarangAktif().getNama();
    }

    private static List<Barang> barangs() {
        return aplikasi.getDaftarBarang();
    }

    private static String label(String nama) {
        Component komponen = find(frame.getContentPane(), nama);
        if (komponen instanceof kantinflow.gui.EmptyState kosong) {
            return kosong.getText();
        }
        return komponen instanceof JLabel label ? label.getText() : "";
    }

    private static JDialog dialog(String nama) {
        for (Window window : Window.getWindows()) {
            if (window instanceof JDialog dialog && dialog.isShowing()
                    && nama.equals(dialog.getName())) {
                return dialog;
            }
        }
        return null;
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
