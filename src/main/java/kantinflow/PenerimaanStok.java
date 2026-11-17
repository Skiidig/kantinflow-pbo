package kantinflow;

import java.time.LocalDate;

public class PenerimaanStok {
    private final LocalDate tanggal;
    private DetailTransaksi detail;

    public PenerimaanStok(LocalDate tanggal) {
        if (tanggal == null) {
            throw new IllegalArgumentException("Tanggal penerimaan wajib diisi");
        }
        this.tanggal = tanggal;
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

    public LocalDate getTanggal() {
        return tanggal;
    }

    public DetailTransaksi getDetail() {
        return detail;
    }
}
