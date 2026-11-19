import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import kantinflow.Barang;
import kantinflow.KodeBatch;
import kantinflow.PengeluaranStok;
import kantinflow.PenerimaanStok;
import kantinflow.penyimpanan.PenyimpananSQLite;

/**
 * Pemeriksaan kecil pembentukan kode batch dan urutan setelah restart.
 */
public final class KodeBatchHarness {
    public static void main(String[] args) throws Exception {
        LocalDate tanggal = LocalDate.of(2026, 11, 19);
        Path db = Files.createTempFile("kantinflow-kode-", ".db");
        int idBarang;
        String kodeKedua;
        try {
            Files.deleteIfExists(db);
            try (PenyimpananSQLite repo = new PenyimpananSQLite(
                    db.toString())) {
                Barang nasi = repo.simpanBarang(
                        new Barang("Nasi Goreng", "kotak"));
                idBarang = nasi.getId();
                String satu = repo.kodeBatchBerikutnya(nasi, tanggal);
                cek("AWALAN_NASI", satu.equals("NASI-20261119-01"), satu);
                terima(repo, nasi, satu, tanggal, 5);

                kodeKedua = repo.kodeBatchBerikutnya(nasi, tanggal);
                cek("URUTAN_KEDUA",
                        kodeKedua.equals("NASI-20261119-02"), kodeKedua);
                terima(repo, nasi, kodeKedua, tanggal, 7);

                Barang roti = repo.simpanBarang(
                        new Barang("Roti Bakar", "bungkus"));
                terima(repo, roti, "IMPOR-20261119-07", tanggal, 1);
                String rotiBerikut = repo.kodeBatchBerikutnya(roti, tanggal);
                cek("IDENTITAS_BARANG_STABIL",
                        rotiBerikut.equals("ROTI-20261119-08"),
                        rotiBerikut);

                PengeluaranStok habis = new PengeluaranStok(tanggal);
                habis.keluarkanFefo(nasi, 12, tanggal);
                repo.simpanTransaksi(nasi, habis);
            }
            try (PenyimpananSQLite repo = new PenyimpananSQLite(
                    db.toString())) {
                Barang nasi = repo.muatBarang(idBarang);
                String tiga = repo.kodeBatchBerikutnya(nasi, tanggal);
                cek("SETELAH_RESTART_DAN_HABIS",
                        tiga.equals("NASI-20261119-03"), tiga);

                int batchSebelum = nasi.jumlahBatch();
                int saldoSebelum = nasi.hitungSaldo();
                int riwayatSebelum = repo.muatRiwayat(nasi).size();
                boolean ditolak = false;
                try {
                    terima(repo, nasi, kodeKedua, tanggal, 1);
                } catch (IllegalArgumentException exception) {
                    ditolak = true;
                }
                cek("KODE_DUPLIKAT_DITOLAK", ditolak, "tidak ditolak");
                cek("GAGAL_TIDAK_MENAMBAH_BATCH",
                        nasi.jumlahBatch() == batchSebelum,
                        "batch=" + nasi.jumlahBatch());
                cek("GAGAL_TIDAK_MENGUBAH_SALDO",
                        nasi.hitungSaldo() == saldoSebelum,
                        "saldo=" + nasi.hitungSaldo());
                cek("GAGAL_TIDAK_MENAMBAH_RIWAYAT",
                        repo.muatRiwayat(nasi).size() == riwayatSebelum,
                        "riwayat=" + repo.muatRiwayat(nasi).size());
            }
            cek("LEBIH_DARI_99",
                    KodeBatch.format("NASI", tanggal, 100)
                            .equals("NASI-20261119-100"),
                    KodeBatch.format("NASI", tanggal, 100));
            System.out.println("KODE_BATCH_HARNESS_LULUS");
        } finally {
            Files.deleteIfExists(db);
        }
    }

    private static void terima(PenyimpananSQLite repo, Barang barang,
            String kode, LocalDate tanggal, int jumlah) {
        PenerimaanStok masuk = new PenerimaanStok(tanggal);
        masuk.terima(barang, kode, jumlah, tanggal, tanggal.plusDays(30));
        repo.simpanTransaksi(barang, masuk);
    }

    private static void cek(String nama, boolean lulus, String nilai) {
        if (!lulus) {
            throw new IllegalStateException(nama + ": " + nilai);
        }
        System.out.println("[OK] " + nama + " " + nilai);
    }
}
