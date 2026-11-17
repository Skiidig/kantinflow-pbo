package kantinflow.fx;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

import kantinflow.Barang;
import kantinflow.BatchStok;
import kantinflow.Transaksi;

public interface KonteksFx {
    Barang getBarangAktif();

    List<Barang> getDaftarBarang();

    List<Transaksi> getRiwayat();

    LocalDate getTanggalPemantauan();

    void setTanggalPemantauan(LocalDate tanggal);

    void simpanPenerimaanAsync(String kodeBatch, int jumlah,
            LocalDate tanggalMasuk, LocalDate tanggalKedaluwarsa,
            Consumer<Boolean> selesai);

    void tambahBarangAsync(String nama, String satuan, int stokMinimum,
            Consumer<Boolean> selesai);

    void simpanMinimumAsync(int stokMinimum, Consumer<Boolean> selesai);

    void pilihBarangAsync(Barang barang);

    void keluarkanManualAsync(BatchStok batch, int jumlah,
            Consumer<HasilPengeluaran> selesai);

    void keluarkanFefoAsync(int jumlah, LocalDate tanggal,
            Consumer<HasilPengeluaran> selesai);

    void tampilkanPesan(String pesan, boolean sukses);
}
