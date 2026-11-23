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

        this.kode = kode.trim();
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

    /**
     * Batch layak dipakai pada tanggal acuan bila sisa positif, tanggal masuk
     * tidak melewati tanggal acuan, dan belum kedaluwarsa pada tanggal acuan.
     */
    public boolean layakPada(LocalDate tanggalPengeluaran) {
        if (tanggalPengeluaran == null) {
            throw new IllegalArgumentException("Tanggal pengeluaran wajib diisi");
        }
        return sisaJumlah > 0
                && !tanggalMasuk.isAfter(tanggalPengeluaran)
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

    /**
     * Mengurangi sisa batch. Hanya untuk transaksi domain dalam package ini
     * (PengeluaranStok, PembuanganStok) agar mutasi stok selalu tercatat.
     */
    void kurangiSisa(int jumlah) {
        if (jumlah <= 0) {
            throw new IllegalArgumentException("Jumlah pengurangan harus lebih dari 0");
        }
        if (jumlah > sisaJumlah) {
            throw new StokTidakCukupException("Stok tidak cukup: diminta "
                    + jumlah + ", tersedia " + sisaJumlah);
        }
        sisaJumlah -= jumlah;
    }
}
