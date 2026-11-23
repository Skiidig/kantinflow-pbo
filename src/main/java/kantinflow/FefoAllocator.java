package kantinflow;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Menyusun rencana pengeluaran FEFO tanpa mengubah stok.
 *
 * <p>Kandidat alokasi hanya batch yang sudah masuk pada tanggal transaksi
 * (tanggal masuk tidak setelah tanggal transaksi), belum kedaluwarsa, dan
 * masih bersisa.</p>
 */
public class FefoAllocator {
    /**
     * Urutan FEFO tunggal untuk domain dan GUI: tanggal kedaluwarsa, tanggal
     * masuk, lalu kode batch tanpa membedakan huruf besar-kecil.
     */
    public static final Comparator<BatchStok> URUTAN_FEFO =
            Comparator.comparing(BatchStok::getTanggalKedaluwarsa)
                    .thenComparing(BatchStok::getTanggalMasuk)
                    .thenComparing(BatchStok::getKode,
                            String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(BatchStok::getKode);

    public List<RincianTransaksi> susun(
            Barang barang,
            int jumlah,
            LocalDate tanggalPengeluaran) {
        if (barang == null) {
            throw new IllegalArgumentException("Barang wajib diisi");
        }
        if (tanggalPengeluaran == null) {
            throw new IllegalArgumentException("Tanggal pengeluaran wajib diisi");
        }
        if (jumlah <= 0) {
            throw new IllegalArgumentException("Jumlah harus lebih dari 0");
        }

        List<BatchStok> batchLayak = new ArrayList<>();
        for (BatchStok batch : barang.getDaftarBatch()) {
            if (batch.layakPada(tanggalPengeluaran)) {
                batchLayak.add(batch);
            }
        }

        batchLayak.sort(URUTAN_FEFO);

        int totalLayak = 0;
        for (BatchStok batch : batchLayak) {
            totalLayak += batch.getSisaJumlah();
        }

        if (totalLayak < jumlah) {
            throw new StokTidakCukupException(
                    "Stok layak tidak cukup: diminta " + jumlah + " "
                            + barang.getSatuan() + ", tersedia " + totalLayak
                            + " " + barang.getSatuan());
        }

        List<RincianTransaksi> alokasi = new ArrayList<>();
        int sisaPermintaan = jumlah;
        for (BatchStok batch : batchLayak) {
            if (sisaPermintaan == 0) {
                break;
            }
            int diambil = Math.min(batch.getSisaJumlah(), sisaPermintaan);
            alokasi.add(new RincianTransaksi(batch, diambil));
            sisaPermintaan -= diambil;
        }
        return alokasi;
    }
}
