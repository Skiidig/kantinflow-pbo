package kantinflow.gui;

import java.time.LocalDate;
import java.util.List;

import kantinflow.Barang;
import kantinflow.Transaksi;

/**
 * Kontrak sempit antara panel halaman dan jendela pengendali.
 * Panel tidak mengakses JDBC secara langsung.
 */
public interface KonteksKantin {
    Barang getBarangAktif();

    List<Barang> getDaftarBarang();

    List<Transaksi> getRiwayat();

    LocalDate getTanggalPemantauan();

    void setTanggalPemantauan(LocalDate tanggal);

    boolean simpanTransaksi(Transaksi transaksi, String pesanSukses);

    boolean tambahBarang(String nama, String satuan);

    boolean simpanStokMinimum(int minimum);

    void gantiBarang(Barang barang);

    boolean muatUlang();

    void tampilkanPesan(String pesan, boolean sukses);
}
