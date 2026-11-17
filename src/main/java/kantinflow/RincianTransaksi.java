package kantinflow;

/**
 * Satu baris alokasi batch di dalam transaksi. Satu transaksi FEFO dapat
 * memiliki beberapa rincian, sedangkan pengeluaran manual hanya satu.
 */
public class RincianTransaksi {
    private final BatchStok batch;
    private final int jumlah;

    public RincianTransaksi(BatchStok batch, int jumlah) {
        if (batch == null) {
            throw new IllegalArgumentException("Batch rincian wajib ada");
        }
        if (jumlah <= 0) {
            throw new IllegalArgumentException("Jumlah rincian harus lebih dari 0");
        }
        this.batch = batch;
        this.jumlah = jumlah;
    }

    public BatchStok getBatch() {
        return batch;
    }

    public int getJumlah() {
        return jumlah;
    }
}
