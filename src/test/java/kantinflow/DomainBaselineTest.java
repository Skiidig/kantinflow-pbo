package kantinflow;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import kantinflow.penyimpanan.PenyimpananException;
import kantinflow.penyimpanan.PenyimpananSQLite;

/**
 * Baseline test domain tanpa dependency tambahan. Berada di package
 * {@code kantinflow} agar dapat menguji method package-private.
 *
 * <p>Semua pengujian memakai objek di memori atau SQLite {@code :memory:};
 * database aplikasi tidak pernah disentuh.</p>
 *
 * <p>Jalankan: {@code java -cp "out-test;out;lib/*" kantinflow.DomainBaselineTest}</p>
 */
public final class DomainBaselineTest {
    private static final LocalDate HARI = LocalDate.of(2026, 11, 20);
    private static int lulus;
    private static final List<String> gagal = new ArrayList<>();

    private interface Uji {
        void jalankan() throws Exception;
    }

    public static void main(String[] args) {
        uji("1. Pengeluaran manual batch valid", DomainBaselineTest::manualValid);
        uji("2. Pengeluaran manual batch kedaluwarsa ditolak", DomainBaselineTest::manualKedaluwarsaDitolak);
        uji("3. Batas tanggal kedaluwarsa (manual & FEFO)", DomainBaselineTest::batasKedaluwarsa);
        uji("4. Urutan alokasi FEFO", DomainBaselineTest::urutanFefo);
        uji("5. Batch masa depan dikecualikan dari FEFO", DomainBaselineTest::batchMasaDepanDikecualikan);
        uji("6. Konsistensi hitungStokLayak, batas tanggal masuk, & stok fisik",
                DomainBaselineTest::konsistensiStokLayakDanBatasMasuk);
        uji("7. Stok layak tidak cukup", DomainBaselineTest::stokLayakTidakCukup);
        uji("8. Atomicity saat transaksi gagal", DomainBaselineTest::atomicity);
        uji("9. Konsistensi tanggal transaksi", DomainBaselineTest::konsistensiTanggal);
        uji("10. Normalisasi input (trim) & duplikat kode", DomainBaselineTest::normalisasiInput);
        uji("11. SQLite in-memory round-trip", DomainBaselineTest::sqliteRoundTrip);
        uji("12. Kompatibilitas mode demo", DomainBaselineTest::modeDemo);

        System.out.println();
        System.out.println("Lulus: " + lulus + ", Gagal: " + gagal.size());
        if (!gagal.isEmpty()) {
            gagal.forEach(pesan -> System.out.println("GAGAL: " + pesan));
            System.exit(1);
        }
    }

    // ---------- skenario ----------

    private static void manualValid() {
        Barang barang = barang();
        BatchStok batch = terima(barang, "A-1", 10, HARI.minusDays(2), HARI.plusDays(10));
        PengeluaranStok keluar = new PengeluaranStok(HARI);
        keluar.keluarkan(barang, batch, 4);
        sama(6, batch.getSisaJumlah(), "sisa batch");
        sama(4, keluar.getJumlah(), "jumlah transaksi");
        sama("Pengeluaran", keluar.getJenis(), "jenis");
        sama(1, keluar.getRincian().size(), "jumlah rincian");
        sama(batch, keluar.getBatch(), "batch transaksi");
    }

    private static void manualKedaluwarsaDitolak() {
        Barang barang = barang();
        BatchStok batch = terima(barang, "A-1", 10, HARI.minusDays(10), HARI.minusDays(1));
        PengeluaranStok keluar = new PengeluaranStok(HARI);
        lempar(IllegalArgumentException.class, () -> keluar.keluarkan(barang, batch, 1));
        sama(10, batch.getSisaJumlah(), "sisa tidak berubah");
        benar(keluar.getRincian().isEmpty(), "rincian tetap kosong");
    }

    private static void batasKedaluwarsa() {
        Barang barang = barang();
        BatchStok tepat = terima(barang, "A-1", 5, HARI.minusDays(3), HARI);
        PengeluaranStok manual = new PengeluaranStok(HARI);
        manual.keluarkan(barang, tepat, 1);
        sama(4, tepat.getSisaJumlah(), "manual pada hari kedaluwarsa diizinkan");
        PengeluaranStok fefo = new PengeluaranStok(HARI);
        fefo.keluarkanFefo(barang, 2);
        sama(2, tepat.getSisaJumlah(), "FEFO pada hari kedaluwarsa diizinkan");

        PengeluaranStok besok = new PengeluaranStok(HARI.plusDays(1));
        lempar(IllegalArgumentException.class, () -> besok.keluarkan(barang, tepat, 1));
        PengeluaranStok fefoBesok = new PengeluaranStok(HARI.plusDays(1));
        lempar(StokTidakCukupException.class, () -> fefoBesok.keluarkanFefo(barang, 1));
        sama(2, tepat.getSisaJumlah(), "sisa tidak berubah setelah hari kedaluwarsa");
    }

    private static void urutanFefo() {
        Barang barang = barang();
        BatchStok a = terima(barang, "A-LAMBAT", 5, HARI.minusDays(5), HARI.plusDays(30));
        BatchStok b = terima(barang, "B-AWAL", 3, HARI.minusDays(5), HARI.plusDays(10));
        BatchStok c = terima(barang, "C-MASUK-BELAKANGAN", 4, HARI.minusDays(2), HARI.plusDays(10));
        PengeluaranStok keluar = new PengeluaranStok(HARI);
        keluar.keluarkanFefo(barang, 9);
        List<RincianTransaksi> r = keluar.getRincian();
        sama(3, r.size(), "jumlah rincian");
        sama(b, r.get(0).getBatch(), "1: kedaluwarsa sama, masuk lebih awal");
        sama(3, r.get(0).getJumlah(), "jumlah 1");
        sama(c, r.get(1).getBatch(), "2: kedaluwarsa sama, masuk lebih akhir");
        sama(4, r.get(1).getJumlah(), "jumlah 2");
        sama(a, r.get(2).getBatch(), "3: kedaluwarsa terjauh");
        sama(2, r.get(2).getJumlah(), "jumlah 3");
        sama("Pengeluaran FEFO", keluar.getJenis(), "jenis");
        sama(3, a.getSisaJumlah(), "sisa A");

        // Tie-breaker kode: tidak membedakan huruf besar-kecil.
        Barang kedua = barang();
        BatchStok kecil = terima(kedua, "a-1", 1, HARI.minusDays(1), HARI.plusDays(5));
        BatchStok besar = terima(kedua, "B-1", 1, HARI.minusDays(1), HARI.plusDays(5));
        List<BatchStok> urut = new ArrayList<>(List.of(besar, kecil));
        urut.sort(FefoAllocator.URUTAN_FEFO);
        sama(kecil, urut.get(0), "a-1 sebelum B-1");
        PengeluaranStok tie = new PengeluaranStok(HARI);
        tie.keluarkanFefo(kedua, 1);
        sama(kecil, tie.getBatch(), "alokasi memakai comparator yang sama");
    }

    private static void batchMasaDepanDikecualikan() {
        Barang barang = barang();
        BatchStok sekarang = terima(barang, "NOW", 5, HARI.minusDays(1), HARI.plusDays(20));
        BatchStok depan = terima(barang, "FUTURE", 10, HARI.plusDays(5), HARI.plusDays(6));
        PengeluaranStok keluar = new PengeluaranStok(HARI);
        keluar.keluarkanFefo(barang, 3);
        sama(1, keluar.getRincian().size(), "hanya satu batch");
        sama(sekarang, keluar.getBatch(), "batch yang sudah masuk");
        sama(10, depan.getSisaJumlah(), "batch masa depan tidak tersentuh");

        // Tanpa batch yang memenuhi syarat, transaksi ditolak walau fisik ada.
        Barang hanyaDepan = barang();
        BatchStok f = terima(hanyaDepan, "FUTURE", 10, HARI.plusDays(5), HARI.plusDays(30));
        PengeluaranStok ditolak = new PengeluaranStok(HARI);
        lempar(StokTidakCukupException.class, () -> ditolak.keluarkanFefo(hanyaDepan, 1));
        sama(10, f.getSisaJumlah(), "sisa tidak berubah");
        List<RincianTransaksi> plan = new FefoAllocator().susun(barang, 2, HARI);
        sama(1, plan.size(), "allocator mengecualikan batch masa depan");
        sama(sekarang, plan.get(0).getBatch(), "allocator memilih batch yang sudah masuk");
        lempar(StokTidakCukupException.class,
                () -> new FefoAllocator().susun(barang, 3, HARI));
    }

    private static void konsistensiStokLayakDanBatasMasuk() {
        Barang barang = barang();
        barang.setStokMinimum(5);
        // Batch 1: sudah masuk sebelum HARI, belum kedaluwarsa (sisa 4)
        terima(barang, "B-LAMA", 4, HARI.minusDays(3), HARI.plusDays(5));
        // Batch 2: masuk tepat pada HARI (boundary tanggal masuk, sisa 3)
        terima(barang, "B-TEPAT-MASUK", 3, HARI, HARI.plusDays(10));
        // Batch 3: masuk setelah HARI / masa depan (sisa 10)
        BatchStok depan = terima(barang, "B-DEPAN", 10, HARI.plusDays(1), HARI.plusDays(20));
        // Batch 4: sudah kedaluwarsa pada HARI (sisa 6)
        terima(barang, "B-KEDALUWARSA", 6, HARI.minusDays(10), HARI.minusDays(1));
        // Batch 5: habis / sisa 0
        BatchStok habis = terima(barang, "B-HABIS", 2, HARI.minusDays(2), HARI.plusDays(8));
        new PengeluaranStok(HARI).keluarkan(barang, habis, 2);

        // Stok fisik tetap menghitung seluruh batch bersisa (4 + 3 + 10 + 6 + 0 = 23).
        sama(23, barang.hitungSaldo(), "stok fisik tidak berubah maknanya");

        // Pada HARI: hanya B-LAMA (4) + B-TEPAT-MASUK (3) = 7 yang layak (> stokMinimum 5).
        sama(7, barang.hitungStokLayak(HARI), "stok layak pada HARI");
        benar(!depan.layakPada(HARI), "batch masa depan tidak layak pada HARI");
        benar(depan.layakPada(HARI.plusDays(1)), "batch layak tepat pada tanggal masuknya");

        // Sebelum B-TEPAT-MASUK masuk (HARI - 1): hanya B-LAMA (4) + B-KEDALUWARSA (6, tepat hari exp) = 10.
        sama(10, barang.hitungStokLayak(HARI.minusDays(1)),
                "batas sehari sebelum tanggal masuk B-TEPAT-MASUK");

        // Sebelum semua batch masuk (HARI - 20): stok layak = 0 (perlu restok), walau fisik = 23.
        sama(0, barang.hitungStokLayak(HARI.minusDays(20)),
                "stok layak 0 sebelum tanggal masuk semua batch");
        benar(barang.hitungStokLayak(HARI.minusDays(20)) <= barang.getStokMinimum(),
                "KPI perlu restok konsisten saat stok layak <= minimum");

        // Invariant: hitungStokLayak(HARI) == jumlah maksimum yang dapat dialokasikan FEFO pada HARI.
        int layakHariIni = barang.hitungStokLayak(HARI);
        List<RincianTransaksi> alokasiMaks = new FefoAllocator().susun(barang, layakHariIni, HARI);
        int totalAlokasi = 0;
        for (RincianTransaksi r : alokasiMaks) {
            totalAlokasi += r.getJumlah();
        }
        sama(layakHariIni, totalAlokasi, "total alokasi FEFO == hitungStokLayak");
        lempar(StokTidakCukupException.class,
                () -> new FefoAllocator().susun(barang, layakHariIni + 1, HARI));
    }

    private static void stokLayakTidakCukup() {
        Barang barang = barang();
        BatchStok layak = terima(barang, "OK", 3, HARI.minusDays(1), HARI.plusDays(5));
        BatchStok basi = terima(barang, "BASI", 50, HARI.minusDays(20), HARI.minusDays(1));
        BatchStok depan = terima(barang, "DEPAN", 50, HARI.plusDays(2), HARI.plusDays(9));
        sama(103, barang.hitungSaldo(), "saldo fisik mencukupi");
        PengeluaranStok fefo = new PengeluaranStok(HARI);
        lempar(StokTidakCukupException.class, () -> fefo.keluarkanFefo(barang, 4));
        sama(3, layak.getSisaJumlah(), "sisa layak");
        sama(50, basi.getSisaJumlah(), "sisa basi");
        sama(50, depan.getSisaJumlah(), "sisa depan");

        PengeluaranStok manual = new PengeluaranStok(HARI);
        lempar(StokTidakCukupException.class, () -> manual.keluarkan(barang, layak, 4));
        lempar(StokTidakCukupException.class, () -> layak.kurangiSisa(4));
        sama(3, layak.getSisaJumlah(), "sisa setelah penolakan");
    }

    private static void atomicity() {
        Barang barang = barang();
        BatchStok a = terima(barang, "A", 2, HARI.minusDays(5), HARI.plusDays(3));
        BatchStok b = terima(barang, "B", 2, HARI.minusDays(5), HARI.plusDays(6));
        PengeluaranStok melebihi = new PengeluaranStok(HARI);
        lempar(StokTidakCukupException.class, () -> melebihi.keluarkanFefo(barang, 5));
        sama(2, a.getSisaJumlah(), "sisa A");
        sama(2, b.getSisaJumlah(), "sisa B");
        benar(melebihi.getRincian().isEmpty(), "rincian kosong setelah gagal");

        PengeluaranStok tanggalBeda = new PengeluaranStok(HARI);
        lempar(IllegalArgumentException.class,
                () -> tanggalBeda.keluarkanFefo(barang, 3, HARI.plusDays(1)));
        sama(2, a.getSisaJumlah(), "sisa A setelah tanggal beda");
        sama(2, b.getSisaJumlah(), "sisa B setelah tanggal beda");

        // Setelah kegagalan, transaksi yang sama masih bisa dipakai dengan benar.
        melebihi.keluarkanFefo(barang, 3);
        sama(0, a.getSisaJumlah(), "sisa A sukses");
        sama(1, b.getSisaJumlah(), "sisa B sukses");
        lempar(IllegalStateException.class, () -> melebihi.keluarkanFefo(barang, 1));

        int batchSebelum = barang.jumlahBatch();
        PenerimaanStok duplikat = new PenerimaanStok(HARI);
        lempar(IllegalArgumentException.class,
                () -> duplikat.terima(barang, " a ", 5, HARI, HARI.plusDays(5)));
        sama(batchSebelum, barang.jumlahBatch(), "batch tidak bertambah");
        benar(duplikat.getDetail() == null, "detail penerimaan tetap kosong");
    }

    private static void konsistensiTanggal() {
        Barang barang = barang();
        BatchStok batch = terima(barang, "A", 10, HARI, HARI.plusDays(10));
        PengeluaranStok sebelumMasuk = new PengeluaranStok(HARI.minusDays(1));
        lempar(IllegalArgumentException.class, () -> sebelumMasuk.keluarkan(barang, batch, 1));
        PembuanganStok buang = new PembuanganStok(HARI.minusDays(1));
        lempar(IllegalArgumentException.class, () -> buang.buang(barang, batch, 1, "rusak"));
        PengeluaranStok fefoBeda = new PengeluaranStok(HARI);
        lempar(IllegalArgumentException.class,
                () -> fefoBeda.keluarkanFefo(barang, 1, HARI.plusDays(1)));
        lempar(IllegalArgumentException.class, () -> fefoBeda.keluarkanFefo(barang, 1, null));
        sama(10, batch.getSisaJumlah(), "sisa tidak berubah");

        PengeluaranStok sama = new PengeluaranStok(HARI);
        sama.keluarkanFefo(barang, 2, HARI);
        sama(HARI, sama.getTanggal(), "tanggal transaksi");
        sama(8, batch.getSisaJumlah(), "sisa setelah FEFO tanggal sama");
    }

    private static void normalisasiInput() {
        Barang barang = new Barang("  Susu UHT  ", " kotak ");
        sama("Susu UHT", barang.getNama(), "nama di-trim");
        sama("kotak", barang.getSatuan(), "satuan di-trim");
        BatchStok batch = new BatchStok("  X-1 ", HARI, HARI.plusDays(1), 1);
        sama("X-1", batch.getKode(), "kode di-trim");
        terima(barang, "SUSU-A", 1, HARI, HARI.plusDays(5));
        lempar(IllegalArgumentException.class,
                () -> new PenerimaanStok(HARI).terima(barang, " susu-a ", 1, HARI, HARI.plusDays(5)));
        lempar(IllegalArgumentException.class,
                () -> new PenerimaanStok(HARI).terima(barang, "   ", 1, HARI, HARI.plusDays(5)));
        lempar(IllegalArgumentException.class,
                () -> new PenerimaanStok(HARI).terima(barang, null, 1, HARI, HARI.plusDays(5)));
    }

    private static void sqliteRoundTrip() {
        try (PenyimpananSQLite db = new PenyimpananSQLite(":memory:")) {
            Barang roti = db.simpanBarang(new Barang("  Roti Isi ", "buah"));
            sama("Roti Isi", roti.getNama(), "nama tersimpan ter-trim");
            lempar(PenyimpananException.class,
                    () -> db.simpanBarang(new Barang("roti isi", "buah")));

            BatchStok awal = terima(roti, "ROTI-1", 10, HARI.minusDays(3), HARI.plusDays(2));
            PenerimaanStok p1 = PenerimaanStok.dariPenyimpanan(HARI.minusDays(3), awal, 10);
            db.simpanTransaksi(roti, p1);
            BatchStok kedua = terima(roti, "ROTI-2", 8, HARI.minusDays(1), HARI.plusDays(9));
            db.simpanTransaksi(roti, PenerimaanStok.dariPenyimpanan(HARI.minusDays(1), kedua, 8));

            PengeluaranStok fefo = new PengeluaranStok(HARI);
            fefo.keluarkanFefo(roti, 12);
            db.simpanTransaksi(roti, fefo);

            PengeluaranStok manual = new PengeluaranStok(HARI);
            manual.keluarkan(roti, kedua, 1);
            db.simpanTransaksi(roti, manual);

            PembuanganStok buang = new PembuanganStok(HARI);
            buang.buang(roti, kedua, 1, "jatuh");
            db.simpanTransaksi(roti, buang);

            Barang muat = db.muatBarang(roti.getId());
            sama(2, muat.jumlahBatch(), "jumlah batch dimuat");
            sama(0, sisa(muat, "ROTI-1"), "sisa ROTI-1");
            sama(4, sisa(muat, "ROTI-2"), "sisa ROTI-2");
            sama(4, muat.hitungSaldo(), "saldo dimuat");

            List<Transaksi> riwayat = db.muatRiwayat(muat);
            sama(5, riwayat.size(), "jumlah riwayat");
            sama("Penerimaan", riwayat.get(0).getJenis(), "riwayat 0");
            sama("Pengeluaran FEFO", riwayat.get(2).getJenis(), "riwayat 2");
            sama(2, riwayat.get(2).getRincian().size(), "FEFO dua rincian");
            sama("Pengeluaran", riwayat.get(3).getJenis(), "riwayat 3");
            sama("Pembuangan", riwayat.get(4).getJenis(), "riwayat 4");
            sama("jatuh", riwayat.get(4).getAlasan(), "alasan pembuangan");
        }
    }

    private static void modeDemo() {
        PrintStream asli = System.out;
        ByteArrayOutputStream tangkap = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(tangkap, true, StandardCharsets.UTF_8));
            Main.main(new String[]{"demo"});
        } finally {
            System.setOut(asli);
        }
        String keluaran = tangkap.toString(StandardCharsets.UTF_8);
        benar(keluaran.contains("Batch SUSU-A tersimpan: 5 kotak"), "skenario 1");
        benar(keluaran.contains("Jumlah harus lebih dari 0"), "skenario 2");
        benar(keluaran.contains("Pengeluaran SUSU-A tersimpan: 2 kotak"), "skenario 3");
        benar(keluaran.contains("Stok tidak cukup: diminta 4 kotak, tersedia 3 kotak"),
                "skenario 4");
        benar(keluaran.contains("Saldo: 3 kotak"), "saldo akhir demo");
    }

    // ---------- helper ----------

    private static Barang barang() {
        return new Barang("Uji", "pcs");
    }

    private static BatchStok terima(Barang barang, String kode, int jumlah,
            LocalDate masuk, LocalDate kedaluwarsa) {
        return new PenerimaanStok(masuk)
                .terima(barang, kode, jumlah, masuk, kedaluwarsa).getBatch();
    }

    private static int sisa(Barang barang, String kode) {
        for (BatchStok batch : barang.getDaftarBatch()) {
            if (batch.getKode().equals(kode)) {
                return batch.getSisaJumlah();
            }
        }
        throw new AssertionError("Batch tidak ditemukan: " + kode);
    }

    private static void uji(String nama, Uji uji) {
        try {
            uji.jalankan();
            lulus++;
            System.out.println("LULUS  " + nama);
        } catch (Throwable throwable) {
            gagal.add(nama + " -> " + throwable);
            System.out.println("GAGAL  " + nama + " -> " + throwable);
        }
    }

    private static void lempar(Class<? extends Throwable> tipe, Runnable aksi) {
        try {
            aksi.run();
        } catch (Throwable throwable) {
            if (tipe.equals(throwable.getClass())) {
                return;
            }
            throw new AssertionError("Diharapkan " + tipe.getSimpleName()
                    + " tetapi " + throwable);
        }
        throw new AssertionError("Diharapkan " + tipe.getSimpleName()
                + " tetapi tidak ada exception");
    }

    private static void benar(boolean kondisi, String pesan) {
        if (!kondisi) {
            throw new AssertionError(pesan);
        }
    }

    private static void sama(Object diharapkan, Object aktual, String pesan) {
        if (!diharapkan.equals(aktual)) {
            throw new AssertionError(pesan + ": diharapkan <" + diharapkan
                    + "> tetapi <" + aktual + ">");
        }
    }
}
