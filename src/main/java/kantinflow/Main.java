package kantinflow;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        Barang susuKotak = new Barang("Susu Kotak", "kotak");
        List<Transaksi> riwayat = new ArrayList<>();

        LocalDate tanggalTransaksi = LocalDate.of(2026, 11, 17);
        LocalDate tanggalKedaluwarsa = LocalDate.of(2026, 12, 17);

        System.out.println("KantinFlow - Demo Penerimaan dan Pengeluaran Stok");
        System.out.println("Barang: " + susuKotak.getNama());
        System.out.println("Stok awal: " + susuKotak.hitungSaldo()
                + " " + susuKotak.getSatuan());
        System.out.println();

        System.out.println("Skenario 1 - Terima batch SUSU-A sebanyak 5 kotak");
        PenerimaanStok penerimaanPertama = new PenerimaanStok(tanggalTransaksi);
        DetailTransaksi detailPertama = penerimaanPertama.terima(
                susuKotak,
                "SUSU-A",
                5,
                tanggalTransaksi,
                tanggalKedaluwarsa);
        riwayat.add(penerimaanPertama);

        System.out.println("Batch SUSU-A tersimpan: "
                + detailPertama.getJumlahDiterima() + " kotak");
        System.out.println("Tanggal masuk: " + detailPertama.getBatch().getTanggalMasuk());
        System.out.println("Tanggal kedaluwarsa: "
                + detailPertama.getBatch().getTanggalKedaluwarsa());
        cetakRingkasan(susuKotak, riwayat);
        System.out.println();

        System.out.println("Skenario 2 - Terima batch SUSU-B sebanyak 0 kotak");
        PenerimaanStok penerimaanKedua = new PenerimaanStok(tanggalTransaksi);
        try {
            penerimaanKedua.terima(
                    susuKotak,
                    "SUSU-B",
                    0,
                    tanggalTransaksi,
                    tanggalKedaluwarsa);
            riwayat.add(penerimaanKedua);
            System.out.println("Batch SUSU-B tersimpan");
        } catch (IllegalArgumentException exception) {
            System.out.println(exception.getMessage());
        }
        cetakRingkasan(susuKotak, riwayat);
        System.out.println();

        BatchStok batchSusuA = detailPertama.getBatch();

        System.out.println("Skenario 3 - Keluarkan 2 kotak dari SUSU-A");
        PengeluaranStok pengeluaranPertama = new PengeluaranStok(tanggalTransaksi);
        pengeluaranPertama.keluarkan(susuKotak, batchSusuA, 2);
        riwayat.add(pengeluaranPertama);

        System.out.println("Pengeluaran SUSU-A tersimpan: "
                + pengeluaranPertama.getJumlah() + " kotak");
        cetakRingkasan(susuKotak, riwayat);
        cetakRiwayat(riwayat, susuKotak.getSatuan());
        System.out.println();

        System.out.println("Skenario 4 - Coba keluarkan 4 kotak dari SUSU-A");
        PengeluaranStok pengeluaranKedua = new PengeluaranStok(tanggalTransaksi);
        try {
            pengeluaranKedua.keluarkan(susuKotak, batchSusuA, 4);
            riwayat.add(pengeluaranKedua);
            System.out.println("Pengeluaran SUSU-A tersimpan");
        } catch (IllegalArgumentException exception) {
            System.out.println(exception.getMessage());
        }
        cetakRingkasan(susuKotak, riwayat);
    }

    private static void cetakRingkasan(Barang barang, List<Transaksi> riwayat) {
        System.out.println("Saldo: " + barang.hitungSaldo() + " " + barang.getSatuan());
        System.out.println("Jumlah batch: " + barang.jumlahBatch());
        System.out.println("Jumlah riwayat: " + riwayat.size());
    }

    private static void cetakRiwayat(List<Transaksi> riwayat, String satuan) {
        System.out.println("Riwayat transaksi:");
        for (Transaksi transaksi : riwayat) {
            System.out.println("- " + transaksi.getJenis() + " "
                    + transaksi.getBatch().getKode() + ": "
                    + transaksi.getJumlah() + " " + satuan);
        }
    }
}
