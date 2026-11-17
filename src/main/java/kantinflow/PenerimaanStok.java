package kantinflow;

import java.time.LocalDate;
import java.util.List;

public class PenerimaanStok extends Transaksi {
    private DetailTransaksi detail;

    public PenerimaanStok(LocalDate tanggal) {
        super(tanggal);
    }

    /**
     * Memvalidasi penerimaan, lalu menambahkan batch ke barang dan mencatat
     * detail penerimaan. Jika validasi gagal, tidak ada batch yang ditambahkan.
     */
    public DetailTransaksi terima(
            Barang barang,
            String kodeBatch,
            int jumlah,
            LocalDate tanggalMasuk,
            LocalDate tanggalKedaluwarsa) {
        if (detail != null) {
            throw new IllegalStateException("Penerimaan ini sudah memiliki detail");
        }
        if (jumlah <= 0) {
            throw new IllegalArgumentException("Jumlah harus lebih dari 0");
        }
        if (barang == null) {
            throw new IllegalArgumentException("Barang wajib diisi");
        }
        for (BatchStok batchAda : barang.getDaftarBatch()) {
            if (batchAda.getKode().equalsIgnoreCase(kodeBatch)) {
                throw new IllegalArgumentException(
                        "Kode batch sudah digunakan: " + kodeBatch);
            }
        }

        BatchStok batch = new BatchStok(
                kodeBatch,
                tanggalMasuk,
                tanggalKedaluwarsa,
                jumlah);
        DetailTransaksi detailBaru = new DetailTransaksi(batch, jumlah);

        barang.tambahBatch(batch);
        this.detail = detailBaru;

        return detailBaru;
    }

    public DetailTransaksi getDetail() {
        return detail;
    }

    public static PenerimaanStok dariPenyimpanan(
            LocalDate tanggal,
            BatchStok batch,
            int jumlah) {
        if (batch == null) {
            throw new IllegalArgumentException("Batch penerimaan wajib ada");
        }
        if (jumlah <= 0) {
            throw new IllegalArgumentException("Jumlah penerimaan harus lebih dari 0");
        }
        PenerimaanStok penerimaan = new PenerimaanStok(tanggal);
        penerimaan.detail = new DetailTransaksi(batch, jumlah);
        return penerimaan;
    }

    @Override
    public String getJenis() {
        return "Penerimaan";
    }

    @Override
    public BatchStok getBatch() {
        if (detail == null) {
            throw new IllegalStateException("Penerimaan belum memiliki detail");
        }
        return detail.getBatch();
    }

    @Override
    public int getJumlah() {
        if (detail == null) {
            throw new IllegalStateException("Penerimaan belum memiliki detail");
        }
        return detail.getJumlahDiterima();
    }

    @Override
    public List<RincianTransaksi> getRincian() {
        if (detail == null) {
            throw new IllegalStateException("Penerimaan belum memiliki detail");
        }
        return List.of(new RincianTransaksi(detail.getBatch(), detail.getJumlahDiterima()));
    }
}
