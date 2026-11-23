package kantinflow;

import java.awt.GraphicsEnvironment;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import kantinflow.gui.JendelaKantinFlow;
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
        uji("13. Multi-item & multi-batch loading serta urutan barang/batch",
                DomainBaselineTest::multiBarangDanUrutanBatch);
        uji("14. Riwayat multi-rincian & validasi batch rujukan",
                DomainBaselineTest::riwayatMultiRincianDanRujukan);
        uji("15. Penanganan tanggal & data database korup",
                DomainBaselineTest::penangananDataKorup);
        uji("16. Pembersihan koneksi saat konstruktor PenyimpananSQLite gagal",
                DomainBaselineTest::pembersihanKoneksiKonstruktorGagal);
        uji("17. Rollback transaksi SQLite saat gagal di tengah transaksi",
                DomainBaselineTest::rollbackTransaksiSqlite);
        uji("18. Akses konkuren multi-thread PenyimpananSQLite",
                DomainBaselineTest::aksesKonkurenSqlite);
        uji("19. Pemulihan state in-memory MenuKonsol saat simpan gagal",
                DomainBaselineTest::pemulihanStateMenuKonsol);
        uji("20. GUI lazy refresh, getRiwayat, duplicate guard, & clean shutdown",
                DomainBaselineTest::guiLazyRefreshGuardDanShutdown);

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

    private static void multiBarangDanUrutanBatch() {
        try (PenyimpananSQLite db = new PenyimpananSQLite(":memory:")) {
            // Barang 1 sudah ada dari seed ("Susu Kotak")
            Barang b2 = db.simpanBarang(new Barang("Teh Botol", "botol"));
            Barang b3 = db.simpanBarang(new Barang("Air Mineral", "botol"));
            db.simpanStokMinimum(b2, 5);
            db.simpanStokMinimum(b3, 12);

            Barang b1 = db.muatBarang(1);
            // Sisipkan batch berselang-seling lintas barang untuk menguji urutan ORDER BY id
            PenerimaanStok p1 = new PenerimaanStok(HARI.minusDays(5));
            p1.terima(b1, "SUSU-01", 6, HARI.minusDays(5), HARI.plusDays(10));
            db.simpanTransaksi(b1, p1);

            PenerimaanStok p2 = new PenerimaanStok(HARI.minusDays(4));
            p2.terima(b2, "TEH-01", 8, HARI.minusDays(4), HARI.plusDays(15));
            db.simpanTransaksi(b2, p2);

            PenerimaanStok p3 = new PenerimaanStok(HARI.minusDays(3));
            p3.terima(b1, "SUSU-02", 4, HARI.minusDays(3), HARI.plusDays(20));
            db.simpanTransaksi(b1, p3);

            PenerimaanStok p4 = new PenerimaanStok(HARI.minusDays(2));
            p4.terima(b3, "AIR-01", 15, HARI.minusDays(2), HARI.plusDays(30));
            db.simpanTransaksi(b3, p4);

            PenerimaanStok p5 = new PenerimaanStok(HARI.minusDays(1));
            p5.terima(b2, "TEH-02", 7, HARI.minusDays(1), HARI.plusDays(25));
            db.simpanTransaksi(b2, p5);

            List<Barang> semua = db.muatSemuaBarang();
            sama(3, semua.size(), "jumlah barang dimuat");
            sama(1, semua.get(0).getId(), "urutan barang 1");
            sama(b2.getId(), semua.get(1).getId(), "urutan barang 2");
            sama(b3.getId(), semua.get(2).getId(), "urutan barang 3");
            sama(5, semua.get(1).getStokMinimum(), "stok minimum barang 2");
            sama(12, semua.get(2).getStokMinimum(), "stok minimum barang 3");

            sama(2, semua.get(0).jumlahBatch(), "batch barang 1");
            sama("SUSU-01", semua.get(0).getDaftarBatch().get(0).getKode(), "urutan batch 1.1");
            sama("SUSU-02", semua.get(0).getDaftarBatch().get(1).getKode(), "urutan batch 1.2");

            sama(2, semua.get(1).jumlahBatch(), "batch barang 2");
            sama("TEH-01", semua.get(1).getDaftarBatch().get(0).getKode(), "urutan batch 2.1");
            sama("TEH-02", semua.get(1).getDaftarBatch().get(1).getKode(), "urutan batch 2.2");

            // Bandingkan dengan muatBarang(id) individual
            for (Barang dariSemua : semua) {
                Barang tunggal = db.muatBarang(dariSemua.getId());
                sama(tunggal.getNama(), dariSemua.getNama(), "nama cocok");
                sama(tunggal.hitungSaldo(), dariSemua.hitungSaldo(), "saldo cocok");
                sama(tunggal.jumlahBatch(), dariSemua.jumlahBatch(), "jumlah batch cocok");
            }
        }
    }

    private static void riwayatMultiRincianDanRujukan() {
        try (PenyimpananSQLite db = new PenyimpananSQLite(":memory:")) {
            Barang barang = db.muatBarang(1);
            for (int i = 1; i <= 3; i++) {
                PenerimaanStok masuk = new PenerimaanStok(HARI.minusDays(5));
                masuk.terima(barang, "B-" + i, 5, HARI.minusDays(5), HARI.plusDays(i * 3));
                db.simpanTransaksi(barang, masuk);
            }
            PengeluaranStok fefo = new PengeluaranStok(HARI);
            fefo.keluarkanFefo(barang, 12);
            db.simpanTransaksi(barang, fefo);

            Barang dimuat = db.muatSemuaBarang().get(0);
            List<Transaksi> riwayat = db.muatRiwayat(dimuat);
            sama(4, riwayat.size(), "3 penerimaan + 1 FEFO");
            Transaksi txFefo = riwayat.get(3);
            sama("Pengeluaran FEFO", txFefo.getJenis(), "jenis FEFO");
            sama(12, txFefo.getJumlah(), "total jumlah FEFO");
            sama(3, txFefo.getRincian().size(), "3 rincian FEFO");
            sama("B-1", txFefo.getRincian().get(0).getBatch().getKode(), "rincian 1");
            sama(5, txFefo.getRincian().get(0).getJumlah(), "jumlah rincian 1");
            sama("B-2", txFefo.getRincian().get(1).getBatch().getKode(), "rincian 2");
            sama(5, txFefo.getRincian().get(1).getJumlah(), "jumlah rincian 2");
            sama("B-3", txFefo.getRincian().get(2).getBatch().getKode(), "rincian 3");
            sama(2, txFefo.getRincian().get(2).getJumlah(), "jumlah rincian 3");
            benar(dimuat.memilikiBatch(txFefo.getRincian().get(0).getBatch()),
                    "referensi batch rincian terhubung ke daftarBatch barang");

            // Jika objek Barang belum memuat batch rujukan, muatRiwayat melempar PenyimpananException
            Barang tanpaBatch = new Barang(dimuat.getId(), dimuat.getNama(), dimuat.getSatuan());
            lempar(PenyimpananException.class, () -> db.muatRiwayat(tanpaBatch));
        }
    }

    private static void penangananDataKorup() throws Exception {
        Path file = Files.createTempFile("kf-corrupt-", ".db");
        try {
            try (PenyimpananSQLite db = new PenyimpananSQLite(file.toString())) {
                Barang b = db.muatBarang(1);
                PenerimaanStok masuk = new PenerimaanStok(HARI);
                masuk.terima(b, "B-1", 5, HARI, HARI.plusDays(5));
                db.simpanTransaksi(b, masuk);
            }
            // Rusak tanggal_masuk di tabel batch
            try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + file);
                    Statement s = c.createStatement()) {
                s.executeUpdate("UPDATE batch SET tanggal_masuk = 'bukan-tanggal'");
            }
            try (PenyimpananSQLite db = new PenyimpananSQLite(file.toString())) {
                // Kesalahan pemrograman (argumen tidak valid) tetap IllegalArgumentException
                lempar(IllegalArgumentException.class, () -> db.muatBarang(0));
                lempar(IllegalArgumentException.class, () -> db.muatRiwayat(null));

                try {
                    db.muatSemuaBarang();
                    throw new AssertionError("Harusnya gagal karena tanggal korup");
                } catch (PenyimpananException expected) {
                    benar(expected.getCause() instanceof DateTimeParseException,
                            "cause harus DateTimeParseException: " + expected.getCause());
                }
                try {
                    db.muatBarang(1);
                    throw new AssertionError("Harusnya gagal karena tanggal korup");
                } catch (PenyimpananException expected) {
                    benar(expected.getCause() instanceof DateTimeParseException,
                            "cause muatBarang harus DateTimeParseException");
                }
            }
            // Perbaiki batch, tetapi rusak tanggal di tabel transaksi
            try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + file);
                    Statement s = c.createStatement()) {
                s.executeUpdate("UPDATE batch SET tanggal_masuk = '2026-11-20'");
                s.executeUpdate("UPDATE transaksi SET tanggal = '2026-99-99'");
            }
            try (PenyimpananSQLite db = new PenyimpananSQLite(file.toString())) {
                Barang b = db.muatBarang(1);
                try {
                    db.muatRiwayat(b);
                    throw new AssertionError("Harusnya gagal karena tanggal transaksi korup");
                } catch (PenyimpananException expected) {
                    benar(expected.getCause() instanceof DateTimeParseException,
                            "cause muatRiwayat harus DateTimeParseException");
                }
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private static void pembersihanKoneksiKonstruktorGagal() throws Exception {
        Path file = Files.createTempFile("kf-leak-", ".db");
        try {
            // Buat VIEW bernama 'barang' agar siapkanSkema() gagal saat INSERT INTO barang
            try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + file);
                    Statement s = c.createStatement()) {
                s.executeUpdate("CREATE VIEW barang AS SELECT 1 AS id");
            }
            lempar(PenyimpananException.class,
                    () -> new PenyimpananSQLite(file.toString()));
            // Di Windows, Files.delete akan gagal jika koneksi JDBC bocor (belum ditutup)
            Files.delete(file);
            benar(!Files.exists(file), "file DB berhasil dihapus karena koneksi sudah ditutup");
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private static void rollbackTransaksiSqlite() throws Exception {
        Path file = Files.createTempFile("kf-rollback-", ".db");
        try {
            try (PenyimpananSQLite db = new PenyimpananSQLite(file.toString())) {
                Barang b = db.muatBarang(1);
                PenerimaanStok masuk = new PenerimaanStok(HARI);
                masuk.terima(b, "B-1", 10, HARI, HARI.plusDays(5));
                db.simpanTransaksi(b, masuk);
            }
            // Pasang trigger yang menggagalkan INSERT rincian_transaksi berikutnya
            try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + file);
                    Statement s = c.createStatement()) {
                s.executeUpdate("CREATE TRIGGER gagal_rincian BEFORE INSERT ON rincian_transaksi "
                        + "BEGIN SELECT RAISE(ABORT, 'simulasi gagal rincian'); END;");
            }
            try (PenyimpananSQLite db = new PenyimpananSQLite(file.toString())) {
                Barang b = db.muatBarang(1);
                BatchStok batch = b.getDaftarBatch().get(0);
                PengeluaranStok keluar = new PengeluaranStok(HARI);
                keluar.keluarkan(b, batch, 4);
                lempar(PenyimpananException.class, () -> db.simpanTransaksi(b, keluar));

                // Pastikan rollback berhasil: sisa batch di DB tetap 10 dan riwayat tetap 1
                Barang setelahRollback = db.muatBarang(1);
                sama(10, setelahRollback.hitungSaldo(), "saldo DB tetap 10 setelah rollback");
                sama(1, db.muatRiwayat(setelahRollback).size(), "riwayat DB tetap 1 setelah rollback");
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private static void aksesKonkurenSqlite() throws Exception {
        try (PenyimpananSQLite db = new PenyimpananSQLite(":memory:")) {
            Barang b = db.muatBarang(1);
            PenerimaanStok awal = new PenerimaanStok(HARI.minusDays(5));
            awal.terima(b, "B-AWAL", 100, HARI.minusDays(5), HARI.plusDays(30));
            db.simpanTransaksi(b, awal);

            int jumlahThread = 4;
            CountDownLatch mulai = new CountDownLatch(1);
            CountDownLatch selesai = new CountDownLatch(jumlahThread);
            AtomicReference<Throwable> galat = new AtomicReference<>();

            for (int t = 0; t < jumlahThread; t++) {
                final int idx = t;
                new Thread(() -> {
                    try {
                        mulai.await();
                        for (int i = 0; i < 5; i++) {
                            Barang baru = db.simpanBarang(
                                    new Barang("Barang-" + idx + "-" + i, "pcs"));
                            db.simpanStokMinimum(baru, i);
                            List<Barang> semua = db.muatSemuaBarang();
                            benar(!semua.isEmpty(), "muatSemuaBarang tidak kosong");
                            List<Transaksi> r = db.muatRiwayat(semua.get(0));
                            sama(1, r.size(), "riwayat barang 1 konsisten");
                        }
                    } catch (Throwable throwable) {
                        galat.compareAndSet(null, throwable);
                    } finally {
                        selesai.countDown();
                    }
                }).start();
            }
            mulai.countDown();
            selesai.await();
            if (galat.get() != null) {
                throw new AssertionError("Kegagalan akses konkuren: " + galat.get(), galat.get());
            }
            sama(1 + jumlahThread * 5, db.muatSemuaBarang().size(),
                    "seluruh barang dari semua thread tersimpan utuh");
        }
    }

    private static void pemulihanStateMenuKonsol() throws Exception {
        Path file = Files.createTempFile("kf-konsol-", ".db");
        try {
            try (PenyimpananSQLite db = new PenyimpananSQLite(file.toString())) {
                Barang b = db.muatBarang(1);
                PenerimaanStok masuk = new PenerimaanStok(HARI);
                masuk.terima(b, "SUSU-1", 10, HARI, HARI.plusDays(10));
                db.simpanTransaksi(b, masuk);
            }
            try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + file);
                    Statement s = c.createStatement()) {
                s.executeUpdate("CREATE TRIGGER tolak_tx BEFORE INSERT ON transaksi "
                        + "BEGIN SELECT RAISE(ABORT, 'simulasi gagal tx'); END;");
            }
            try (PenyimpananSQLite db = new PenyimpananSQLite(file.toString())) {
                // Menu 5 (FEFO 4 kotak pada HARI) -> gagal simpan di DB -> Menu 6 (cek saldo) -> Menu 10 (keluar)
                String input = "5\n4\n" + HARI + "\n6\n10\n";
                PrintStream asli = System.out;
                ByteArrayOutputStream tangkap = new ByteArrayOutputStream();
                try {
                    System.setOut(new PrintStream(tangkap, true, StandardCharsets.UTF_8));
                    new MenuKonsol(new Scanner(input), db).jalankan();
                } finally {
                    System.setOut(asli);
                }
                String keluaran = tangkap.toString(StandardCharsets.UTF_8);
                benar(keluaran.contains("Penyimpanan gagal"), "pesan gagal tampil");
                benar(keluaran.contains("Stok fisik: 10 kotak"),
                        "saldo in-memory pulih ke 10 kotak setelah gagal simpan");
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private static void guiLazyRefreshGuardDanShutdown() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("       (Dilewati: lingkungan headless)");
            return;
        }
        AtomicReference<Throwable> galat = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            PenyimpananTerhitung db = new PenyimpananTerhitung();
            JendelaKantinFlow jendela = null;
            try {
                jendela = new JendelaKantinFlow(db);
                // 1. Saat startup (halaman awal "ringkasan"):
                // muatSemuaBarang dipanggil 1x, muatRiwayat = 0x, muatBarang(id) = 0x.
                sama(1, db.hitungMuatSemua, "startup hanya 1x muatSemuaBarang");
                sama(0, db.hitungMuatBarangId, "startup 0x muatBarang(id)");
                sama(0, db.hitungMuatRiwayat, "startup 0x muatRiwayat (riwayat lazy)");

                // 2. Tambah barang & simpan transaksi ketika halaman "riwayat" belum dibuka:
                benar(jendela.tambahBarang("Teh Botol", "botol"), "tambahBarang berhasil");
                sama(0, db.hitungMuatRiwayat, "tambahBarang tidak memicu muatRiwayat");

                Barang susu = jendela.getDaftarBarang().get(0);
                PenerimaanStok masuk = new PenerimaanStok(HARI.minusDays(5));
                masuk.terima(susu, "SUSU-BASI", 4, HARI.minusDays(5), HARI.minusDays(1));
                benar(jendela.simpanTransaksi(susu, masuk, "masuk 1"), "simpanTransaksi 1");
                sama(0, db.hitungMuatRiwayat, "simpanTransaksi tidak memicu PanelRiwayat tersembunyi");
                sama(0, db.hitungMuatBarangId, "simpanTransaksi 0x muatBarang(id)");

                // 3. Perubahan tanggal pemantauan tidak memicu query DB sama sekali:
                int semuaSebelumTanggal = db.hitungMuatSemua;
                jendela.setTanggalPemantauan(HARI);
                sama(semuaSebelumTanggal, db.hitungMuatSemua,
                        "setTanggalPemantauan 0 query DB");
                sama(0, db.hitungMuatRiwayat,
                        "setTanggalPemantauan tidak memicu muatRiwayat");

                // 4. Navigasi programatik ke "pemantauan" dan bukaPembuanganDariPemantauan:
                jendela.bukaPemantauanStatus(StatusKedaluwarsa.KEDALUWARSA);
                benar(jendela.bukaPembuanganDariPemantauan(
                        susu.getId(), "SUSU-BASI", HARI),
                        "bukaPembuanganDariPemantauan berhasil dengan data segar");

                // 5. Navigasi ke "riwayat" menyegarkan PanelRiwayat tepat 1x (1 query per barang, 0 muatBarang(id)):
                jendela.bukaHalaman("riwayat");
                int jumlahBarang = jendela.getDaftarBarang().size();
                sama(jumlahBarang, db.hitungMuatRiwayat,
                        "bukaHalaman('riwayat') memanggil muatRiwayat 1x per barang");
                sama(0, db.hitungMuatBarangId,
                        "getRiwayat memakai daftarBarang tanpa muatBarang(id)");

                // Membuka halaman "riwayat" lagi saat tidak dirty tidak menambah query:
                jendela.bukaHalaman("riwayat");
                sama(jumlahBarang, db.hitungMuatRiwayat,
                        "navigasi ulang ke halaman bersih 0 query");

                // Fallback getRiwayat untuk Barang dengan ID valid yang belum ada di daftarBarang:
                Barang luar = db.simpanBarangLangsung(new Barang("Kopi", "cup"));
                List<Transaksi> rLuar = jendela.getRiwayat(luar);
                benar(rLuar.isEmpty(), "fallback getRiwayat barang luar berhasil");
                sama(1, db.hitungMuatBarangId,
                        "fallback memanggil muatBarang(id) tepat 1x");

                // 6. Duplicate submission guard menolak panggilan tulis re-entrant:
                final JendelaKantinFlow refJendela = jendela;
                db.aksiSaatSimpanMinimum = () -> {
                    benar(!refJendela.muatUlang(),
                            "panggilan muatUlang re-entrant harus ditolak");
                    benar(!refJendela.tambahBarang("Duplikat", "pcs"),
                            "panggilan tambahBarang re-entrant harus ditolak");
                };
                benar(jendela.simpanStokMinimum(
                        jendela.getDaftarBarang().get(0), 3),
                        "simpanStokMinimum utama tetap berhasil (muatUlangInternal tidak terblokir)");
                db.aksiSaatSimpanMinimum = null;

                // 7. Clean shutdown menutup DB tepat 1x dan menolak operasi setelah ditutup:
                jendela.tutup();
                jendela.tutup();
                sama(1, db.hitungClose, "close() dipanggil tepat 1x");
                benar(!jendela.muatUlang(), "muatUlang ditolak setelah tutup");
                benar(!jendela.tambahBarang("Baru", "pcs"),
                        "tambahBarang ditolak setelah tutup");
                benar(jendela.getRiwayat(susu).isEmpty(),
                        "getRiwayat kosong setelah tutup");
            } catch (Throwable t) {
                galat.set(t);
            } finally {
                if (jendela != null) {
                    jendela.tutup();
                }
            }
        });
        if (galat.get() != null) {
            throw new AssertionError("Kegagalan uji GUI Phase 2B-1: "
                    + galat.get(), galat.get());
        }
    }

    private static final class PenyimpananTerhitung extends PenyimpananSQLite {
        int hitungMuatSemua;
        int hitungMuatBarangId;
        int hitungMuatRiwayat;
        int hitungClose;
        Runnable aksiSaatSimpanMinimum;

        PenyimpananTerhitung() {
            super(":memory:");
        }

        Barang simpanBarangLangsung(Barang barang) {
            return super.simpanBarang(barang);
        }

        @Override
        public synchronized List<Barang> muatSemuaBarang() {
            hitungMuatSemua++;
            return super.muatSemuaBarang();
        }

        @Override
        public synchronized Barang muatBarang(int id) {
            hitungMuatBarangId++;
            return super.muatBarang(id);
        }

        @Override
        public synchronized List<Transaksi> muatRiwayat(Barang barang) {
            hitungMuatRiwayat++;
            return super.muatRiwayat(barang);
        }

        @Override
        public synchronized void simpanStokMinimum(Barang barang, int stokMinimum) {
            if (aksiSaatSimpanMinimum != null) {
                aksiSaatSimpanMinimum.run();
            }
            super.simpanStokMinimum(barang, stokMinimum);
        }

        @Override
        public synchronized void close() {
            hitungClose++;
            super.close();
        }
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
