package kantinflow;

import java.time.LocalDate;
import java.util.List;

/**
 * Kontrak umum untuk semua transaksi stok yang disimpan pada satu riwayat.
 */
public abstract class Transaksi {
    private final LocalDate tanggal;

    protected Transaksi(LocalDate tanggal) {
        if (tanggal == null) {
            throw new IllegalArgumentException("Tanggal transaksi wajib diisi");
        }
        this.tanggal = tanggal;
    }

    public LocalDate getTanggal() {
        return tanggal;
    }

    public abstract String getJenis();

    public abstract BatchStok getBatch();

    public abstract int getJumlah();

    public abstract List<RincianTransaksi> getRincian();

    /**
     * Alasan opsional untuk transaksi yang membutuhkannya, misalnya pembuangan.
     */
    public String getAlasan() {
        return "";
    }
}
