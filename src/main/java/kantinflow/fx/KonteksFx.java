package kantinflow.fx;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

import kantinflow.Barang;
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

    void tampilkanPesan(String pesan, boolean sukses);
}
