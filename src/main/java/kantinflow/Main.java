package kantinflow;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        Barang susuKotak = new Barang("Susu Kotak", "kotak");
        List<PenerimaanStok> riwayat = new ArrayList<>();

        LocalDate tanggalPenerimaan = LocalDate.of(2026, 11, 17);
        LocalDate tanggalKedaluwarsa = LocalDate.of(2026, 12, 17);

        System.out.println("KantinFlow - Demo Penerimaan Stok");
        System.out.println("Barang: " + susuKotak.getNama());
        System.out.println("Stok awal: " + susuKotak.hitungSaldo()
                + " " + susuKotak.getSatuan());
        System.out.println();

        System.out.println("Skenario 1 - Terima batch SUSU-A sebanyak 5 kotak");
        PenerimaanStok penerimaanPertama = new PenerimaanStok(tanggalPenerimaan);
        DetailTransaksi detailPertama = penerimaanPertama.terima(
                susuKotak,
                "SUSU-A",
                5,
                tanggalPenerimaan,
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
        PenerimaanStok penerimaanKedua = new PenerimaanStok(tanggalPenerimaan);
        try {
            penerimaanKedua.terima(
                    susuKotak,
                    "SUSU-B",
                    0,
                    tanggalPenerimaan,
                    tanggalKedaluwarsa);
            riwayat.add(penerimaanKedua);
            System.out.println("Batch SUSU-B tersimpan");
        } catch (IllegalArgumentException exception) {
            System.out.println(exception.getMessage());
        }
        cetakRingkasan(susuKotak, riwayat);
    }

    private static void cetakRingkasan(Barang barang, List<PenerimaanStok> riwayat) {
        System.out.println("Saldo: " + barang.hitungSaldo() + " " + barang.getSatuan());
        System.out.println("Jumlah batch: " + barang.jumlahBatch());
        System.out.println("Jumlah riwayat: " + riwayat.size());
    }
}
