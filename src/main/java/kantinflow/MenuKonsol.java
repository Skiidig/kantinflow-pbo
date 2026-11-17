package kantinflow;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import kantinflow.penyimpanan.PenyimpananException;
import kantinflow.penyimpanan.PenyimpananSQLite;

public class MenuKonsol {
    private Barang barang;
    private final List<Transaksi> riwayat;
    private final Scanner scanner;
    private final PenyimpananSQLite penyimpanan;

    public MenuKonsol(Scanner scanner) {
        this(scanner, null);
    }

    public MenuKonsol(Scanner scanner, PenyimpananSQLite penyimpanan) {
        if (scanner == null) {
            throw new IllegalArgumentException("Scanner wajib diisi");
        }
        this.scanner = scanner;
        this.penyimpanan = penyimpanan;
        this.riwayat = new ArrayList<>();
        if (penyimpanan == null) {
            this.barang = new Barang("Susu Kotak", "kotak");
        } else {
            this.barang = penyimpanan.muatBarang();
            this.riwayat.addAll(penyimpanan.muatRiwayat(this.barang));
        }
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
                        keluarkanStokManual();
                        break;
                    case "3":
                        keluarkanStokFefo();
                        break;
                    case "4":
                        lihatSaldoDanRiwayat();
                        break;
                    case "5":
                        pemantauanKedaluwarsa();
                        break;
                    case "6":
                        buangStokKedaluwarsa();
                        break;
                    case "7":
                        System.out.println("Terima kasih. Program selesai.");
                        berjalan = false;
                        break;
                    default:
                        System.out.println("Pilihan tidak valid. Masukkan angka 1-7.");
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
        System.out.println("2. Keluarkan stok dari batch yang dipilih (manual)");
        System.out.println("3. Keluarkan stok otomatis (FEFO)");
        System.out.println("4. Lihat saldo dan riwayat");
        System.out.println("5. Pemantauan Kedaluwarsa");
        System.out.println("6. Pembuangan Stok Kedaluwarsa");
        System.out.println("7. Keluar");
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
            if (!simpan(penerimaan)) {
                return;
            }
            riwayat.add(penerimaan);
            System.out.println("Penerimaan berhasil: " + kodeBatch + ", "
                    + jumlah + " " + barang.getSatuan()
                    + ". Saldo: " + barang.hitungSaldo() + " " + barang.getSatuan());
        } catch (IllegalArgumentException exception) {
            System.out.println("Penerimaan gagal: " + exception.getMessage());
        }
    }

    private void keluarkanStokManual() {
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
            if (!simpan(pengeluaran)) {
                return;
            }
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

    private void keluarkanStokFefo() {
        Integer jumlah = bacaJumlah();
        if (jumlah == null) {
            return;
        }

        LocalDate tanggalPengeluaran = bacaTanggal(
                "Tanggal pengeluaran (yyyy-MM-dd):");
        if (tanggalPengeluaran == null) {
            return;
        }

        PengeluaranStok pengeluaran = new PengeluaranStok(tanggalPengeluaran);
        try {
            pengeluaran.keluarkanFefo(barang, jumlah, tanggalPengeluaran);
            if (!simpan(pengeluaran)) {
                return;
            }
            riwayat.add(pengeluaran);
            System.out.println("Pengeluaran FEFO berhasil: " + jumlah + " "
                    + barang.getSatuan()
                    + ". Saldo: " + barang.hitungSaldo() + " "
                    + barang.getSatuan());
        } catch (StokTidakCukupException exception) {
            System.out.println(exception.getMessage());
        } catch (IllegalArgumentException exception) {
            System.out.println("Pengeluaran FEFO gagal: " + exception.getMessage());
        }
    }

    private void lihatSaldoDanRiwayat() {
        System.out.println("Saldo: " + barang.hitungSaldo() + " " + barang.getSatuan());
        System.out.println("Jumlah batch: " + barang.jumlahBatch());
        System.out.println("Jumlah riwayat: " + riwayat.size());

        if (barang.jumlahBatch() > 0) {
            System.out.println("Daftar batch:");
            for (BatchStok batch : barang.getDaftarBatch()) {
                System.out.println("- " + batch.getKode()
                        + ": " + batch.getSisaJumlah() + " " + barang.getSatuan()
                        + " (kedaluwarsa " + batch.getTanggalKedaluwarsa() + ")");
            }
        }

        if (riwayat.isEmpty()) {
            System.out.println("Riwayat transaksi: (kosong)");
            return;
        }

        System.out.println("Riwayat transaksi:");
        for (Transaksi transaksi : riwayat) {
            String alasan = transaksi.getAlasan();
            for (RincianTransaksi rincian : transaksi.getRincian()) {
                String baris = "- " + transaksi.getJenis() + " "
                        + rincian.getBatch().getKode() + ": "
                        + rincian.getJumlah() + " " + barang.getSatuan();
                if (!alasan.isEmpty()) {
                    baris += " | tanggal: " + transaksi.getTanggal()
                            + " | alasan: " + alasan;
                }
                System.out.println(baris);
            }
        }
    }

    private void buangStokKedaluwarsa() {
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

        String kodeBatch = baca("Kode batch yang dibuang:");
        BatchStok batch = cariBatch(kodeBatch);
        if (batch == null) {
            System.out.println("Batch tidak ditemukan.");
            return;
        }

        LocalDate tanggalPembuangan = bacaTanggal(
                "Tanggal pembuangan (yyyy-MM-dd):");
        if (tanggalPembuangan == null) {
            return;
        }

        Integer jumlah = bacaJumlah();
        if (jumlah == null) {
            return;
        }

        String alasan = baca("Alasan pembuangan:");
        PembuanganStok pembuangan = new PembuanganStok(tanggalPembuangan);
        try {
            pembuangan.buang(barang, batch, jumlah, alasan);
            if (!simpan(pembuangan)) {
                return;
            }
            riwayat.add(pembuangan);
            System.out.println("Pembuangan berhasil: " + batch.getKode() + ", "
                    + jumlah + " " + barang.getSatuan() + " pada "
                    + tanggalPembuangan + ". Saldo: "
                    + barang.hitungSaldo() + " " + barang.getSatuan());
        } catch (StokTidakCukupException exception) {
            System.out.println(exception.getMessage());
        } catch (IllegalArgumentException exception) {
            System.out.println("Pembuangan gagal: " + exception.getMessage());
        }
    }

    private void pemantauanKedaluwarsa() {
        LocalDate tanggalPengecekan = bacaTanggal(
                "Tanggal pengecekan (yyyy-MM-dd):");
        if (tanggalPengecekan == null) {
            return;
        }

        System.out.println("Pemantauan kedaluwarsa per " + tanggalPengecekan + ":");
        System.out.println("Kode batch | Sisa | Kedaluwarsa | Status");

        boolean adaBatchBersisa = false;
        for (BatchStok batch : barang.getDaftarBatch()) {
            if (batch.getSisaJumlah() <= 0) {
                continue;
            }
            adaBatchBersisa = true;
            StatusKedaluwarsa status = batch.statusPada(tanggalPengecekan);
            System.out.println("- " + batch.getKode()
                    + " | " + batch.getSisaJumlah() + " " + barang.getSatuan()
                    + " | " + batch.getTanggalKedaluwarsa()
                    + " | " + status.getLabel());
        }

        if (!adaBatchBersisa) {
            System.out.println("Tidak ada batch bersisa.");
        }
    }

    private boolean simpan(Transaksi transaksi) {
        if (penyimpanan == null) {
            return true;
        }
        try {
            penyimpanan.simpanTransaksi(transaksi);
            return true;
        } catch (PenyimpananException exception) {
            System.out.println("Penyimpanan gagal: " + exception.getMessage());
            muatUlang();
            return false;
        }
    }

    private void muatUlang() {
        if (penyimpanan == null) {
            return;
        }
        try {
            Barang barangTerbaru = penyimpanan.muatBarang();
            List<Transaksi> riwayatTerbaru = penyimpanan.muatRiwayat(barangTerbaru);
            this.barang = barangTerbaru;
            this.riwayat.clear();
            this.riwayat.addAll(riwayatTerbaru);
        } catch (PenyimpananException exception) {
            System.out.println("Gagal memuat ulang data: " + exception.getMessage());
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
