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
    private final List<Barang> daftarBarang;
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
        this.daftarBarang = new ArrayList<>();
        this.riwayat = new ArrayList<>();
        if (penyimpanan == null) {
            this.barang = new Barang("Susu Kotak", "kotak");
            this.daftarBarang.add(this.barang);
        } else {
            this.daftarBarang.addAll(penyimpanan.muatSemuaBarang());
            if (this.daftarBarang.isEmpty()) {
                throw new PenyimpananException("Data barang tidak ditemukan di database");
            }
            this.barang = this.daftarBarang.get(0);
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
                        pilihBarangAktif();
                        break;
                    case "2":
                        tambahBarang();
                        break;
                    case "3":
                        terimaStok();
                        break;
                    case "4":
                        keluarkanStokManual();
                        break;
                    case "5":
                        keluarkanStokFefo();
                        break;
                    case "6":
                        lihatSaldoDanRiwayat();
                        break;
                    case "7":
                        pemantauanKedaluwarsa();
                        break;
                    case "8":
                        buangStokKedaluwarsa();
                        break;
                    case "9":
                        aturStokMinimum();
                        break;
                    case "10":
                        System.out.println("Terima kasih. Program selesai.");
                        berjalan = false;
                        break;
                    default:
                        System.out.println("Pilihan tidak valid. Masukkan angka 1-10.");
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
        System.out.println("Barang aktif: " + barang.getNama()
                + " (" + barang.getSatuan() + ")");
        System.out.println("1. Pilih barang aktif");
        System.out.println("2. Tambah barang");
        System.out.println("3. Terima stok");
        System.out.println("4. Keluarkan stok dari batch yang dipilih (manual)");
        System.out.println("5. Keluarkan stok otomatis (FEFO)");
        System.out.println("6. Lihat saldo dan riwayat");
        System.out.println("7. Pemantauan Kedaluwarsa");
        System.out.println("8. Pembuangan Stok Kedaluwarsa");
        System.out.println("9. Atur stok minimum");
        System.out.println("10. Keluar");
    }

    private void pilihBarangAktif() {
        System.out.println("Daftar barang:");
        for (int i = 0; i < daftarBarang.size(); i++) {
            Barang item = daftarBarang.get(i);
            System.out.println((i + 1) + ". " + item.getNama()
                    + " (" + item.getSatuan() + ")");
        }

        String teks = baca("Pilih nomor barang (0 batal):");
        int nomor;
        try {
            nomor = Integer.parseInt(teks);
        } catch (NumberFormatException exception) {
            System.out.println("Nomor barang harus berupa angka.");
            return;
        }
        if (nomor == 0) {
            return;
        }
        if (nomor < 1 || nomor > daftarBarang.size()) {
            System.out.println("Nomor barang tidak valid.");
            return;
        }

        Barang dipilih = daftarBarang.get(nomor - 1);
        try {
            if (penyimpanan != null) {
                Barang terbaru = penyimpanan.muatBarang(dipilih.getId());
                daftarBarang.set(nomor - 1, terbaru);
                dipilih = terbaru;
            }
            this.barang = dipilih;
            this.riwayat.clear();
            if (penyimpanan != null) {
                this.riwayat.addAll(penyimpanan.muatRiwayat(this.barang));
            }
            System.out.println("Barang aktif: " + barang.getNama()
                    + " (" + barang.getSatuan() + ")");
        } catch (PenyimpananException exception) {
            System.out.println("Gagal memilih barang: " + exception.getMessage());
        }
    }

    private void tambahBarang() {
        String nama = baca("Nama barang:");
        String satuan = baca("Satuan:");
        try {
            Barang baru = new Barang(nama, satuan);
            if (penyimpanan != null) {
                baru = penyimpanan.simpanBarang(baru);
            }
            daftarBarang.add(baru);
            this.barang = baru;
            this.riwayat.clear();
            System.out.println("Barang ditambahkan: " + barang.getNama()
                    + " (" + barang.getSatuan() + "). Barang aktif diganti.");
        } catch (IllegalArgumentException | PenyimpananException exception) {
            System.out.println("Tambah barang gagal: " + exception.getMessage());
        }
    }

    private void aturStokMinimum() {
        System.out.println("Stok minimum saat ini: " + barang.getStokMinimum()
                + " " + barang.getSatuan());
        String teks = baca("Stok minimum baru (bilangan bulat >= 0):");
        int minimum;
        try {
            minimum = Integer.parseInt(teks);
        } catch (NumberFormatException exception) {
            System.out.println("Stok minimum harus berupa angka bulat.");
            return;
        }
        if (minimum < 0) {
            System.out.println("Stok minimum tidak boleh negatif.");
            return;
        }

        try {
            if (penyimpanan != null) {
                penyimpanan.simpanStokMinimum(barang, minimum);
            }
            barang.setStokMinimum(minimum);
            System.out.println("Stok minimum " + barang.getNama()
                    + " disimpan: " + minimum + " " + barang.getSatuan());
        } catch (IllegalArgumentException | PenyimpananException exception) {
            System.out.println("Gagal menyimpan stok minimum: "
                    + exception.getMessage());
        }
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
        System.out.println("Stok fisik: " + barang.hitungSaldo() + " " + barang.getSatuan());
        System.out.println("Stok minimum: " + barang.getStokMinimum()
                + " " + barang.getSatuan());
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

        int fisik = barang.hitungSaldo();
        int layak = barang.hitungStokLayak(tanggalPengecekan);
        int minimum = barang.getStokMinimum();
        System.out.println("Stok fisik: " + fisik + " " + barang.getSatuan());
        System.out.println("Stok layak per " + tanggalPengecekan + ": "
                + layak + " " + barang.getSatuan());
        System.out.println("Stok minimum: " + minimum + " " + barang.getSatuan());
        System.out.println("Status: " + (layak <= minimum ? "Perlu restok" : "Cukup"));
    }

    private boolean simpan(Transaksi transaksi) {
        if (penyimpanan == null) {
            return true;
        }
        try {
            penyimpanan.simpanTransaksi(barang, transaksi);
            return true;
        } catch (PenyimpananException | IllegalArgumentException exception) {
            System.out.println("Penyimpanan gagal: " + exception.getMessage());
            muatUlang();
            return false;
        }
    }

    private void muatUlang() {
        if (penyimpanan == null || barang == null || barang.getId() <= 0) {
            return;
        }
        try {
            Barang barangTerbaru = penyimpanan.muatBarang(barang.getId());
            for (int i = 0; i < daftarBarang.size(); i++) {
                if (daftarBarang.get(i).getId() == barang.getId()) {
                    daftarBarang.set(i, barangTerbaru);
                    break;
                }
            }
            this.barang = barangTerbaru;
            List<Transaksi> riwayatTerbaru = penyimpanan.muatRiwayat(barangTerbaru);
            this.riwayat.clear();
            this.riwayat.addAll(riwayatTerbaru);
        } catch (PenyimpananException | IllegalArgumentException exception) {
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
