import java.awt.Component;
import java.awt.Container;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import kantinflow.Barang;
import kantinflow.gui.JendelaKantinFlow;
import kantinflow.penyimpanan.PenyimpananSQLite;

/**
 * Pemeriksaan kondisi database tanpa barang dan penambahan barang pertama.
 */
public class SwingEmptyBarangHarness {
    private static JFrame frame;
    private static int gagal;

    public static void main(String[] args) throws Exception {
        Path database = Path.of("data/empty-barang-feature.db");
        Path preferensi = database.resolveSibling(
                database.getFileName() + ".ui");
        Files.deleteIfExists(database);
        Files.deleteIfExists(preferensi);
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

        JendelaKantinFlow[] aplikasi = new JendelaKantinFlow[1];
        SwingUtilities.invokeAndWait(() -> {
            aplikasi[0] = new JendelaKantinFlow(storage);
            frame = aplikasi[0];
            frame.setSize(1180, 720);
            aplikasi[0].tampilkan();
        });
        Thread.sleep(500);
        JComboBox<?> combo = (JComboBox<?>) find(
                frame.getContentPane(), "barangAktif");
        JLabel kosong = (JLabel) find(
                frame.getContentPane(), "barangKosongSidebar");
        check("KOSONG_TANPA_CRASH", aplikasi[0].getBarangAktif() == null,
                "aktif=" + aplikasi[0].getBarangAktif());
        check("KOSONG_COMBO_DISABLED",
                !combo.isEnabled() && combo.getItemCount() == 0,
                "enabled=" + combo.isEnabled() + " items="
                + combo.getItemCount());
        check("KOSONG_PESAN_SIDEBAR",
                kosong != null && kosong.isVisible()
                && kosong.getText().contains("Barang"),
                kosong == null ? "null" : kosong.getText());
        check("KOSONG_HALAMAN_BARANG",
                page("PanelBarang") != null && page("PanelBarang").isShowing(),
                "page");

        ((JTextField) find(frame.getContentPane(), "namaBarangBaru"))
                .setText("Barang Baru");
        ((JTextField) find(frame.getContentPane(), "satuanBarangBaru"))
                .setText("pcs");
        SwingUtilities.invokeAndWait(() -> ((JButton) find(
                frame.getContentPane(), "tombolTambahBarang")).doClick());
        Thread.sleep(300);
        Barang aktif = aplikasi[0].getBarangAktif();
        check("TAMBAH_PERTAMA_AKTIF",
                aktif != null && "Barang Baru".equals(aktif.getNama()),
                aktif == null ? "null" : aktif.getNama());
        check("COMBO_AKTIF_SETELAH_TAMBAH",
                combo.isEnabled() && combo.getItemCount() == 1,
                "enabled=" + combo.isEnabled() + " items="
                + combo.getItemCount());
        check("KONTEKS_SETELAH_TAMBAH",
                ((JLabel) find(frame.getContentPane(), "konteksStokMasuk")) != null
                && ((JLabel) find(frame.getContentPane(),
                        "konteksStokMasuk")).getText().contains("Barang Baru"),
                "konteks");

        SwingUtilities.invokeAndWait(() -> aplikasi[0].tutup());
        Files.deleteIfExists(database);
        Files.deleteIfExists(preferensi);
        System.out.println(gagal == 0
                ? "EMPTY_BARANG_HARNESS_LULUS"
                : "EMPTY_BARANG_HARNESS_GAGAL=" + gagal);
        System.exit(gagal == 0 ? 0 : 1);
    }

    private static JComponent page(String simpleName) {
        return page(frame.getContentPane(), simpleName);
    }

    private static JComponent page(Container root, String simpleName) {
        for (Component component : root.getComponents()) {
            if (component.getClass().getSimpleName().equals(simpleName)
                    && component instanceof JComponent jcomponent) {
                return jcomponent;
            }
            if (component instanceof Container container) {
                JComponent found = page(container, simpleName);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
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
