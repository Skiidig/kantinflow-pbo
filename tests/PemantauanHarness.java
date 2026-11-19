import java.awt.Component;
import java.awt.Container;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import kantinflow.Barang;
import kantinflow.BatchStok;
import kantinflow.PenerimaanStok;
import kantinflow.PengeluaranStok;
import kantinflow.Transaksi;
import kantinflow.gui.JendelaKantinFlow;
import kantinflow.gui.KonteksKantin;
import kantinflow.gui.PanelPemantauan;
import kantinflow.penyimpanan.PenyimpananSQLite;

/**
 * Uji perilaku Pemantauan otomatis dan status batch.
 * Jalankan setelah src/main/java dikompilasi ke out.
 */
public class PemantauanHarness {
    private static final LocalDate HARI = LocalDate.now();
    private static JFrame frame;
    private static JendelaKantinFlow aplikasi;
    private static int gagal;

    public static void main(String[] args) throws Exception {
        Path database = Path.of("data/pemantauan-otomatis.db");
        Files.deleteIfExists(database);
        Files.deleteIfExists(database.resolveSibling(
                database.getFileName() + ".ui"));
        seed(database.toString());

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
        Thread.sleep(300);

        int saldoAwal = aplikasi.getBarangAktif().hitungSaldo();
        int riwayatAwal = aplikasi.getRiwayat().size();
        pilihHalaman("pemantauan");
        Thread.sleep(300);

        check("PEMANTAUAN_TERBUKA_TANPA_BARANG",
                aplikasi.getBarangAktif() != null,
                "barang=" + namaAktif());
        check("TANGGAL_AWAL_HARI_INI",
                HARI.toString().equals(field("tanggalPemantauan")),
                field("tanggalPemantauan"));
        check("LANGSUNG_ADA_TABEL", rowsPemantauan() == 3,
                "rows=" + rowsPemantauan());
        check("TIDAK_MENAMPILKAN_KOSONG",
                !tampil(find(frame.getContentPane(), "kosongPemantauan"))
                        && !tampil(find(frame.getContentPane(),
                                "kosongBarangPemantauan")),
                "kosong=" + tampil(find(frame.getContentPane(),
                        "kosongPemantauan")));
        check("SEMUA_BATCH_BERSISA_TAMPIL",
                memuatKode("A-AMAN") && memuatKode("A-SEGERA")
                        && memuatKode("A-EXP"),
                kodeTabel());
        check("BATCH_NOL_DISEMBUNYIKAN", !memuatKode("A-HABIS"), kodeTabel());
        check("LABEL_BUKAN_BELUM_DIPERIKSA",
                !label("infoPemantauan").contains("Belum diperiksa"),
                label("infoPemantauan"));
        check("RINGKASAN_SESUAI_TANGGAL",
                (saldoAwal + " bungkus").equals(label("fisikPemantauan"))
                        && (aplikasi.getBarangAktif().hitungStokLayak(HARI)
                                + " bungkus").equals(label("layakPemantauan")),
                label("fisikPemantauan") + " / " + label("layakPemantauan"));
        check("BUKA_PEMANTAUAN_TIDAK_UBAH_STOK",
                saldoAwal == aplikasi.getBarangAktif().hitungSaldo()
                        && riwayatAwal == aplikasi.getRiwayat().size(),
                "saldo=" + aplikasi.getBarangAktif().hitungSaldo()
                        + " riwayat=" + aplikasi.getRiwayat().size());

        pilihHalaman("stok-masuk");
        Thread.sleep(250);
        check("STOK_MASUK_HABIS",
                "Habis".equals(statusBatch("A-HABIS")),
                "A-HABIS=" + statusBatch("A-HABIS"));
        check("STOK_MASUK_KEDALUWARSA",
                "Kedaluwarsa".equals(statusBatch("A-EXP")),
                "A-EXP=" + statusBatch("A-EXP"));
        check("STOK_MASUK_AMAN",
                "Aman".equals(statusBatch("A-AMAN")),
                "A-AMAN=" + statusBatch("A-AMAN"));
        check("STOK_MASUK_SEGERA",
                "Segera kedaluwarsa".equals(statusBatch("A-SEGERA")),
                "A-SEGERA=" + statusBatch("A-SEGERA"));

        pilihHalaman("pemantauan");
        String tanggalLain = HARI.plusDays(1).toString();
        isi("tanggalPemantauan", tanggalLain);
        klik("tombolPeriksa");
        Thread.sleep(300);
        check("TANGGAL_PILIHAN_TERSIMPAN",
                tanggalLain.equals(String.valueOf(
                        aplikasi.getTanggalPemantauan())),
                "tanggal=" + aplikasi.getTanggalPemantauan());

        pilihHalaman("riwayat");
        Thread.sleep(250);
        pilihHalaman("pemantauan");
        check("TANGGAL_TETAP_SETELAH_HALAMAN",
                tanggalLain.equals(field("tanggalPemantauan"))
                        && rowsPemantauan() == 3,
                field("tanggalPemantauan") + " rows=" + rowsPemantauan());

        pilihBarang("Susu");
        Thread.sleep(300);
        check("GANTI_BARANG_TIDAK_RESET_TANGGAL",
                tanggalLain.equals(field("tanggalPemantauan")),
                field("tanggalPemantauan"));
        check("BARANG_TANPA_BATCH_BERSISA",
                rowsPemantauan() == 0 && tampil(find(
                        frame.getContentPane(), "kosongPemantauan"))
                        && !tampil(tabelPemantauan()),
                "rows=" + rowsPemantauan() + " kosong="
                        + tampil(find(frame.getContentPane(),
                                "kosongPemantauan")));

        pilihBarang("Roti");
        Thread.sleep(300);
        check("KEMBALI_KE_BARANG_BERSISA",
                tanggalLain.equals(field("tanggalPemantauan"))
                        && rowsPemantauan() == 3,
                field("tanggalPemantauan") + " rows=" + rowsPemantauan());

        int sebelum = rowsPemantauan();
        String infoSebelum = label("infoPemantauan");
        isi("tanggalPemantauan", "2026-02-30");
        klik("tombolPeriksa");
        Thread.sleep(300);
        check("TANGGAL_INVALID_PESAN",
                label("pesanPemantauan").contains("yyyy-MM-dd"),
                label("pesanPemantauan"));
        check("TANGGAL_INVALID_TIDAK_MENGHAPUS_HASIL",
                rowsPemantauan() == sebelum
                        && infoSebelum.equals(label("infoPemantauan")),
                "rows=" + rowsPemantauan() + " info="
                        + label("infoPemantauan"));

        LocalDate tanggalKedaluwarsa = HARI.minusDays(1);
        isi("tanggalPemantauan", tanggalKedaluwarsa.toString());
        klik("tombolPeriksa");
        Thread.sleep(300);
        check("BATCH_KEDALUWARSA_HARI_INI",
                rowsPemantauan() == 3
                        && "Segera kedaluwarsa".equals(statusPemantauan("A-EXP")),
                statusPemantauan("A-EXP"));
        int layak = aplikasi.getBarangAktif().hitungStokLayak(
                tanggalKedaluwarsa);
        check("RINGKASAN_TANGGAL_ACUAN",
                (layak + " bungkus").equals(label("layakPemantauan")),
                label("layakPemantauan") + " expected=" + layak);

        pilihHalaman("ringkasan");
        Thread.sleep(250);
        pilihHalaman("pemantauan");
        check("BUKA_HALAMAN_TIDAK_UBAH_DATA",
                saldoAwal == aplikasi.getBarangAktif().hitungSaldo()
                        && riwayatAwal == aplikasi.getRiwayat().size(),
                "saldo=" + aplikasi.getBarangAktif().hitungSaldo()
                        + " riwayat=" + aplikasi.getRiwayat().size());

        SwingUtilities.invokeAndWait(() -> aplikasi.tutup());
        ujiTanpaBarang();
        Files.deleteIfExists(database);
        Files.deleteIfExists(database.resolveSibling(
                database.getFileName() + ".ui"));
        System.out.println(gagal == 0
                ? "PEMANTAUAN_HARNESS_LULUS"
                : "PEMANTAUAN_HARNESS_GAGAL=" + gagal);
        System.exit(gagal == 0 ? 0 : 1);
    }

    public static void seed(String path) {
        try (PenyimpananSQLite db = new PenyimpananSQLite(path)) {
            Barang roti = db.simpanBarang(new Barang("Roti", "bungkus"));
            Barang susu = db.simpanBarang(new Barang("Susu", "kotak"));

            terima(db, roti, "A-AMAN", 5, HARI.minusDays(15),
                    HARI.plusDays(20));
            terima(db, roti, "A-SEGERA", 3, HARI.minusDays(12),
                    HARI.plusDays(2));
            terima(db, roti, "A-EXP", 2, HARI.minusDays(10),
                    HARI.minusDays(1));
            terima(db, roti, "A-HABIS", 1, HARI.minusDays(9),
                    HARI.plusDays(10));

            PengeluaranStok habisRoti = new PengeluaranStok(HARI.minusDays(2));
            habisRoti.keluarkan(roti, batch(roti, "A-HABIS"), 1);
            db.simpanTransaksi(roti, habisRoti);

            terima(db, susu, "B-HABIS", 1, HARI.minusDays(20),
                    HARI.plusDays(10));
            PengeluaranStok habisSusu = new PengeluaranStok(HARI.minusDays(3));
            habisSusu.keluarkan(susu, batch(susu, "B-HABIS"), 1);
            db.simpanTransaksi(susu, habisSusu);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private static void terima(PenyimpananSQLite db, Barang barang,
            String kode, int jumlah, LocalDate masuk, LocalDate kedaluwarsa) {
        PenerimaanStok penerimaan = new PenerimaanStok(masuk);
        penerimaan.terima(barang, kode, jumlah, masuk, kedaluwarsa);
        db.simpanTransaksi(barang, penerimaan);
    }

    private static BatchStok batch(Barang barang, String kode) {
        for (BatchStok batch : barang.getDaftarBatch()) {
            if (kode.equals(batch.getKode())) {
                return batch;
            }
        }
        throw new IllegalStateException("Batch tidak ditemukan: " + kode);
    }

    private static void ujiTanpaBarang() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            KonteksKantin kosong = new KonteksKantin() {
                @Override
                public Barang getBarangAktif() {
                    return null;
                }

                @Override
                public List<Barang> getDaftarBarang() {
                    return List.of();
                }

                @Override
                public List<Transaksi> getRiwayat() {
                    return List.of();
                }

                @Override
                public LocalDate getTanggalPemantauan() {
                    return null;
                }

                @Override
                public void setTanggalPemantauan(LocalDate tanggal) {
                }

                @Override
                public boolean simpanTransaksi(Transaksi transaksi,
                        String pesanSukses) {
                    return false;
                }

                @Override
                public boolean tambahBarang(String nama, String satuan) {
                    return false;
                }

                @Override
                public boolean simpanStokMinimum(int minimum) {
                    return false;
                }

                @Override
                public boolean simpanStokMinimum(Barang barang, int minimum) {
                    return false;
                }

                @Override
                public void gantiBarang(Barang barang) {
                }

                @Override
                public boolean muatUlang() {
                    return true;
                }

                @Override
                public void tampilkanPesan(String pesan, boolean sukses) {
                }
            };
            PanelPemantauan panel = new PanelPemantauan(kosong);
            panel.perbarui();
            check("TANPA_BARANG_TIDAK_CRASH", true, "ok");
            check("TANPA_BARANG_PESAN",
                    tampil(find(panel, "kosongBarangPemantauan"))
                            && !tampil(find(panel, "tabelPemantauan")),
                    "kosong=" + tampil(find(panel,
                            "kosongBarangPemantauan")));
            check("TANPA_BARANG_TANGGAL_HARI_INI",
                    HARI.toString().equals(
                            ((JTextField) find(panel, "tanggalPemantauan"))
                                    .getText()),
                    ((JTextField) find(panel, "tanggalPemantauan")).getText());
        });
    }

    private static String namaAktif() {
        return aplikasi.getBarangAktif() == null ? "-"
                : aplikasi.getBarangAktif().getNama();
    }

    private static void pilihHalaman(String nama) throws Exception {
        SwingUtilities.invokeAndWait(() -> ((AbstractButton) find(
                frame.getContentPane(), "nav-" + nama)).doClick());
        Thread.sleep(300);
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

    private static JTable tabelPemantauan() {
        return (JTable) find(frame.getContentPane(), "tabelPemantauan");
    }

    private static int rowsPemantauan() {
        return tabelPemantauan().getRowCount();
    }

    private static boolean memuatKode(String kode) {
        return kodeTabel().contains(kode);
    }

    private static String kodeTabel() {
        StringBuilder hasil = new StringBuilder();
        for (int i = 0; i < rowsPemantauan(); i++) {
            hasil.append(tabelPemantauan().getValueAt(i, 0)).append(' ');
        }
        return hasil.toString();
    }

    private static String statusPemantauan(String kode) {
        for (int i = 0; i < rowsPemantauan(); i++) {
            if (kode.equals(String.valueOf(
                    tabelPemantauan().getValueAt(i, 0)))) {
                return String.valueOf(tabelPemantauan().getValueAt(i, 3));
            }
        }
        return "<tidak ada>";
    }

    private static String statusBatch(String kode) {
        JTable tabel = (JTable) find(frame.getContentPane(), "tabelBatch");
        for (int i = 0; i < tabel.getRowCount(); i++) {
            if (kode.equals(String.valueOf(tabel.getValueAt(i, 0)))) {
                return String.valueOf(tabel.getValueAt(i, 4));
            }
        }
        return "<tidak ada>";
    }

    private static String field(String nama) {
        return ((JTextField) find(frame.getContentPane(), nama)).getText();
    }

    private static String label(String nama) {
        return ((JLabel) find(frame.getContentPane(), nama)).getText();
    }

    private static void isi(String nama, String nilai) {
        ((JTextField) find(frame.getContentPane(), nama)).setText(nilai);
    }

    private static void klik(String nama) throws Exception {
        SwingUtilities.invokeAndWait(() -> ((JButton) find(
                frame.getContentPane(), nama)).doClick());
        Thread.sleep(150);
    }

    private static boolean visible(String nama) {
        Component component = find(frame.getContentPane(), nama);
        return component != null && component.isVisible();
    }

    private static boolean tampil(Component component) {
        Component sekarang = component;
        while (sekarang != null) {
            if (!sekarang.isVisible()) {
                return false;
            }
            sekarang = sekarang.getParent();
        }
        return component != null;
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

    private static void check(String nama, boolean lulus, String detail) {
        if (!lulus) {
            gagal++;
        }
        System.out.println((lulus ? "[OK] " : "[GAGAL] ") + nama
                + " :: " + detail);
    }
}
