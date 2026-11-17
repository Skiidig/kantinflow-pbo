package kantinflow;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MenuKonsol {
    private final Barang barang;
    private final List<Transaksi> riwayat;
    private final Scanner scanner;

    public MenuKonsol(Scanner scanner) {
        if (scanner == null) {
            throw new IllegalArgumentException("Scanner wajib diisi");
        }
        this.scanner = scanner;
        this.barang = new Barang("Susu Kotak", "kotak");
        this.riwayat = new ArrayList<>();
    }

    public void jalankan() {
        try {
            boolean berjalan = true;
            while (berjalan) {
                tampilkanMenu();
                String pilihan = baca("Pilih menu:");

                switch (pilihan) {
                    case "1":
                        terimaStok();
                        break;
                    case "2":
                        keluarkanStok();
                        break;
                    case "3":
                        lihatSaldoDanRiwayat();
                        break;
                    case "4":
                        System.out.println("Terima kasih. Program selesai.");
                        berjalan = false;
                        break;
                    default:
                        System.out.println("Pilihan tidak valid. Masukkan angka 1-4.");
                }
            }
        } catch (InputSelesaiException exception) {
            System.out.println();
            System.out.println("Input berakhir. Program selesai.");
        }
    }

    private void tampilkanMenu() {
        System.out.println();
        System.out.println("=== KantinFlow ===");
        System.out.println("Barang: " + barang.getNama());
        System.out.println("1. Terima stok");
        System.out.println("2. Keluarkan stok dari batch yang dipilih");
        System.out.println("3. Lihat saldo dan riwayat");
        System.out.println("4. Keluar");
    }

    private void terimaStok() {
        String kodeBatch = baca("Kode batch:");
        Integer jumlah = bacaJumlah();
        if (jumlah == null) {
            return;
        }

        LocalDate tanggalMasuk = bacaTanggal("Tanggal masuk (yyyy-MM-dd):");
        if (tanggalMasuk == null) {
            return;
        }

        LocalDate tanggalKedaluwarsa = bacaTanggal("Tanggal kedaluwarsa (yyyy-MM-dd):");
        if (tanggalKedaluwarsa == null) {
            return;
        }

        PenerimaanStok penerimaan = new PenerimaanStok(tanggalMasuk);
        try {
            penerimaan.terima(
                    barang,
                    kodeBatch,
                    jumlah,
                    tanggalMasuk,
                    tanggalKedaluwarsa);
            riwayat.add(penerimaan);
            System.out.println("Penerimaan berhasil: " + kodeBatch + ", "
                    + jumlah + " " + barang.getSatuan()
                    + ". Saldo: " + barang.hitungSaldo() + " " + barang.getSatuan());
        } catch (IllegalArgumentException exception) {
            System.out.println("Penerimaan gagal: " + exception.getMessage());
        }
    }

    private void keluarkanStok() {
        if (barang.jumlahBatch() == 0) {
            System.out.println("Belum ada batch. Lakukan penerimaan stok terlebih dahulu.");
            return;
        }

        System.out.println("Batch tersedia:");
        for (BatchStok batch : barang.getDaftarBatch()) {
            System.out.println("- " + batch.getKode()
                    + ": " + batch.getSisaJumlah() + " " + barang.getSatuan()
                    + " (kedaluwarsa " + batch.getTanggalKedaluwarsa() + ")");
        }

        String kodeBatch = baca("Kode batch yang dikeluarkan:");
        BatchStok batch = cariBatch(kodeBatch);
        if (batch == null) {
            System.out.println("Batch tidak ditemukan.");
            return;
        }

        Integer jumlah = bacaJumlah();
        if (jumlah == null) {
            return;
        }

        PengeluaranStok pengeluaran = new PengeluaranStok(LocalDate.now());
        try {
            pengeluaran.keluarkan(barang, batch, jumlah);
            riwayat.add(pengeluaran);
            System.out.println("Pengeluaran berhasil: " + batch.getKode() + ", "
                    + jumlah + " " + barang.getSatuan()
                    + ". Saldo: " + barang.hitungSaldo() + " " + barang.getSatuan());
        } catch (StokTidakCukupException exception) {
            System.out.println(exception.getMessage());
        } catch (IllegalArgumentException exception) {
            System.out.println("Pengeluaran gagal: " + exception.getMessage());
        }
    }

    private void lihatSaldoDanRiwayat() {
        System.out.println("Saldo: " + barang.hitungSaldo() + " " + barang.getSatuan());
        System.out.println("Jumlah batch: " + barang.jumlahBatch());
        System.out.println("Jumlah riwayat: " + riwayat.size());

        if (riwayat.isEmpty()) {
            System.out.println("Riwayat transaksi: (kosong)");
            return;
        }

        System.out.println("Riwayat transaksi:");
        for (Transaksi transaksi : riwayat) {
            System.out.println("- " + transaksi.getJenis() + " "
                    + transaksi.getBatch().getKode() + ": "
                    + transaksi.getJumlah() + " " + barang.getSatuan());
        }
    }

    private BatchStok cariBatch(String kodeBatch) {
        for (BatchStok batch : barang.getDaftarBatch()) {
            if (batch.getKode().equalsIgnoreCase(kodeBatch)) {
                return batch;
            }
        }
        return null;
    }

    private Integer bacaJumlah() {
        String teks = baca("Jumlah:");
        try {
            return Integer.valueOf(teks);
        } catch (NumberFormatException exception) {
            System.out.println("Jumlah harus berupa angka bulat.");
            return null;
        }
    }

    private LocalDate bacaTanggal(String label) {
        String teks = baca(label);
        try {
            return LocalDate.parse(teks);
        } catch (DateTimeParseException exception) {
            System.out.println("Format tanggal tidak valid. Gunakan yyyy-MM-dd.");
            return null;
        }
    }

    private String baca(String label) {
        System.out.println(label);
        if (!scanner.hasNextLine()) {
            throw new InputSelesaiException();
        }
        return scanner.nextLine().trim();
    }

    private static final class InputSelesaiException extends RuntimeException {
    }
}
