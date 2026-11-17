package kantinflow.fx;

import java.util.List;

import kantinflow.Barang;
import kantinflow.RincianTransaksi;
import kantinflow.Transaksi;

/**
 * Hasil task pengeluaran: status, rincian alokasi, dan state database terbaru.
 */
public record HasilPengeluaran(
        boolean berhasil,
        String pesan,
        List<RincianTransaksi> rincian,
        Barang barang,
        List<Transaksi> riwayat) {
    public HasilPengeluaran {
        rincian = rincian == null ? List.of() : List.copyOf(rincian);
        riwayat = riwayat == null ? List.of() : List.copyOf(riwayat);
    }
}
