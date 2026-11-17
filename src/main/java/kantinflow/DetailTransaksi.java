package kantinflow;

public class DetailTransaksi {
    private final BatchStok batch;
    private final int jumlahDiterima;

    public DetailTransaksi(BatchStok batch, int jumlahDiterima) {
        if (batch == null) {
            throw new IllegalArgumentException("Batch transaksi wajib ada");
        }

        this.batch = batch;
        this.jumlahDiterima = jumlahDiterima;
    }

    public BatchStok getBatch() {
        return batch;
    }

    public int getJumlahDiterima() {
        return jumlahDiterima;
    }
}
