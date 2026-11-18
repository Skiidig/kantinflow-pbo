import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import kantinflow.Barang;
import kantinflow.BatchStok;
import kantinflow.PembuanganStok;
import kantinflow.PenerimaanStok;
import kantinflow.PengeluaranStok;
import kantinflow.gui.JendelaKantinFlow;
import kantinflow.penyimpanan.PenyimpananSQLite;

/**
 * Pemeriksaan UI Riwayat: filter, detail, FEFO, dan isolasi barang.
 * Jalankan setelah mengompilasi src/main/java ke out.
 */
public class SwingRiwayatHarness {
    private static final LocalDate T1 = LocalDate.of(2026, 11, 1);
    private static final LocalDate T2 = LocalDate.of(2026, 11, 2);
    private static final LocalDate T3 = LocalDate.of(2026, 11, 3);
    private static final LocalDate T4 = LocalDate.of(2026, 11, 5);
    private static final LocalDate T5 = LocalDate.of(2026, 11, 15);
    private static JFrame frame;
    private static int gagal;

    public static void main(String[] args) throws Exception {
        Path database = Path.of("data/riwayat-feature.db");
        Files.deleteIfExists(database);
        seed(database.toString());
        JendelaKantinFlow[] aplikasi = new JendelaKantinFlow[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                aplikasi[0] = new JendelaKantinFlow(
                        new PenyimpananSQLite(database.toString()));
                frame = aplikasi[0];
                frame.setSize(1180, 720);
                frame.setLocationRelativeTo(null);
                aplikasi[0].tampilkan();
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
        Thread.sleep(500);
        System.out.println("START_ACTIVE="
                + aplikasi[0].getBarangAktif().getNama()
                + " RIWAYAT=" + aplikasi[0].getRiwayat().size());
        pilihBarang("Roti");
        SwingUtilities.invokeAndWait(() -> ((JToggleButton) find(
                frame.getContentPane(), "nav-riwayat")).doClick());
        Thread.sleep(200);

        check("AWAL_COUNT", rows() == 6 && count().contains("5 transaksi")
                && count().contains("6 rincian"), count() + " rows=" + rows());

        cari("rusak");
        terapkan();
        check("CARI_ALASAN_CASE", rows() == 1 && count().contains("1 transaksi")
                && count().contains("1 rincian"), count() + " rows=" + rows());

        cari("ROTI-exp");
        terapkan();
        check("CARI_BATCH_CASE", rows() == 2, count() + " rows=" + rows());

        reset();
        pilihJenis("Penerimaan");
        isi("tanggalAwalRiwayat", "2026-11-02");
        isi("tanggalAkhirRiwayat", "2026-11-02");
        terapkan();
        check("FILTER_TANGGAL_INKLUSIF", rows() == 1
                && count().contains("1 transaksi"), count() + " rows=" + rows());

        isi("tanggalAwalRiwayat", "2026-11-20");
        isi("tanggalAkhirRiwayat", "2026-11-01");
        terapkan();
        String pesan = label("pesanFilterRiwayat");
        check("RENTANG_INVALID", pesan.contains("tidak boleh setelah"), pesan);

        reset();
        cari("zzz");
        terapkan();
        Component kosongKomponen = find(frame.getContentPane(), "kosongRiwayat");
        String teksKosong = kosongKomponen instanceof kantinflow.gui.EmptyState kosong
                ? kosong.getText()
                : (kosongKomponen instanceof JLabel label ? label.getText() : "");
        check("TANPA_HASIL", rows() == 0 && kosongKomponen != null
                && kosongKomponen.isVisible()
                && teksKosong.contains("Tidak ada riwayat"),
                rows() + " " + teksKosong);

        reset();
        pilihJenis("Pembuangan");
        cari("rusak");
        isi("tanggalAwalRiwayat", "2026-11-15");
        isi("tanggalAkhirRiwayat", "2026-11-15");
        terapkan();
        check("FILTER_GABUNGAN", rows() == 1 && count().contains("1 transaksi"),
                count() + " rows=" + rows());

        reset();
        check("RESET", rows() == 6 && count().contains("5 transaksi")
                && count().contains("6 rincian"), count() + " rows=" + rows());

        pilihBarang("Susu");
        check("PINDAH_BARANG_RESET_FILTER",
                ((JTextField) find(frame.getContentPane(), "cariRiwayat"))
                        .getText().isEmpty() && rows() == 1,
                count() + " rows=" + rows());
        pilihBarang("Roti");
        reset();
        pilihJenis("Pengeluaran FEFO");
        terapkan();
        check("FEFO_SATU_TRANSAKSI", rows() == 2
                && count().contains("1 transaksi")
                && count().contains("2 rincian"), count() + " rows=" + rows());
        Object[] detail = bukaDetail(0);
        check("FEFO_DETAIL_DUA_RINCIAN", Integer.valueOf(2).equals(detail[0]),
                "detailRows=" + detail[0] + " title=" + detail[3]);
        check("FEFO_DETAIL_NILAI", String.valueOf(detail[1]).contains("ROTI-A")
                && String.valueOf(detail[2]).contains("5"),
                detail[1] + " / " + detail[2]);

        cari("ROTI-B");
        terapkan();
        check("FEFO_FILTER_BATCH_SATU_BARIS", rows() == 1
                && count().contains("1 transaksi")
                && count().contains("1 rincian"), count() + " rows=" + rows());
        Object[] detailBatch = bukaDetail(0);
        check("FEFO_DETAIL_TETAP_LENGKAP", Integer.valueOf(2).equals(detailBatch[0]),
                "detailRows=" + detailBatch[0]);

        reset();
        pilihJenis("Pengeluaran FEFO");
        terapkan();
        SwingUtilities.invokeAndWait(() -> {
            tabel().getRowSorter().toggleSortOrder(0);
            tabel().setRowSelectionInterval(0, 0);
        });
        Thread.sleep(100);
        Object[] detailSorted = bukaDetail(0);
        check("DETAIL_SETELAH_SORT", Integer.valueOf(2).equals(detailSorted[0]),
                "detailRows=" + detailSorted[0]);
        check("FILTER_DETAIL_TIDAK_UBAH_SALDO",
                aplikasi[0].getBarangAktif().hitungSaldo() == 5,
                "saldo=" + aplikasi[0].getBarangAktif().hitungSaldo());
        check("FILTER_DETAIL_TIDAK_UBAH_TRANSAKSI",
                aplikasi[0].getRiwayat().size() == 5,
                "riwayat=" + aplikasi[0].getRiwayat().size());

        reset();
        cari("ROTI-NEW");
        terapkan();
        check("FILTER_SEBELUM_TRANSAKSI", rows() == 0, "rows=" + rows());
        SwingUtilities.invokeAndWait(() -> ((JToggleButton) find(
                frame.getContentPane(), "nav-stok-masuk")).doClick());
        isi("kodeBatch", "ROTI-NEW");
        isi("jumlah", "1");
        isi("tanggalMasuk", "2026-11-05");
        isi("tanggalKedaluwarsa", "2026-12-05");
        SwingUtilities.invokeAndWait(() -> ((JButton) find(
                frame.getContentPane(), "tombolSimpan")).doClick());
        Thread.sleep(200);
        SwingUtilities.invokeAndWait(() -> ((JToggleButton) find(
                frame.getContentPane(), "nav-riwayat")).doClick());
        Thread.sleep(200);
        check("FILTER_TETAP_SETELAH_TRANSAKSI",
                "ROTI-NEW".equals(((JTextField) find(
                        frame.getContentPane(), "cariRiwayat")).getText())
                && rows() == 1 && count().contains("1 transaksi")
                && count().contains("1 rincian"), count() + " rows=" + rows());

        try (PenyimpananSQLite db = new PenyimpananSQLite(database.toString())) {
            Barang roti = null;
            for (Barang item : db.muatSemuaBarang()) {
                if ("Roti".equals(item.getNama())) {
                    roti = item;
                }
            }
            check("FILTER_TIDAK_UBAH_STOK",
                    roti != null && roti.hitungSaldo() == 6,
                    roti == null ? "no roti" : "saldo=" + roti.hitungSaldo());
            check("FILTER_TIDAK_UBAH_TRANSAKSI",
                    roti != null && db.muatRiwayat(roti).size() == 6,
                    roti == null ? "no roti"
                            : "riwayat=" + db.muatRiwayat(roti).size());
        }

        cekUkuran(1366, 768, "1366x768");
        cekUkuran(960, 640, "960x640");
        SwingUtilities.invokeAndWait(() -> aplikasi[0].tutup());
        Files.deleteIfExists(database);
        Files.deleteIfExists(database.resolveSibling(
                database.getFileName() + ".ui"));
        System.out.println(gagal == 0
                ? "RIWAYAT_HARNESS_LULUS" : "RIWAYAT_HARNESS_GAGAL=" + gagal);
        System.exit(gagal == 0 ? 0 : 1);
    }

    public static void seed(String path) {
        try (PenyimpananSQLite db = new PenyimpananSQLite(path)) {
            Barang roti = db.simpanBarang(new Barang("Roti", "bungkus"));
            PenerimaanStok a = new PenerimaanStok(T1);
            a.terima(roti, "ROTI-A", 5, T1, LocalDate.of(2026, 11, 20));
            db.simpanTransaksi(roti, a);
            PenerimaanStok b = new PenerimaanStok(T2);
            b.terima(roti, "ROTI-B", 7, T2, LocalDate.of(2026, 11, 25));
            db.simpanTransaksi(roti, b);
            PengeluaranStok fefo = new PengeluaranStok(T4);
            fefo.keluarkanFefo(roti, 8, T4);
            db.simpanTransaksi(roti, fefo);
            PenerimaanStok exp = new PenerimaanStok(T3);
            exp.terima(roti, "ROTI-EXP", 2, T3, LocalDate.of(2026, 11, 10));
            db.simpanTransaksi(roti, exp);
            BatchStok batchExp = null;
            for (BatchStok batch : roti.getDaftarBatch()) {
                if ("ROTI-EXP".equals(batch.getKode())) {
                    batchExp = batch;
                }
            }
            PembuanganStok buang = new PembuanganStok(T5);
            buang.buang(roti, batchExp, 1, "Rusak");
            db.simpanTransaksi(roti, buang);
            Barang susu = db.simpanBarang(new Barang("Susu", "kotak"));
            PenerimaanStok masukSusu = new PenerimaanStok(T3);
            masukSusu.terima(susu, "SUSU-A", 3, T3,
                    LocalDate.of(2026, 12, 10));
            db.simpanTransaksi(susu, masukSusu);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private static JTable tabel() {
        return (JTable) find(frame.getContentPane(), "tabelRiwayat");
    }

    private static int rows() {
        return tabel().getRowCount();
    }

    private static String count() {
        return label("jumlahRiwayat");
    }

    private static String label(String name) {
        return ((JLabel) find(frame.getContentPane(), name)).getText();
    }

    private static void cari(String value) {
        isi("cariRiwayat", value);
    }

    private static void isi(String name, String value) {
        ((JTextField) find(frame.getContentPane(), name)).setText(value);
    }

    private static void pilihJenis(String value) {
        ((JComboBox<?>) find(frame.getContentPane(), "jenisRiwayat"))
                .setSelectedItem(value);
    }

    private static void terapkan() {
        ((JButton) find(frame.getContentPane(), "terapkanFilterRiwayat")).doClick();
    }

    private static void reset() {
        ((JButton) find(frame.getContentPane(), "resetFilterRiwayat")).doClick();
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
                    break;
                }
            }
        });
        Thread.sleep(250);
    }

    private static Object[] bukaDetail(int row) throws Exception {
        Object[] hasil = new Object[4];
        SwingUtilities.invokeAndWait(() -> {
            tabel().setRowSelectionInterval(row, row);
            JButton tombol = (JButton) find(
                    frame.getContentPane(), "detailRiwayat");
            Timer timer = new Timer(80, event -> {
                for (Window window : Window.getWindows()) {
                    if (window instanceof JDialog dialog && dialog.isShowing()
                            && "dialogDetailRiwayat".equals(dialog.getName())) {
                        JTable detail = (JTable) find(
                                dialog.getContentPane(), "tabelDetailRiwayat");
                        hasil[0] = detail == null ? 0 : detail.getRowCount();
                        hasil[1] = detail != null && detail.getRowCount() > 0
                                ? detail.getValueAt(0, 0) : "";
                        hasil[2] = detail != null && detail.getRowCount() > 0
                                ? detail.getValueAt(0, 1) : "";
                        hasil[3] = dialog.getTitle();
                        dialog.dispose();
                        ((Timer) event.getSource()).stop();
                    }
                }
            });
            timer.start();
            tombol.doClick();
        });
        Thread.sleep(150);
        return hasil;
    }

    private static void cekUkuran(int lebar, int tinggi, String label)
            throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            frame.setSize(lebar, tinggi);
            frame.validate();
        });
        Thread.sleep(200);
        String[][] halaman = {
            {"ringkasan", "PanelRingkasan"},
            {"barang", "PanelBarang"},
            {"stok-masuk", "PanelStokMasuk"},
            {"stok-keluar", "PanelStokKeluar"},
            {"pemantauan", "PanelPemantauan"},
            {"pembuangan", "PanelPembuangan"},
            {"riwayat", "PanelRiwayat"}
        };
        for (String[] item : halaman) {
            SwingUtilities.invokeAndWait(() -> ((JToggleButton) find(
                    frame.getContentPane(), "nav-" + item[0])).doClick());
            Thread.sleep(100);
            JComponent panel = findPage(frame.getContentPane(), item[1]);
            check("SIZE_" + label + "_PAGE_OK=" + item[0],
                    panel != null && panel.isShowing(),
                    panel == null ? "panel null" : "showing");
        }
    }

    private static JComponent findPage(Container root, String simpleName) {
        for (Component component : root.getComponents()) {
            if (component.getClass().getSimpleName().equals(simpleName)
                    && component instanceof JComponent jcomponent) {
                return jcomponent;
            }
            if (component instanceof Container container) {
                JComponent found = findPage(container, simpleName);
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
