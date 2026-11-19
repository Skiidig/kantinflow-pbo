import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import kantinflow.Barang;
import kantinflow.BatchStok;
import kantinflow.PembuanganStok;
import kantinflow.PenerimaanStok;
import kantinflow.PengeluaranStok;
import kantinflow.Transaksi;
import kantinflow.penyimpanan.PenyimpananSQLite;

/**
 * Mengisi data sintetis kantin kampus melalui domain/repository. Kode batch
 * deterministik membuat skrip aman dijalankan ulang tanpa menggandakan data.
 */
public final class IsiDataKantin {
    private static final Set<String> KODE = new LinkedHashSet<>(List.of(
            "SUSU-20261117-01", "SUSU-20261110-01",
            "ROTI-20261118-01", "ROTI-20261114-01",
            "NASI-20261119-01",
            "BABI-20261117-01", "BABI-20261110-01",
            "AIR-20261114-01", "AIR-20261118-01",
            "COLA-20261112-01", "SPRITE-20261112-01",
            "TEH-20261115-01", "FANTA-20261113-01"));

    private IsiDataKantin() {
    }

    public static void main(String[] args) throws Exception {
        Path lokasi = Paths.get(args.length > 0
                ? args[0] : "data/kantinflow.db");
        try (PenyimpananSQLite repo = new PenyimpananSQLite(
                lokasi.toString())) {
            List<Barang> semua = repo.muatSemuaBarang();
            if (sudahAda(semua)) {
                System.out.println("[LEWATI] Data pengganti sudah ada; "
                        + "tidak ada data yang digandakan.");
                laporkan(repo);
                return;
            }

            LocalDate hari = LocalDate.now();
            Barang susu = wajib(semua, "Susu Kotak");
            Barang roti = wajib(semua, "Roti Bakar");
            Barang nasi = wajib(semua, "Nasi Goreng");
            Barang babi = wajib(semua, "Babi Guling");
            Barang air = wajib(semua, "Air Mineral");
            Barang cola = wajib(semua, "Coca Cola");
            Barang sprite = wajib(semua, "Sprite");
            Barang teh = wajib(semua, "Teh Kotak");
            Barang fanta = wajib(semua, "Fanta");

            terima(repo, susu, "SUSU-20261117-01",
                    hari.minusDays(2), hari.plusDays(20), 24);
            terima(repo, susu, "SUSU-20261110-01",
                    hari.minusDays(9), hari.plusDays(2), 12);
            terima(repo, roti, "ROTI-20261118-01",
                    hari.minusDays(1), hari.plusDays(2), 20);
            terima(repo, roti, "ROTI-20261114-01",
                    hari.minusDays(5), hari.minusDays(1), 6);
            terima(repo, nasi, "NASI-20261119-01",
                    hari, hari.plusDays(1), 18);
            terima(repo, babi, "BABI-20261117-01",
                    hari.minusDays(2), hari.plusDays(2), 10);
            terima(repo, babi, "BABI-20261110-01",
                    hari.minusDays(9), hari.minusDays(2), 4);
            terima(repo, air, "AIR-20261114-01",
                    hari.minusDays(5), hari.plusDays(180), 48);
            terima(repo, air, "AIR-20261118-01",
                    hari.minusDays(1), hari.plusDays(300), 24);
            terima(repo, cola, "COLA-20261112-01",
                    hari.minusDays(7), hari.plusDays(120), 24);
            terima(repo, sprite, "SPRITE-20261112-01",
                    hari.minusDays(7), hari.plusDays(120), 18);
            terima(repo, teh, "TEH-20261115-01",
                    hari.minusDays(4), hari.plusDays(90), 18);
            terima(repo, fanta, "FANTA-20261113-01",
                    hari.minusDays(6), hari.plusDays(120), 12);

            keluar(repo, susu, "SUSU-20261117-01", hari.minusDays(1), 4);
            keluar(repo, susu, "SUSU-20261117-01", hari, 10);
            keluar(repo, roti, "ROTI-20261118-01", hari, 5);
            buang(repo, roti, "ROTI-20261114-01", hari, 6,
                    "Kedaluwarsa");
            keluar(repo, nasi, "NASI-20261119-01", hari, 4);
            keluar(repo, babi, "BABI-20261117-01", hari, 3);
            fefo(repo, air, hari, 60);
            keluar(repo, cola, "COLA-20261112-01", hari, 6);
            fefo(repo, sprite, hari, 18);
            keluar(repo, teh, "TEH-20261115-01", hari, 3);
            keluar(repo, fanta, "FANTA-20261113-01", hari, 2);

            laporkan(repo);
        }
    }

    private static boolean sudahAda(List<Barang> semua) {
        for (Barang barang : semua) {
            for (BatchStok batch : barang.getDaftarBatch()) {
                if (KODE.contains(batch.getKode())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Barang wajib(List<Barang> semua, String nama) {
        for (Barang barang : semua) {
            if (barang.getNama().equalsIgnoreCase(nama)) {
                return barang;
            }
        }
        throw new IllegalStateException(
                "Barang wajib tidak ditemukan: " + nama);
    }

    private static BatchStok batch(Barang barang, String kode) {
        for (BatchStok batch : barang.getDaftarBatch()) {
            if (batch.getKode().equals(kode)) {
                return batch;
            }
        }
        throw new IllegalStateException("Batch tidak ditemukan: " + kode);
    }

    private static void terima(PenyimpananSQLite repo, Barang barang,
            String kode, LocalDate masuk, LocalDate kedaluwarsa, int jumlah) {
        PenerimaanStok transaksi = new PenerimaanStok(masuk);
        transaksi.terima(barang, kode, jumlah, masuk, kedaluwarsa);
        repo.simpanTransaksi(barang, transaksi);
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

    private static void laporkan(PenyimpananSQLite repo) {
        LocalDate hari = LocalDate.now();
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
                + " rincian=" + rincian + " acuan=" + hari);
        for (Barang barang : semua) {
            System.out.println("  " + barang.getNama() + " | "
                    + barang.getSatuan() + " | saldo="
                    + barang.hitungSaldo() + " | layak="
                    + barang.hitungStokLayak(hari) + " | min="
                    + barang.getStokMinimum() + " | batch="
                    + barang.jumlahBatch());
        }
    }
}
