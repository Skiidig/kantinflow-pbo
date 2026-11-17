package kantinflow;

import java.time.LocalDate;

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
}
