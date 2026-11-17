package kantinflow;

import java.time.LocalDate;
import java.util.List;

public class PembuanganStok extends Transaksi {
    private BatchStok batch;
    private int jumlah;
    private String alasan;

    public PembuanganStok(LocalDate tanggal) {
        super(tanggal);
    }

    public static PembuanganStok dariPenyimpanan(
            LocalDate tanggal,
            BatchStok batch,
            int jumlah,
            String alasan) {
        if (batch == null) {
            throw new IllegalArgumentException("Batch pembuangan wajib ada");
        }
        if (jumlah <= 0) {
            throw new IllegalArgumentException("Jumlah pembuangan harus lebih dari 0");
        }
        PembuanganStok pembuangan = new PembuanganStok(tanggal);
        pembuangan.batch = batch;
        pembuangan.jumlah = jumlah;
        pembuangan.alasan = alasan == null ? "" : alasan;
        return pembuangan;
    }

    /**
     * Memvalidasi seluruh isian sebelum sisa batch dikurangi. Hanya batch
     * berstatus KEDALUWARSA pada tanggal pembuangan yang boleh dibuang.
     */
    public void buang(Barang barang, BatchStok batch, int jumlah, String alasan) {
        if (this.batch != null) {
            throw new IllegalStateException("Pembuangan ini sudah dicatat");
        }
        if (barang == null) {
            throw new IllegalArgumentException("Barang wajib diisi");
        }
        if (batch == null) {
            throw new IllegalArgumentException("Batch wajib dipilih");
        }
        if (!barang.memilikiBatch(batch)) {
            throw new IllegalArgumentException("Batch bukan milik barang tersebut");
        }
        if (alasan == null || alasan.isBlank()) {
            throw new IllegalArgumentException("Alasan pembuangan wajib diisi");
        }
        if (batch.statusPada(getTanggal()) != StatusKedaluwarsa.KEDALUWARSA) {
            throw new IllegalArgumentException(
                    "Batch belum kedaluwarsa pada tanggal pembuangan");
        }
        if (jumlah <= 0) {
            throw new IllegalArgumentException("Jumlah harus lebih dari 0");
        }
        if (jumlah > batch.getSisaJumlah()) {
            throw new StokTidakCukupException(
                    "Jumlah pembuangan melebihi sisa batch: diminta " + jumlah
                            + " " + barang.getSatuan() + ", tersedia "
                            + batch.getSisaJumlah() + " " + barang.getSatuan());
        }

        batch.kurangiSisa(jumlah);
        this.batch = batch;
        this.jumlah = jumlah;
        this.alasan = alasan.trim();
    }

    @Override
    public String getJenis() {
        return "Pembuangan";
    }

    @Override
    public BatchStok getBatch() {
        if (batch == null) {
            throw new IllegalStateException("Pembuangan belum dicatat");
        }
        return batch;
    }

    @Override
    public int getJumlah() {
        if (batch == null) {
            throw new IllegalStateException("Pembuangan belum dicatat");
        }
        return jumlah;
    }

    @Override
    public List<RincianTransaksi> getRincian() {
        if (batch == null) {
            throw new IllegalStateException("Pembuangan belum dicatat");
        }
        return List.of(new RincianTransaksi(batch, jumlah));
    }

    @Override
    public String getAlasan() {
        if (batch == null) {
            throw new IllegalStateException("Pembuangan belum dicatat");
        }
        return alasan;
    }
}
