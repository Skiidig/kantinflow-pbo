import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import kantinflow.Barang;
import kantinflow.BatchStok;
import kantinflow.PembuanganStok;
import kantinflow.PenerimaanStok;
import kantinflow.PengeluaranStok;
import kantinflow.RincianTransaksi;
import kantinflow.Transaksi;
import kantinflow.penyimpanan.PenyimpananSQLite;

/**
 * Mengganti data aplikasi dengan simulasi kantin kampus. Saldo hanya dibentuk
 * melalui transaksi domain dan repository.
 */
public final class IsiDataKantin {
    private IsiDataKantin() {
    }

    public static void main(String[] args) throws Exception {
        Path lokasi = Paths.get(args.length > 0
                ? args[0] : "data/kantinflow.db");
        try (PenyimpananSQLite repo = new PenyimpananSQLite(
                lokasi.toString())) {
            List<Barang> lama = repo.muatSemuaBarang();
            if (sudahDiganti(lama)) {
                System.out.println("[LEWATI] Data simulasi sudah ada.");
                laporkan(repo);
                return;
            }

            kosongkanData(lokasi);
            isi(repo);
            laporkan(repo);
        }
    }

    private static boolean sudahDiganti(List<Barang> semua) {
        if (semua.size() < 15) {
            return false;
        }
        String[] nama = {
            "Nasi Ayam", "Nasi Telur", "Nasi Goreng", "Mi Goreng",
            "Roti Isi", "Risoles", "Pastel", "Air Mineral", "Teh Botol",
            "Susu UHT", "Kopi Susu Kemasan", "Minuman Isotonik",
            "Keripik Singkong", "Biskuit", "Kacang Panggang"
        };
        for (String target : nama) {
            boolean ditemukan = false;
            for (Barang barang : semua) {
                if (target.equals(barang.getNama())
                        && barang.jumlahBatch() > 0) {
                    ditemukan = true;
                    break;
                }
            }
            if (!ditemukan) {
                return false;
            }
        }
        return true;
    }

    private static void kosongkanData(Path lokasi) throws Exception {
        Class.forName("org.sqlite.JDBC");
        try (Connection koneksi = DriverManager.getConnection(
                "jdbc:sqlite:" + lokasi.toAbsolutePath())) {
            try (Statement perintah = koneksi.createStatement()) {
                perintah.execute("PRAGMA foreign_keys = ON");
            }
            boolean autoCommit = koneksi.getAutoCommit();
            koneksi.setAutoCommit(false);
            try (Statement perintah = koneksi.createStatement()) {
                perintah.executeUpdate("DELETE FROM rincian_transaksi");
                perintah.executeUpdate("DELETE FROM transaksi");
                perintah.executeUpdate("DELETE FROM batch");
                perintah.executeUpdate("DELETE FROM barang");
                koneksi.commit();
            } catch (Exception exception) {
                koneksi.rollback();
                throw exception;
            } finally {
                koneksi.setAutoCommit(autoCommit);
            }
        }
        System.out.println("[OK] Data lama dikosongkan.");
    }

    private static void isi(PenyimpananSQLite repo) {
        LocalDate t = LocalDate.now();
        Map<String, Barang> b = new LinkedHashMap<>();
        b.put("nasiAyam", barang(repo, "Nasi Ayam", "porsi", 10));
        b.put("nasiTelur", barang(repo, "Nasi Telur", "porsi", 8));
        b.put("nasiGoreng", barang(repo, "Nasi Goreng", "porsi", 8));
        b.put("miGoreng", barang(repo, "Mi Goreng", "porsi", 6));
        b.put("rotiIsi", barang(repo, "Roti Isi", "buah", 10));
        b.put("risoles", barang(repo, "Risoles", "buah", 12));
        b.put("pastel", barang(repo, "Pastel", "buah", 12));
        b.put("air", barang(repo, "Air Mineral", "botol", 24));
        b.put("teh", barang(repo, "Teh Botol", "botol", 12));
        b.put("susu", barang(repo, "Susu UHT", "kotak", 10));
        b.put("kopi", barang(repo, "Kopi Susu Kemasan", "botol", 8));
        b.put("isotonik", barang(repo, "Minuman Isotonik", "botol", 10));
        b.put("keripik", barang(repo, "Keripik Singkong", "bungkus", 8));
        b.put("biskuit", barang(repo, "Biskuit", "bungkus", 6));
        b.put("kacang", barang(repo, "Kacang Panggang", "bungkus", 6));

        String nasiAyamA = terima(repo, b.get("nasiAyam"),
                t.minusDays(10), t.plusDays(1), 30);
        String nasiAyamB = terima(repo, b.get("nasiAyam"),
                t.minusDays(4), t.plusDays(3), 20);
        fefo(repo, b.get("nasiAyam"), t.minusDays(2), 20);
        keluar(repo, b.get("nasiAyam"), nasiAyamB, t.minusDays(1), 5);

        String nasiTelurA = terima(repo, b.get("nasiTelur"),
                t.minusDays(12), t.plusDays(2), 24);
        String nasiTelurB = terima(repo, b.get("nasiTelur"),
                t.minusDays(5), t.plusDays(4), 18);
        fefo(repo, b.get("nasiTelur"), t.minusDays(1), 20);
        keluar(repo, b.get("nasiTelur"), nasiTelurB, t, 3);

        String nasiGorengLama = terima(repo, b.get("nasiGoreng"),
                t.minusDays(13), t.minusDays(1), 10);
        String nasiGorengBaru = terima(repo, b.get("nasiGoreng"),
                t.minusDays(6), t.plusDays(2), 22);
        keluar(repo, b.get("nasiGoreng"), nasiGorengLama,
                t.minusDays(2), 10);
        fefo(repo, b.get("nasiGoreng"), t.minusDays(1), 18);
        keluar(repo, b.get("nasiGoreng"), nasiGorengBaru, t, 3);

        String miA = terima(repo, b.get("miGoreng"),
                t.minusDays(8), t.plusDays(2), 20);
        terima(repo, b.get("miGoreng"), t.minusDays(2),
                t.plusDays(3), 15);
        keluar(repo, b.get("miGoreng"), miA, t.minusDays(1), 8);
        fefo(repo, b.get("miGoreng"), t, 10);

        String rotiLama = terima(repo, b.get("rotiIsi"),
                t.minusDays(5), t.minusDays(1), 10);
        String rotiBaru = terima(repo, b.get("rotiIsi"),
                t.minusDays(2), t.plusDays(2), 20);
        keluar(repo, b.get("rotiIsi"), rotiLama, t.minusDays(3), 6);
        fefo(repo, b.get("rotiIsi"), t, 8);
        keluar(repo, b.get("rotiIsi"), rotiBaru, t, 2);
        buang(repo, b.get("rotiIsi"), rotiLama, t, 1,
                "Kemasan rusak");

        String risolesA = terima(repo, b.get("risoles"),
                t.minusDays(4), t.plusDays(2), 24);
        String risolesB = terima(repo, b.get("risoles"),
                t.minusDays(1), t.plusDays(3), 12);
        keluar(repo, b.get("risoles"), risolesA, t.minusDays(2), 10);
        fefo(repo, b.get("risoles"), t, 15);

        String pastelA = terima(repo, b.get("pastel"),
                t.minusDays(3), t.plusDays(2), 18);
        String pastelB = terima(repo, b.get("pastel"),
                t.minusDays(2), t.plusDays(4), 12);
        fefo(repo, b.get("pastel"), t, 30);

        String airA = terima(repo, b.get("air"),
                t.minusDays(14), t.plusDays(300), 48);
        String airB = terima(repo, b.get("air"),
                t.minusDays(2), t.plusDays(240), 24);
        fefo(repo, b.get("air"), t, 30);
        keluar(repo, b.get("air"), airA, t, 12);

        String tehA = terima(repo, b.get("teh"),
                t.minusDays(12), t.plusDays(180), 24);
        String tehB = terima(repo, b.get("teh"),
                t.minusDays(3), t.plusDays(120), 12);
        fefo(repo, b.get("teh"), t, 25);

        String susuA = terima(repo, b.get("susu"),
                t.minusDays(9), t.plusDays(60), 30);
        terima(repo, b.get("susu"), t.minusDays(1),
                t.plusDays(90), 18);
        fefo(repo, b.get("susu"), t, 12);
        keluar(repo, b.get("susu"), susuA, t, 6);

        String kopiA = terima(repo, b.get("kopi"),
                t.minusDays(10), t.plusDays(120), 18);
        terima(repo, b.get("kopi"), t.minusDays(4),
                t.plusDays(150), 12);
        keluar(repo, b.get("kopi"), kopiA, t.minusDays(1), 8);
        fefo(repo, b.get("kopi"), t, 5);

        String isotonik = terima(repo, b.get("isotonik"),
                t.minusDays(6), t.plusDays(180), 24);
        keluar(repo, b.get("isotonik"), isotonik, t, 10);

        String keripikA = terima(repo, b.get("keripik"),
                t.minusDays(13), t.plusDays(90), 30);
        terima(repo, b.get("keripik"), t.minusDays(2),
                t.plusDays(120), 15);
        fefo(repo, b.get("keripik"), t, 20);
        keluar(repo, b.get("keripik"), keripikA, t, 5);

        String biskuitLama = terima(repo, b.get("biskuit"),
                t.minusDays(14), t.minusDays(2), 12);
        String biskuitBaru = terima(repo, b.get("biskuit"),
                t.minusDays(4), t.plusDays(60), 18);
        keluar(repo, b.get("biskuit"), biskuitLama, t.minusDays(5), 9);
        keluar(repo, b.get("biskuit"), biskuitBaru, t.minusDays(1), 5);

        String kacang = terima(repo, b.get("kacang"),
                t.minusDays(7), t.plusDays(45), 16);
        keluar(repo, b.get("kacang"), kacang, t, 4);
    }

    private static Barang barang(PenyimpananSQLite repo, String nama,
            String satuan, int minimum) {
        Barang barang = repo.simpanBarang(new Barang(nama, satuan));
        barang.setStokMinimum(minimum);
        repo.simpanStokMinimum(barang, minimum);
        return barang;
    }

    private static String terima(PenyimpananSQLite repo, Barang barang,
            LocalDate masuk, LocalDate kedaluwarsa, int jumlah) {
        String kode = repo.kodeBatchBerikutnya(barang, masuk);
        PenerimaanStok transaksi = new PenerimaanStok(masuk);
        transaksi.terima(barang, kode, jumlah, masuk, kedaluwarsa);
        repo.simpanTransaksi(barang, transaksi);
        return kode;
    }

    private static void keluar(PenyimpananSQLite repo, Barang barang,
            String kode, LocalDate tanggal, int jumlah) {
        PengeluaranStok transaksi = new PengeluaranStok(tanggal);
        transaksi.keluarkan(barang, batch(barang, kode), jumlah);
        repo.simpanTransaksi(barang, transaksi);
    }

    private static void fefo(PenyimpananSQLite repo, Barang barang,
            LocalDate tanggal, int jumlah) {
        PengeluaranStok transaksi = new PengeluaranStok(tanggal);
        transaksi.keluarkanFefo(barang, jumlah, tanggal);
        repo.simpanTransaksi(barang, transaksi);
    }

    private static void buang(PenyimpananSQLite repo, Barang barang,
            String kode, LocalDate tanggal, int jumlah, String alasan) {
        PembuanganStok transaksi = new PembuanganStok(tanggal);
        transaksi.buang(barang, batch(barang, kode), jumlah, alasan);
        repo.simpanTransaksi(barang, transaksi);
    }

    private static BatchStok batch(Barang barang, String kode) {
        for (BatchStok batch : barang.getDaftarBatch()) {
            if (batch.getKode().equals(kode)) {
                return batch;
            }
        }
        throw new IllegalStateException("Batch tidak ditemukan: " + kode);
    }

    private static void laporkan(PenyimpananSQLite repo) {
        LocalDate t = LocalDate.now();
        List<Barang> semua = repo.muatSemuaBarang();
        int batch = 0;
        int transaksi = 0;
        int rincian = 0;
        for (Barang barang : semua) {
            batch += barang.jumlahBatch();
            List<Transaksi> riwayat = repo.muatRiwayat(barang);
            transaksi += riwayat.size();
            for (Transaksi item : riwayat) {
                rincian += item.getRincian().size();
            }
        }
        System.out.println("[HASIL] barang=" + semua.size()
                + " batch=" + batch + " transaksi=" + transaksi
                + " rincian=" + rincian + " acuan=" + t);
        for (Barang barang : semua) {
            System.out.println("  " + barang.getNama() + " | "
                    + barang.getSatuan() + " | saldo="
                    + barang.hitungSaldo() + " | layak="
                    + barang.hitungStokLayak(t) + " | min="
                    + barang.getStokMinimum() + " | batch="
                    + barang.jumlahBatch());
        }
    }
}
