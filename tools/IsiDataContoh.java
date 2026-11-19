import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import kantinflow.Barang;
import kantinflow.BatchStok;
import kantinflow.PembuanganStok;
import kantinflow.PenerimaanStok;
import kantinflow.PengeluaranStok;
import kantinflow.RincianTransaksi;
import kantinflow.Transaksi;
import kantinflow.penyimpanan.PenyimpananSQLite;

/**
 * Mengisi data contoh ke database aplikasi melalui operasi domain dan
 * repository. Aman dijalankan ulang: bila batch contoh sudah ada, tidak ada
 * data yang ditambahkan.
 *
 * Jalankan: java -cp "out;out-seed;lib/*" IsiDataContoh data/kantinflow.db
 */
public final class IsiDataContoh {
    private static final String PREFIX = "CONTOH-";

    private IsiDataContoh() {
    }

    public static void main(String[] args) throws Exception {
        Path lokasi = Paths.get(args.length > 0
                ? args[0] : "data/kantinflow.db");
        try (PenyimpananSQLite repo = new PenyimpananSQLite(lokasi.toString())) {
            List<Barang> semua = repo.muatSemuaBarang();
            if (sudahAdaContoh(semua)) {
                System.out.println("[LEWATI] Dataset contoh sudah ada; "
                        + "tidak ada data yang digandakan.");
            } else {
                isi(repo, semua);
            }
        }

        checkpointJikaWal(lokasi);

        try (PenyimpananSQLite repo = new PenyimpananSQLite(lokasi.toString())) {
            laporkan(repo);
        }
    }

    private static boolean sudahAdaContoh(List<Barang> semua) {
        for (Barang barang : semua) {
            for (BatchStok batch : barang.getDaftarBatch()) {
                if (batch.getKode().toUpperCase(Locale.ROOT)
                        .startsWith(PREFIX)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void isi(PenyimpananSQLite repo, List<Barang> semua) {
        LocalDate hari = LocalDate.now();

        Barang susu = wajib(semua, "Susu Kotak");
        Barang roti = wajib(semua, "Roti Bakar");
        Barang nasi = wajib(semua, "Nasi Goreng");
        Barang babi = wajib(semua, "Babi Guling");
        Barang air = wajib(semua, "Air Mineral");
        Barang cola = wajib(semua, "Coca Cola");
        Barang sprite = wajib(semua, "Sprite");
        Barang teh = cari(semua, "Teh Kotak");
        if (teh == null) {
            teh = repo.simpanBarang(new Barang("Teh Kotak", "kotak"));
            teh.setStokMinimum(6);
            repo.simpanStokMinimum(teh, 6);
            semua.add(teh);
            System.out.println("[BARU] Barang Teh Kotak (min 6).");
        }

        // Susu: aman, segera kedaluwarsa, dan batch kedaluwarsa bersisa.
        terima(repo, susu, "CONTOH-SUSU-A", hari.minusDays(20),
                hari.plusDays(120), 24);
        terima(repo, susu, "CONTOH-SUSU-B", hari.minusDays(12),
                hari.plusDays(5), 18);
        terima(repo, susu, "CONTOH-SUSU-C", hari.minusDays(40),
                hari.minusDays(3), 10);
        keluar(repo, susu, "CONTOH-SUSU-A", hari.minusDays(6), 4);
        fefo(repo, susu, hari.minusDays(2), 22);
        buang(repo, susu, "CONTOH-SUSU-C", hari, 6,
                "Kemasan rusak dan kedaluwarsa");

        // Roti: batch segera kedaluwarsa dan batch kedaluwarsa habis.
        terima(repo, roti, "CONTOH-ROTI-A", hari.minusDays(10),
                hari.plusDays(4), 20);
        terima(repo, roti, "CONTOH-ROTI-B", hari.minusDays(30),
                hari.minusDays(2), 8);
        keluar(repo, roti, "CONTOH-ROTI-A", hari.minusDays(3), 5);
        buang(repo, roti, "CONTOH-ROTI-B", hari, 8,
                "Kedaluwarsa");

        // Nasi: FEFO lintas dua batch.
        terima(repo, nasi, "CONTOH-NASI-A", hari.minusDays(15),
                hari.plusDays(2), 15);
        terima(repo, nasi, "CONTOH-NASI-B", hari.minusDays(8),
                hari.plusDays(30), 25);
        fefo(repo, nasi, hari.minusDays(1), 18);
        keluar(repo, nasi, "CONTOH-NASI-B", hari, 4);

        // Babi: dua batch aman dan satu batch kedaluwarsa habis.
        terima(repo, babi, "CONTOH-BABI-A", hari.minusDays(9),
                hari.plusDays(60), 12);
        terima(repo, babi, "CONTOH-BABI-B", hari.minusDays(4),
                hari.plusDays(90), 20);
        terima(repo, babi, "CONTOH-BABI-C", hari.minusDays(25),
                hari.minusDays(6), 5);
        keluar(repo, babi, "CONTOH-BABI-B", hari.minusDays(2), 6);
        buang(repo, babi, "CONTOH-BABI-C", hari, 5,
                "Kedaluwarsa");

        // Air: dua FEFO menghabiskan seluruh stok.
        terima(repo, air, "CONTOH-AIR-A", hari.minusDays(14),
                hari.plusDays(180), 36);
        terima(repo, air, "CONTOH-AIR-B", hari.minusDays(2),
                hari.plusDays(365), 24);
        fefo(repo, air, hari, 30);
        fefo(repo, air, hari, 30);

        // Cola: FEFO memakai batch segera kedaluwarsa lebih dahulu.
        terima(repo, cola, "CONTOH-COLA-A", hari.minusDays(6),
                hari.plusDays(45), 18);
        terima(repo, cola, "CONTOH-COLA-B", hari.minusDays(18),
                hari.plusDays(3), 12);
        keluar(repo, cola, "CONTOH-COLA-A", hari.minusDays(2), 4);
        fefo(repo, cola, hari, 14);

        // Sprite: batch kedaluwarsa masih bersisa.
        terima(repo, sprite, "CONTOH-SPRITE-A", hari.minusDays(5),
                hari.plusDays(40), 14);
        terima(repo, sprite, "CONTOH-SPRITE-B", hari.minusDays(35),
                hari.minusDays(1), 6);
        keluar(repo, sprite, "CONTOH-SPRITE-A", hari.minusDays(1), 3);
        buang(repo, sprite, "CONTOH-SPRITE-B", hari, 2,
                "Kedaluwarsa");

        // Teh: stok layak di bawah minimum sehingga perlu restok.
        terima(repo, teh, "CONTOH-TEH-A", hari.minusDays(7),
                hari.plusDays(90), 8);
        terima(repo, teh, "CONTOH-TEH-B", hari.minusDays(20),
                hari.plusDays(1), 4);
        fefo(repo, teh, hari, 9);
    }

    private static Barang cari(List<Barang> semua, String nama) {
        for (Barang barang : semua) {
            if (barang.getNama().equalsIgnoreCase(nama)) {
                return barang;
            }
        }
        return null;
    }

    private static Barang wajib(List<Barang> semua, String nama) {
        Barang barang = cari(semua, nama);
        if (barang == null) {
            throw new IllegalStateException("Barang tidak ditemukan: " + nama);
        }
        return barang;
    }

    private static BatchStok batch(Barang barang, String kode) {
        for (BatchStok batch : barang.getDaftarBatch()) {
            if (batch.getKode().equals(kode)) {
                return batch;
            }
        }
        throw new IllegalStateException(
                "Batch tidak ditemukan: " + barang.getNama() + "/" + kode);
    }

    private static void terima(PenyimpananSQLite repo, Barang barang,
            String kode, LocalDate masuk, LocalDate kedaluwarsa, int jumlah) {
        PenerimaanStok penerimaan = new PenerimaanStok(masuk);
        penerimaan.terima(barang, kode, jumlah, masuk, kedaluwarsa);
        repo.simpanTransaksi(barang, penerimaan);
    }

    private static void keluar(PenyimpananSQLite repo, Barang barang,
            String kode, LocalDate tanggal, int jumlah) {
        PengeluaranStok pengeluaran = new PengeluaranStok(tanggal);
        pengeluaran.keluarkan(barang, batch(barang, kode), jumlah);
        repo.simpanTransaksi(barang, pengeluaran);
    }

    private static void fefo(PenyimpananSQLite repo, Barang barang,
            LocalDate tanggal, int jumlah) {
        PengeluaranStok pengeluaran = new PengeluaranStok(tanggal);
        pengeluaran.keluarkanFefo(barang, jumlah, tanggal);
        repo.simpanTransaksi(barang, pengeluaran);
    }

    private static void buang(PenyimpananSQLite repo, Barang barang,
            String kode, LocalDate tanggal, int jumlah, String alasan) {
        PembuanganStok pembuangan = new PembuanganStok(tanggal);
        pembuangan.buang(barang, batch(barang, kode), jumlah, alasan);
        repo.simpanTransaksi(barang, pembuangan);
    }

    private static void checkpointJikaWal(Path lokasi) throws Exception {
        Class.forName("org.sqlite.JDBC");
        try (Connection koneksi = DriverManager.getConnection(
                "jdbc:sqlite:" + lokasi.toAbsolutePath())) {
            String mode;
            try (Statement statement = koneksi.createStatement();
                    ResultSet hasil = statement.executeQuery(
                            "PRAGMA journal_mode")) {
                mode = hasil.next() ? hasil.getString(1) : "";
            }
            if ("wal".equalsIgnoreCase(mode)) {
                try (Statement statement = koneksi.createStatement()) {
                    statement.execute("PRAGMA wal_checkpoint(TRUNCATE)");
                }
                System.out.println("[OK] Checkpoint WAL selesai.");
            }
        }
    }

    private static void laporkan(PenyimpananSQLite repo) {
        LocalDate hari = LocalDate.now();
        List<Barang> semua = repo.muatSemuaBarang();
        int totalBatch = 0;
        int totalTransaksi = 0;
        int totalRincian = 0;
        for (Barang barang : semua) {
            totalBatch += barang.jumlahBatch();
            List<Transaksi> riwayat = repo.muatRiwayat(barang);
            totalTransaksi += riwayat.size();
            for (Transaksi transaksi : riwayat) {
                totalRincian += transaksi.getRincian().size();
            }
        }
        System.out.println("[HASIL] barang=" + semua.size()
                + " batch=" + totalBatch
                + " transaksi=" + totalTransaksi
                + " rincian=" + totalRincian
                + " acuan=" + hari);
        for (Barang barang : semua) {
            System.out.println("  " + barang.getNama() + " | "
                    + barang.getSatuan() + " | saldo=" + barang.hitungSaldo()
                    + " | layak=" + barang.hitungStokLayak(hari)
                    + " | min=" + barang.getStokMinimum()
                    + " | batch=" + barang.jumlahBatch());
        }
    }
}
