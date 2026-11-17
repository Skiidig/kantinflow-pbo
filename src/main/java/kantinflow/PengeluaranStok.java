package kantinflow;

import java.time.LocalDate;

public class PengeluaranStok extends Transaksi {
    private BatchStok batch;
    private int jumlah;

    public PengeluaranStok(LocalDate tanggal) {
        super(tanggal);
    }

    /**
     * Memvalidasi kepemilikan batch, jumlah, dan kecukupan stok sebelum
     * mengurangi sisa batch. Jika validasi gagal, tidak ada perubahan stok.
     */
    public void keluarkan(Barang barang, BatchStok batch, int jumlah) {
        if (this.batch != null) {
            throw new IllegalStateException("Pengeluaran ini sudah dicatat");
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
        if (jumlah <= 0) {
            throw new IllegalArgumentException("Jumlah harus lebih dari 0");
        }
        if (batch.getSisaJumlah() < jumlah) {
            throw new StokTidakCukupException(
                    "Stok tidak cukup: diminta " + jumlah + " " + barang.getSatuan()
                            + ", tersedia " + batch.getSisaJumlah() + " "
                            + barang.getSatuan());
        }

        batch.kurangiSisa(jumlah);
        this.batch = batch;
        this.jumlah = jumlah;
    }

    @Override
    public String getJenis() {
        return "Pengeluaran";
    }

    @Override
    public BatchStok getBatch() {
        if (batch == null) {
            throw new IllegalStateException("Pengeluaran belum dicatat");
        }
        return batch;
    }

    @Override
    public int getJumlah() {
        if (batch == null) {
            throw new IllegalStateException("Pengeluaran belum dicatat");
        }
        return jumlah;
    }
}
