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
    List<Barang> getDaftarBarang();

    List<Transaksi> getRiwayat(Barang barang);

    LocalDate getTanggalPemantauan();

    /**
     * Tanggal acuan efektif. Sebelum pengguna memilih tanggal, halaman
     * memakai hari ini agar ringkasan dan pemantauan langsung terisi.
     */
    default LocalDate getTanggalAcuan() {
        LocalDate tanggal = getTanggalPemantauan();
        return tanggal == null ? LocalDate.now() : tanggal;
    }

    void setTanggalPemantauan(LocalDate tanggal);

    boolean simpanTransaksi(Barang barang, Transaksi transaksi,
            String pesanSukses);

    /**
     * Kode batch internal untuk barang dan tanggal masuk, dihitung dari
     * batch yang sudah tersimpan.
     */
    default String kodeBatchOtomatis(Barang barang, LocalDate tanggalMasuk) {
        return null;
    }

    boolean tambahBarang(String nama, String satuan);

    boolean simpanStokMinimum(Barang barang, int minimum);

    /**
     * Membuka formulir Pembuangan untuk batch dari Pemantauan tanpa
     * menjalankan transaksi.
     */
    default boolean bukaPembuanganDariPemantauan(
            int idBarang, String kodeBatch, LocalDate tanggal) {
        return false;
    }

    boolean muatUlang();

    void tampilkanPesan(String pesan, boolean sukses);
}
