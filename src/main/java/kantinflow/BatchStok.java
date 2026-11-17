package kantinflow;

import java.time.LocalDate;

public class BatchStok {
    private final String kode;
    private final LocalDate tanggalMasuk;
    private final LocalDate tanggalKedaluwarsa;
    private int sisaJumlah;

    public BatchStok(
            String kode,
            LocalDate tanggalMasuk,
            LocalDate tanggalKedaluwarsa,
            int sisaJumlah) {
        if (kode == null || kode.isBlank()) {
            throw new IllegalArgumentException("Kode batch wajib diisi");
        }
        if (tanggalMasuk == null) {
            throw new IllegalArgumentException("Tanggal masuk wajib diisi");
        }
        if (tanggalKedaluwarsa == null) {
            throw new IllegalArgumentException("Tanggal kedaluwarsa wajib diisi");
        }
        if (tanggalKedaluwarsa.isBefore(tanggalMasuk)) {
            throw new IllegalArgumentException(
                    "Tanggal kedaluwarsa tidak boleh sebelum tanggal masuk");
        }
        if (sisaJumlah < 0) {
            throw new IllegalArgumentException("Sisa jumlah tidak boleh negatif");
        }

        this.kode = kode;
        this.tanggalMasuk = tanggalMasuk;
        this.tanggalKedaluwarsa = tanggalKedaluwarsa;
        this.sisaJumlah = sisaJumlah;
    }

    public String getKode() {
        return kode;
    }

    public LocalDate getTanggalMasuk() {
        return tanggalMasuk;
    }

    public LocalDate getTanggalKedaluwarsa() {
        return tanggalKedaluwarsa;
    }

    public int getSisaJumlah() {
        return sisaJumlah;
    }

    public boolean layakPada(LocalDate tanggalPengeluaran) {
        if (tanggalPengeluaran == null) {
            throw new IllegalArgumentException("Tanggal pengeluaran wajib diisi");
        }
        return sisaJumlah > 0
                && statusPada(tanggalPengeluaran) != StatusKedaluwarsa.KEDALUWARSA;
    }

    public StatusKedaluwarsa statusPada(LocalDate tanggalPengecekan) {
        if (tanggalPengecekan == null) {
            throw new IllegalArgumentException("Tanggal pengecekan wajib diisi");
        }
        if (tanggalKedaluwarsa.isBefore(tanggalPengecekan)) {
            return StatusKedaluwarsa.KEDALUWARSA;
        }
        LocalDate batasSegera = tanggalPengecekan.plusDays(3);
        if (!tanggalKedaluwarsa.isAfter(batasSegera)) {
            return StatusKedaluwarsa.SEGERA_KEDALUWARSA;
        }
        return StatusKedaluwarsa.AMAN;
    }

    public void kurangiSisa(int jumlah) {
        if (jumlah <= 0) {
            throw new IllegalArgumentException("Jumlah pengurangan harus lebih dari 0");
        }
        if (jumlah > sisaJumlah) {
            throw new IllegalArgumentException("Stok tidak cukup");
        }
        sisaJumlah -= jumlah;
    }
}
