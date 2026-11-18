import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import kantinflow.Barang;
import kantinflow.PenerimaanStok;
import kantinflow.gui.JendelaKantinFlow;
import kantinflow.penyimpanan.PenyimpananSQLite;

/**
 * Pemeriksaan pemilih barang di sidebar, konfirmasi form transaksi, dan
 * sinkronisasi konteks dua barang.
 */
public class SwingActiveBarangHarness {
    private static JFrame frame;
    private static JendelaKantinFlow aplikasi;
    private static int gagal;

    public static void main(String[] args) throws Exception {
        Path database = Path.of("data/active-barang-feature.db");
        Files.deleteIfExists(database);
        SwingRiwayatHarness.seed(database.toString());
        try (PenyimpananSQLite db = new PenyimpananSQLite(database.toString())) {
            db.simpanBarang(new Barang(
                    "Roti Ganda Cokelat Keju dengan Nama Sangat Panjang",
                    "bungkus"));
        }

        SwingUtilities.invokeAndWait(() -> {
            try {
                aplikasi = new JendelaKantinFlow(
                        new PenyimpananSQLite(database.toString()));
                frame = aplikasi;
                frame.setSize(1180, 720);
                frame.setLocationRelativeTo(null);
                aplikasi.tampilkan();
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
        Thread.sleep(500);
        pilihBarang("Roti");
        pilihBarang("Roti Ganda Cokelat Keju dengan Nama Sangat Panjang");
        JComboBox<?> comboPanjang = (JComboBox<?>) find(
                frame.getContentPane(), "barangAktif");
        check("NAMA_PANJANG_TOOLTIP",
                comboPanjang.getToolTipText() != null
                && comboPanjang.getToolTipText().contains("Roti Ganda"),
                "tooltip=" + comboPanjang.getToolTipText());
        check("NAMA_PANJANG_TIDAK_MELEBARKAN",
                comboPanjang.getWidth() <= 240,
                "width=" + comboPanjang.getWidth());
        pilihBarang("Roti");
        SwingUtilities.invokeAndWait(() -> ((JToggleButton) find(
                frame.getContentPane(), "nav-stok-masuk")).doClick());
        Thread.sleep(200);

        check("SATU_DROPDOWN", count(frame.getContentPane(), "barangAktif") == 1,
                "count=" + count(frame.getContentPane(), "barangAktif"));
        check("KONTEKS_STOK_MASUK",
                label("konteksStokMasuk").contains("Roti")
                && label("konteksStokMasuk").contains("bungkus"),
                label("konteksStokMasuk"));
        SwingUtilities.invokeAndWait(() -> ((JToggleButton) find(
                frame.getContentPane(), "nav-stok-keluar")).doClick());
        check("KONTEKS_STOK_KELUAR",
                label("konteksStokKeluar").contains("Roti"),
                label("konteksStokKeluar"));
        SwingUtilities.invokeAndWait(() -> ((JToggleButton) find(
                frame.getContentPane(), "nav-pemantauan")).doClick());
        check("KONTEKS_PEMANTAUAN",
                label("konteksPemantauan").contains("Roti"),
                label("konteksPemantauan"));
        SwingUtilities.invokeAndWait(() -> ((JToggleButton) find(
                frame.getContentPane(), "nav-pembuangan")).doClick());
        check("KONTEKS_PEMBUANGAN",
                label("konteksPembuangan").contains("Roti"),
                label("konteksPembuangan"));
        SwingUtilities.invokeAndWait(() -> ((JToggleButton) find(
                frame.getContentPane(), "nav-stok-masuk")).doClick());

        isi("kodeBatch", "ROTI-CONFIRM");
        pilihBarangDenganJawaban("Susu", false);
        check("BATAL_TETAP_ROTI",
                "Roti".equals(aplikasi.getBarangAktif().getNama())
                && "ROTI-CONFIRM".equals(text("kodeBatch")),
                "aktif=" + aplikasi.getBarangAktif().getNama()
                + " kode=" + text("kodeBatch"));
        check("KONFIRMASI_BATAL_MENGEMBALIKAN_COMBO",
                "Roti".equals(comboText()), "combo=" + comboText());

        pilihBarangDenganJawaban("Susu", true);
        check("SETUJU_PINDAH_SUSU",
                "Susu".equals(aplikasi.getBarangAktif().getNama()),
                "aktif=" + aplikasi.getBarangAktif().getNama());
        check("FORM_DIBERSIHKAN",
                text("kodeBatch").isEmpty()
                && label("konteksStokMasuk").contains("Susu"),
                "kode=" + text("kodeBatch") + " konteks="
                + label("konteksStokMasuk"));

        pilihBarang("Roti");
        check("KONTEKS_KEMBALI_ROTI",
                label("konteksStokMasuk").contains("Roti"),
                label("konteksStokMasuk"));
        check("RIWAYAT_DUA_BARANG_TIDAK_CAMPUR",
                aplikasi.getRiwayat().size() == 5
                && aplikasi.getRiwayat().stream()
                        .flatMap(t -> t.getRincian().stream())
                        .allMatch(r -> !"SUSU-A".equals(
                                r.getBatch().getKode())),
                "riwayatRoti=" + aplikasi.getRiwayat().size());

        SwingUtilities.invokeAndWait(() -> ((JToggleButton) find(
                frame.getContentPane(), "nav-riwayat")).doClick());
        Thread.sleep(150);
        check("KONTEKS_RIWAYAT",
                label("konteksRiwayat").contains("Roti"),
                label("konteksRiwayat"));

        SwingUtilities.invokeAndWait(() -> ((JToggleButton) find(
                frame.getContentPane(), "nav-stok-masuk")).doClick());
        isi("kodeBatch", "ROTI-SAVE");
        isi("jumlah", "1");
        isi("tanggalMasuk", "2026-11-05");
        isi("tanggalKedaluwarsa", "2026-12-05");
        SwingUtilities.invokeAndWait(() -> ((JButton) find(
                frame.getContentPane(), "tombolSimpan")).doClick());
        Thread.sleep(250);
        check("SIMPAN_FORM_BERSIH", !aplikasi.getBarangAktif()
                .getDaftarBatch().isEmpty(), "simpan");
        pilihBarang("Susu");
        check("RELOAD_TANPA_KONFIRMASI",
                "Susu".equals(aplikasi.getBarangAktif().getNama()),
                "aktif=" + aplikasi.getBarangAktif().getNama());

        SwingUtilities.invokeAndWait(() -> {
            frame.setSize(960, 640);
            frame.validate();
            ((JToggleButton) find(frame.getContentPane(),
                    "nav-barang")).doClick();
        });
        Thread.sleep(200);
        JComboBox<?> combo960 = (JComboBox<?>) find(
                frame.getContentPane(), "barangAktif");
        boolean semuaMenu = true;
        for (String kunci : new String[]{"ringkasan", "barang", "stok-masuk",
                "stok-keluar", "pemantauan", "pembuangan", "riwayat"}) {
            Component menu = find(frame.getContentPane(), "nav-" + kunci);
            semuaMenu &= menu != null && menu.isShowing();
        }
        check("SIDEBAR_960_TERJANGKAU",
                combo960 != null && combo960.isShowing() && semuaMenu,
                "comboShowing=" + (combo960 != null && combo960.isShowing())
                + " semuaMenu=" + semuaMenu);
        check("KEYBOARD_FOKUS_TERSEDIA",
                combo960.isFocusable() && semuaMenu,
                "comboFocusable=" + combo960.isFocusable());

        SwingUtilities.invokeAndWait(() -> aplikasi.tutup());
        JendelaKantinFlow[] dibukaUlang = new JendelaKantinFlow[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                dibukaUlang[0] = new JendelaKantinFlow(
                        new PenyimpananSQLite(database.toString()));
                dibukaUlang[0].setSize(1180, 720);
                dibukaUlang[0].tampilkan();
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
        Thread.sleep(300);
        check("BARANG_AKTIF_TETAP_SETELAH_REOPEN",
                "Susu".equals(dibukaUlang[0].getBarangAktif().getNama()),
                "aktif=" + dibukaUlang[0].getBarangAktif().getNama());
        SwingUtilities.invokeAndWait(() -> dibukaUlang[0].tutup());
        Files.deleteIfExists(database);
        Files.deleteIfExists(database.resolveSibling(
                database.getFileName() + ".ui"));
        System.out.println(gagal == 0
                ? "ACTIVE_BARANG_HARNESS_LULUS"
                : "ACTIVE_BARANG_HARNESS_GAGAL=" + gagal);
        System.exit(gagal == 0 ? 0 : 1);
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
        Thread.sleep(250);
    }

    private static void pilihBarangDenganJawaban(String nama, boolean setuju)
            throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Timer timer = new Timer(100, event -> {
                for (Window window : Window.getWindows()) {
                    if (window instanceof JDialog dialog && dialog.isShowing()) {
                        JButton tombol = cariTombol(dialog.getContentPane(),
                                setuju);
                        if (tombol != null) {
                            tombol.doClick();
                            ((Timer) event.getSource()).stop();
                        }
                    }
                }
            });
            timer.start();
            JComboBox<?> combo = (JComboBox<?>) find(
                    frame.getContentPane(), "barangAktif");
            for (int i = 0; i < combo.getItemCount(); i++) {
                Object item = combo.getItemAt(i);
                if (item instanceof Barang barang
                        && nama.equals(barang.getNama())) {
                    combo.setSelectedIndex(i);
                    break;
                }
            }
            timer.stop();
        });
        Thread.sleep(250);
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

    private static String comboText() {
        JComboBox<?> combo = (JComboBox<?>) find(
                frame.getContentPane(), "barangAktif");
        return combo.getSelectedItem() instanceof Barang barang
                ? barang.getNama() : "";
    }

    private static String text(String name) {
        return ((JTextField) find(frame.getContentPane(), name)).getText();
    }

    private static String label(String name) {
        return ((javax.swing.JLabel) find(frame.getContentPane(), name))
                .getText();
    }

    private static void isi(String name, String value) {
        ((JTextField) find(frame.getContentPane(), name)).setText(value);
    }

    private static int count(Container root, String name) {
        int jumlah = 0;
        for (Component component : root.getComponents()) {
            if (name.equals(component.getName())) {
                jumlah++;
            }
            if (component instanceof Container container) {
                jumlah += count(container, name);
            }
        }
        return jumlah;
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
