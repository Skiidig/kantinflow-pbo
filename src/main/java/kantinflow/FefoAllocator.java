package kantinflow;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Menyusun rencana pengeluaran FEFO tanpa mengubah stok.
 */
public class FefoAllocator {
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

        batchLayak.sort(
                Comparator.comparing(BatchStok::getTanggalKedaluwarsa)
                        .thenComparing(BatchStok::getTanggalMasuk)
                        .thenComparing(BatchStok::getKode));

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
