import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

import kantinflow.Barang;
import kantinflow.BatchStok;
import kantinflow.PembuanganStok;
import kantinflow.PenerimaanStok;
import kantinflow.PengeluaranStok;
import kantinflow.RincianTransaksi;
import kantinflow.StokTidakCukupException;
import kantinflow.Transaksi;
import kantinflow.penyimpanan.PenyimpananException;
import kantinflow.penyimpanan.PenyimpananSQLite;

/**
 * Regression test tanpa framework, dijalankan dari akar repo:
 * javac -cp "out;lib/*" -d tests-out tests/RegresiKantinFlow.java
 * java --enable-native-access=ALL-UNNAMED -cp "tests-out;out;lib/*" RegresiKantinFlow
 */
public class RegresiKantinFlow {
    private static final LocalDate MASUK = LocalDate.of(2026, 11, 10);
    private static final LocalDate SEBELUM_MASUK = LocalDate.of(2026, 11, 9);
    private static final LocalDate KEDALUWARSA = LocalDate.of(2026, 12, 1);

    public static void main(String[] args) throws Exception {
        Path dbUtama = Paths.get("data", "regresi-kantinflow.db");
        Path dbRollback = Paths.get("data", "regresi-rollback.db");
        Path dbIsolasi = Paths.get("data", "regresi-isolasi.db");
        Files.createDirectories(dbUtama.getParent());
        Files.deleteIfExists(dbUtama);
        Files.deleteIfExists(dbRollback);
        Files.deleteIfExists(dbIsolasi);

        try {
            ujiDuplikatBatch();
            ujiTanggalSebelumMasuk();
            ujiPersistensiDanRiwayat(dbUtama);
            ujiRollback(dbRollback);
            ujiIsolasiBarang(dbIsolasi);
            System.out.println("SEMUA_REGRESI_LULUS");
        } finally {
            Files.deleteIfExists(dbUtama);
            Files.deleteIfExists(dbRollback);
            Files.deleteIfExists(dbIsolasi);
        }
    }

    private static void ujiDuplikatBatch() {
        Barang barang = new Barang("Uji", "pcs");
        PenerimaanStok pertama = new PenerimaanStok(MASUK);
        pertama.terima(barang, "DUP", 5, MASUK, KEDALUWARSA);
        PenerimaanStok kedua = new PenerimaanStok(MASUK);
        try {
            kedua.terima(barang, "dup", 2, MASUK, KEDALUWARSA);
            throw new AssertionError("Duplikat batch seharusnya ditolak");
        } catch (IllegalArgumentException exception) {
            cek(exception.getMessage().contains("sudah digunakan"),
                    "Pesan duplikat tidak jelas: " + exception.getMessage());
            cek(barang.jumlahBatch() == 1,
                    "Duplikat batch mengubah jumlah batch");
            System.out.println("LULUS duplikat: " + exception.getMessage());
        }
    }

    private static void ujiTanggalSebelumMasuk() {
        Barang barang = new Barang("Uji", "pcs");
        PenerimaanStok terima = new PenerimaanStok(MASUK);
        terima.terima(barang, "B1", 5, MASUK, KEDALUWARSA);

        try {
            new PengeluaranStok(SEBELUM_MASUK)
                    .keluarkan(barang, terima.getBatch(), 1);
            throw new AssertionError("Pengeluaran sebelum masuk seharusnya ditolak");
        } catch (IllegalArgumentException exception) {
            cek(exception.getMessage().contains("sebelum tanggal masuk"),
                    "Pesan tanggal manual tidak jelas");
            cek(barang.getDaftarBatch().get(0).getSisaJumlah() == 5,
                    "Pengeluaran manual mengubah sisa");
            System.out.println("LULUS tanggal manual: " + exception.getMessage());
        }

        try {
            new PengeluaranStok(SEBELUM_MASUK)
                    .keluarkanFefo(barang, 1, SEBELUM_MASUK);
            throw new AssertionError("FEFO sebelum masuk seharusnya ditolak");
        } catch (IllegalArgumentException exception) {
            cek(exception.getMessage().contains("sebelum tanggal masuk"),
                    "Pesan tanggal FEFO tidak jelas");
            cek(barang.getDaftarBatch().get(0).getSisaJumlah() == 5,
                    "FEFO mengubah sisa");
            System.out.println("LULUS tanggal FEFO: " + exception.getMessage());
        }

        try {
            new PembuanganStok(SEBELUM_MASUK)
                    .buang(barang, terima.getBatch(), 1, "Rusak");
            throw new AssertionError("Pembuangan sebelum masuk seharusnya ditolak");
        } catch (IllegalArgumentException exception) {
            cek(exception.getMessage().contains("sebelum tanggal masuk"),
                    "Pesan tanggal pembuangan tidak jelas");
            cek(barang.getDaftarBatch().get(0).getSisaJumlah() == 5,
                    "Pembuangan mengubah sisa");
            System.out.println("LULUS tanggal pembuangan: " + exception.getMessage());
        }
    }

    private static void ujiPersistensiDanRiwayat(Path db) {
        LocalDate masuk = LocalDate.of(2026, 11, 1);
        try (PenyimpananSQLite penyimpanan = new PenyimpananSQLite(db.toString())) {
            Barang susu = cari(penyimpanan.muatSemuaBarang(), "Susu Kotak");
            Barang roti = penyimpanan.simpanBarang(new Barang("Roti", "bungkus"));
            cek(susu.getId() > 0 && roti.getId() > 0, "ID barang tidak tersimpan");

            PenerimaanStok roti1 = new PenerimaanStok(masuk);
            roti1.terima(roti, "ROTI-1", 10, masuk,
                    LocalDate.of(2026, 12, 15));
            penyimpanan.simpanTransaksi(roti, roti1);

            BatchStok batch1 = roti1.getBatch();
            PengeluaranStok keluar = new PengeluaranStok(LocalDate.of(2026, 11, 18));
            keluar.keluarkan(roti, batch1, 3);
            penyimpanan.simpanTransaksi(roti, keluar);

            PenerimaanStok roti2 = new PenerimaanStok(masuk);
            roti2.terima(roti, "ROTI-2", 7, masuk,
                    LocalDate.of(2026, 12, 20));
            penyimpanan.simpanTransaksi(roti, roti2);

            PengeluaranStok fefo = new PengeluaranStok(LocalDate.of(2026, 11, 18));
            fefo.keluarkanFefo(roti, 10, LocalDate.of(2026, 11, 18));
            penyimpanan.simpanTransaksi(roti, fefo);
            penyimpanan.simpanStokMinimum(roti, 5);

            cek(roti1.getJumlah() == 10,
                    "Jumlah historis penerimaan berubah: " + roti1.getJumlah());
            cek(fefo.getRincian().size() == 2, "FEFO tidak memiliki dua rincian");
            System.out.println("LULUS simpan: saldo=" + roti.hitungSaldo()
                    + ", historis penerimaan=" + roti1.getJumlah()
                    + ", rincian FEFO=" + fefo.getRincian().size());
        }

        try (PenyimpananSQLite penyimpanan = new PenyimpananSQLite(db.toString())) {
            List<Barang> daftar = penyimpanan.muatSemuaBarang();
            Barang roti = cari(daftar, "Roti");
            cek(roti.getStokMinimum() == 5, "Stok minimum tidak bertahan");
            cek(roti.hitungSaldo() == 4, "Saldo Roti setelah reload salah");
            List<Transaksi> riwayat = penyimpanan.muatRiwayat(roti);
            PenerimaanStok penerimaan = cariTransaksi(riwayat, "Penerimaan");
            PengeluaranStok fefo = cariPengeluaranFefo(riwayat);
            cek(penerimaan.getJumlah() == 10,
                    "Jumlah historis penerimaan setelah reload salah");
            cek(fefo.getRincian().size() == 2,
                    "Rincian FEFO setelah reload tidak lengkap");
            cek(riwayat.size() == 4, "Jumlah transaksi Roti setelah reload salah");
            System.out.println("LULUS reload: min=" + roti.getStokMinimum()
                    + ", saldo=" + roti.hitungSaldo()
                    + ", transaksi=" + riwayat.size()
                    + ", rincianFEFO=" + fefo.getRincian().size());
        }
    }

    private static void ujiRollback(Path db) throws Exception {
        try (PenyimpananSQLite penyimpanan = new PenyimpananSQLite(db.toString())) {
            Barang susu = penyimpanan.muatBarang();
            PenerimaanStok masuk = new PenerimaanStok(MASUK);
            masuk.terima(susu, "ROLLBACK", 5, MASUK, KEDALUWARSA);
            penyimpanan.simpanTransaksi(susu, masuk);
        }

        try (Connection koneksi = DriverManager.getConnection("jdbc:sqlite:" + db);
                Statement statement = koneksi.createStatement()) {
            statement.executeUpdate(
                    "CREATE TRIGGER gagal_transaksi BEFORE INSERT ON transaksi "
                    + "BEGIN SELECT RAISE(ABORT, 'uji rollback'); END");
        }

        try (PenyimpananSQLite penyimpanan = new PenyimpananSQLite(db.toString())) {
            Barang susu = penyimpanan.muatBarang();
            BatchStok batch = susu.getDaftarBatch().get(0);
            PengeluaranStok keluar = new PengeluaranStok(LocalDate.of(2026, 11, 18));
            keluar.keluarkan(susu, batch, 1);
            try {
                penyimpanan.simpanTransaksi(susu, keluar);
                throw new AssertionError("Simpan dengan trigger seharusnya gagal");
            } catch (PenyimpananException exception) {
                cek(exception.getMessage().contains("Gagal menyimpan"),
                        "Pesan rollback tidak jelas");
            }
            Barang setelah = penyimpanan.muatBarang();
            cek(setelah.getDaftarBatch().get(0).getSisaJumlah() == 5,
                    "Rollback tidak mengembalikan sisa batch");
            cek(penyimpanan.muatRiwayat(setelah).size() == 1,
                    "Rollback meninggalkan transaksi");
            System.out.println("LULUS rollback: sisa="
                    + setelah.getDaftarBatch().get(0).getSisaJumlah()
                    + ", riwayat=" + penyimpanan.muatRiwayat(setelah).size());
        }
    }

    private static void ujiIsolasiBarang(Path db) {
        LocalDate masuk = LocalDate.of(2026, 11, 10);
        try (PenyimpananSQLite penyimpanan = new PenyimpananSQLite(db.toString())) {
            Barang susu = penyimpanan.muatBarang();
            Barang roti = penyimpanan.simpanBarang(new Barang("Roti", "bungkus"));
            PenerimaanStok susuMasuk = new PenerimaanStok(masuk);
            susuMasuk.terima(susu, "SUSU-ISO", 3, masuk,
                    LocalDate.of(2026, 12, 1));
            penyimpanan.simpanTransaksi(susu, susuMasuk);
            PenerimaanStok rotiMasuk = new PenerimaanStok(masuk);
            rotiMasuk.terima(roti, "ROTI-ISO", 2, masuk,
                    LocalDate.of(2026, 12, 1));
            penyimpanan.simpanTransaksi(roti, rotiMasuk);
        }

        try (PenyimpananSQLite penyimpanan = new PenyimpananSQLite(db.toString())) {
            Barang susu = cari(penyimpanan.muatSemuaBarang(), "Susu Kotak");
            Barang roti = cari(penyimpanan.muatSemuaBarang(), "Roti");
            cek(susu.hitungSaldo() == 3, "Saldo Susu tercampur");
            cek(roti.hitungSaldo() == 2, "Saldo Roti salah");
            cek(penyimpanan.muatRiwayat(susu).size() == 1,
                    "Riwayat Susu tercampur");
            cek(penyimpanan.muatRiwayat(roti).size() == 1,
                    "Riwayat Roti salah");
            try {
                new PengeluaranStok(LocalDate.of(2026, 11, 18))
                        .keluarkan(susu, roti.getDaftarBatch().get(0), 1);
                throw new AssertionError("Batch Roti diterima oleh Susu");
            } catch (IllegalArgumentException exception) {
                cek(exception.getMessage().contains("bukan milik barang"),
                        "Pesan kepemilikan domain tidak jelas");
            }
            PengeluaranStok cross = PengeluaranStok.dariPenyimpanan(
                    LocalDate.of(2026, 11, 18),
                    List.of(new RincianTransaksi(roti.getDaftarBatch().get(0), 1)),
                    false);
            try {
                penyimpanan.simpanTransaksi(susu, cross);
                throw new AssertionError("Penyimpanan menerima batch Roti untuk Susu");
            } catch (PenyimpananException exception) {
                cek(exception.getMessage().contains("bukan milik barang"),
                        "Pesan kepemilikan penyimpanan tidak jelas");
            }
            System.out.println("LULUS isolasi: Susu=3, Roti=2, batch/riwayat terpisah");
        }
    }

    private static Barang cari(List<Barang> daftar, String nama) {
        for (Barang barang : daftar) {
            if (barang.getNama().equals(nama)) {
                return barang;
            }
        }
        throw new AssertionError("Barang tidak ditemukan: " + nama);
    }

    private static PenerimaanStok cariTransaksi(List<Transaksi> riwayat, String jenis) {
        for (Transaksi transaksi : riwayat) {
            if (transaksi instanceof PenerimaanStok penerimaan
                    && transaksi.getJenis().equals(jenis)) {
                return penerimaan;
            }
        }
        throw new AssertionError("Penerimaan tidak ditemukan");
    }

    private static PengeluaranStok cariPengeluaranFefo(List<Transaksi> riwayat) {
        for (Transaksi transaksi : riwayat) {
            if (transaksi instanceof PengeluaranStok pengeluaran
                    && "Pengeluaran FEFO".equals(transaksi.getJenis())) {
                return pengeluaran;
            }
        }
        throw new AssertionError("Pengeluaran FEFO tidak ditemukan");
    }

    private static void cek(boolean kondisi, String pesan) {
        if (!kondisi) {
            throw new AssertionError(pesan);
        }
    }
}
