# Ringkasan Implementasi KantinFlow PBO

Dokumen ini merangkum kode terbaru setelah pemeriksaan akhir. Fokusnya adalah
struktur kelas, relasi data, penerapan PBO, penanganan exception, skema SQLite,
hasil pengujian, dan batas aplikasi. GUI utama adalah Swing + FlatLaf dengan
tujuh halaman; ikon SVG memakai FlatLaf Extras/JSVG, PDF memakai Apache PDFBox 3.0.8 dan font DejaVu Sans.

## 1. Kelas dan tanggung jawab

| Kelas | Atribut utama | Metode utama | Tanggung jawab |
|---|---|---|---|
| `Barang` | `id`, `nama`, `satuan`, `daftarBatch`, `stokMinimum` | `hitungSaldo()`, `hitungStokLayak()`, `tambahBatch()`, `setStokMinimum()` | Identitas barang, kumpulan batch, saldo fisik, stok layak, minimum. |
| `BatchStok` | `kode`, `tanggalMasuk`, `tanggalKedaluwarsa`, `sisaJumlah` | `layakPada()`, `statusPada()`, `kurangiSisa()` | Satu batch dan aturan kelayakan FEFO. |
| `Transaksi` | `tanggal` | abstrak `getJenis()`, `getBatch()`, `getJumlah()`, `getRincian()`; `getAlasan()` | Kontrak umum transaksi. |
| `PenerimaanStok` | `detail` | `terima()`, `dariPenyimpanan()` | Menerima batch dan menyimpan jumlah historis. |
| `PengeluaranStok` | `rincian`, `otomatis` | `keluarkan()`, `keluarkanFefo()`, `dariPenyimpanan()` | Pengeluaran manual atau FEFO. |
| `PembuanganStok` | `batch`, `jumlah`, `alasan` | `buang()`, `dariPenyimpanan()` | Pembuangan batch kedaluwarsa. |
| `DetailTransaksi` | `batch`, `jumlahDiterima` | `getBatch()`, `getJumlahDiterima()` | Detail historis penerimaan. |
| `RincianTransaksi` | `batch`, `jumlah` | `getBatch()`, `getJumlah()` | Satu baris alokasi transaksi. |
| `FefoAllocator` | tidak menyimpan state | `susun()` | Menyusun alokasi FEFO tanpa mengubah stok. |
| `StatusKedaluwarsa` | `label` | `getLabel()` | Enum `KEDALUWARSA`, `SEGERA_KEDALUWARSA`, `AMAN`. |
| `PenyimpananSQLite` | `koneksi` | `muatSemuaBarang()`, `muatBarang(id)`, `simpanBarang()`, `muatRiwayat()`, `simpanTransaksi()`, `simpanStokMinimum()` | Semua akses JDBC dan transaksi SQLite. |
| `MenuKonsol` | `barang`, `daftarBarang`, `riwayat`, `scanner`, `penyimpanan` | `jalankan()`, aksi menu | Antarmuka konsol dan barang aktif. |
| `JendelaKantinFlow` | `barang`, `daftarBarang`, `riwayat`, `cardLayout`, `panel`, `penyimpanan` | `tampilkan()`, `gantiBarang()`, `simpanTransaksi()`, `muatUlang()` | Shell Swing: sidebar dengan pemilih barang, konfirmasi form, CardLayout, status, dan koordinasi panel. |
| `KonteksKantin` | kontrak `Barang`, `Transaksi`, tanggal pemantauan | getter, simpan, tambah, ganti, pesan | Batas antara panel halaman dan shell. |
| `PanelKantin` | `konteks` | `perbarui()` | Dasar halaman dengan header konsisten. |
| `PanelRingkasan` | label stok/status, field tanggal | `perbarui()` | Ringkasan fisik, layak, minimum, status restok. |
| `PanelBarang` | tabel barang, form tambah, field minimum | `perbarui()` | Daftar barang, tambah barang, minimum. |
| `PanelStokMasuk` | field penerimaan, tabel batch | `perbarui()` | Penerimaan dan daftar batch. |
| `PanelStokKeluar` | combo batch, field manual/FEFO | `perbarui()` | Pengeluaran manual dan FEFO. |
| `PanelPemantauan` | tanggal, tabel status | `perbarui()` | Pemantauan kedaluwarsa. |
| `PanelPembuangan` | combo batch, jumlah, tanggal, alasan | `perbarui()` | Pembuangan batch. |
| `PanelRiwayat` | tabel rincian, filter, detail, ekspor | `perbarui()` | Riwayat dengan pencarian/filter, jumlah transaksi/rincian, dialog detail, konteks barang, dan ekspor CSV. |
| `RiwayatCsvExporter` | aturan CSV | `tulis()` | UTF-8 BOM, CRLF, escaping, proteksi formula, dan penulisan atomik. |
| `RiwayatPdfExporter` | aturan PDF | `tulis()` | A4 landscape, PDFBox, wrapping, header berulang, nomor halaman, dan penulisan atomik. |
| `Ui` | token warna/font/ukuran, kartu, tombol, tabel, ikon | helper tampilan | Konsistensi visual FlatLaf, layout responsif, dan pemuatan SVG dari classpath. |
| `Main` | — | `main()` | Memilih mode konsol, demo, atau GUI Swing; memasang `FlatLightLaf`. |

## 2. Relasi aktual

```text
Barang 1 ──── * BatchStok
Barang 1 ──── * Transaksi
Transaksi 1 ── * RincianTransaksi
RincianTransaksi * ── 1 BatchStok
PenerimaanStok 1 ── 1 DetailTransaksi ── 1 BatchStok
```

- `Barang.id` menjadi kunci pemilik batch dan transaksi.
- `BatchStok` tidak menyimpan `barang_id` di domain; kepemilikan dijaga oleh
  `Barang.daftarBatch` dan diverifikasi ulang oleh penyimpanan.
- Satu transaksi FEFO memiliki beberapa `RincianTransaksi`; transaksi manual
  dan pembuangan memiliki satu rincian.
- Barang aktif adalah state antarmuka. Konsol dan Swing selalu meneruskan
  barang aktif ke repository saat menyimpan dan memuat riwayat.

## 3. Contoh penerapan PBO

### Enkapsulasi

- `BatchStok.sisaJumlah` dan `Barang.daftarBatch` bersifat private.
- Perubahan stok hanya lewat `kurangiSisa()`; daftar batch keluar sebagai
  `Collections.unmodifiableList()`.
- `Barang.setStokMinimum()` dan konstruktor `BatchStok` memvalidasi nilai.
- `PenyimpananSQLite.koneksi` tidak diekspos ke MenuKonsol atau Swing.

### Abstraksi

- `Transaksi` mendefinisikan kontrak `getJenis()`, `getBatch()`, `getJumlah()`,
  dan `getRincian()` tanpa menentukan cara penyimpanan.
- UI tidak menulis SQL; semua JDBC berada di `PenyimpananSQLite`.
- `FefoAllocator` menyembunyikan algoritma pemilihan batch dari UI.

### Pewarisan

```text
Transaksi
├── PenerimaanStok
├── PengeluaranStok
└── PembuanganStok
```

Ketiga subclass mewarisi `getTanggal()` dan mengimplementasikan kontrak
transaksi. `PembuanganStok` menambahkan override `getAlasan()`.

### Polimorfisme

Riwayat disimpan sebagai `List<Transaksi>`. UI dan konsol memanggil
`getJenis()`, `getRincian()`, dan `getAlasan()` tanpa mengetahui subclass.
Repository juga membentuk kembali subclass yang tepat di `buatTransaksi()`.

## 4. Custom exception dan penanganannya

| Exception | Lokasi throw utama | Lokasi catch utama |
|---|---|---|
| `StokTidakCukupException` | `PengeluaranStok.keluarkan()`, `FefoAllocator.susun()`, `PembuanganStok.buang()` | `MenuKonsol` (pengeluaran/FEFO/pembuangan), `JendelaKantinFlow` (form terkait). |
| `PenyimpananException` | `PenyimpananSQLite`: buka, muat, simpan, migrasi, tutup; validasi kepemilikan/tanggal di `simpanTransaksi()` | `MenuKonsol.simpan()/muatUlang()`, GUI pada blok simpan/muat, `Main` saat membuka database. |
| `InputSelesaiException` (private) | `MenuKonsol.baca()` saat input habis | `MenuKonsol.jalankan()` untuk menutup sesi konsol dengan rapi. |

Validasi isian menggunakan `IllegalArgumentException` (misalnya kode batch
duplikat, tanggal sebelum batch masuk, alasan kosong, minimum negatif).
Pelanggaran urutan pemakaian objek menggunakan `IllegalStateException`.
`DateTimeParseException` ditangani di konsol dan Swing dengan pesan format
`yyyy-MM-dd`.

## 5. Skema SQLite terbaru

```sql
CREATE TABLE barang (
    id INTEGER PRIMARY KEY,
    nama TEXT NOT NULL,
    satuan TEXT NOT NULL,
    stok_minimum INTEGER NOT NULL DEFAULT 0
        CHECK (stok_minimum >= 0)
);

CREATE TABLE batch (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    barang_id INTEGER NOT NULL,
    kode TEXT NOT NULL,
    tanggal_masuk TEXT NOT NULL,
    tanggal_kedaluwarsa TEXT NOT NULL,
    sisa_jumlah INTEGER NOT NULL CHECK (sisa_jumlah >= 0),
    UNIQUE (barang_id, kode),
    FOREIGN KEY (barang_id) REFERENCES barang(id)
);

CREATE TABLE transaksi (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    barang_id INTEGER NOT NULL,
    jenis TEXT NOT NULL,
    tanggal TEXT NOT NULL,
    alasan TEXT,
    FOREIGN KEY (barang_id) REFERENCES barang(id)
);

CREATE TABLE rincian_transaksi (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    transaksi_id INTEGER NOT NULL,
    batch_id INTEGER NOT NULL,
    jumlah INTEGER NOT NULL CHECK (jumlah > 0),
    UNIQUE (transaksi_id, batch_id),
    FOREIGN KEY (transaksi_id) REFERENCES transaksi(id) ON DELETE CASCADE,
    FOREIGN KEY (batch_id) REFERENCES batch(id)
);
```

Catatan migrasi:

- Database baru langsung memakai `stok_minimum`.
- Database lama diperiksa dengan `PRAGMA table_info(barang)`.
- Jika kolom belum ada, `ALTER TABLE ... ADD COLUMN stok_minimum INTEGER NOT
  NULL DEFAULT 0 CHECK (stok_minimum >= 0)` dijalankan dalam transaksi.
- Barang lama mendapat nilai awal 0; batch, transaksi, dan rincian tidak
  disalin atau diubah.
- `UNIQUE (barang_id, kode)` menjaga kode batch unik per barang; domain juga
  memvalidasi duplikat agar pesannya jelas sebelum penyimpanan.

## 6. Hasil pengujian aktual

### Regression test terotomatisasi

Perintah:

```powershell
javac -cp "out;lib/*" -d tests-out tests/RegresiKantinFlow.java
java --enable-native-access=ALL-UNNAMED -cp "tests-out;out;lib/*" RegresiKantinFlow
```

Hasil:

```text
LULUS duplikat: Kode batch sudah digunakan: dup
LULUS tanggal manual: Tanggal pengeluaran tidak boleh sebelum tanggal masuk batch B1
LULUS tanggal FEFO: Tanggal pengeluaran tidak boleh sebelum tanggal masuk batch B1
LULUS tanggal pembuangan: Tanggal pembuangan tidak boleh sebelum tanggal masuk batch B1
LULUS simpan: saldo=4, historis penerimaan=10, rincian FEFO=2
LULUS reload: min=5, saldo=4, transaksi=4, rincianFEFO=2
LULUS rollback: sisa=5, riwayat=1
LULUS isolasi: Susu=3, Roti=2, batch/riwayat terpisah
SEMUA_REGRESI_LULUS
```

### Pemeriksaan GUI dan konsol

| Pemeriksaan | Hasil aktual |
|---|---|
| Duplikat batch di Swing | `Penerimaan gagal: Kode batch sudah digunakan: B1`; batch tetap 5, riwayat 1. |
| Duplikat batch di konsol | Pesan sama; stok tetap 5, riwayat 1. |
| FEFO sebelum tanggal masuk di Swing | `Pengeluaran FEFO gagal: Tanggal pengeluaran tidak boleh sebelum tanggal masuk batch B1`; stok/riwayat tidak berubah. |
| FEFO sebelum tanggal masuk di konsol | Pesan sama; stok/riwayat tidak berubah. |
| Panduan demo setelah perbaikan | FEFO 8 menghasilkan A=0/B=4; FEFO 5 ditolak; minimum 5 menghasilkan `Perlu restok`; reopen mempertahankan saldo 4, minimum 5, dan 4 baris riwayat. |
| GUI FlatLaf baru | Tambah Roti/minimum 5; terima ROTI-A 5 dan ROTI-B 7; manual 2; manual 4 ditolak; FEFO 8 menghasilkan A=0/B=2; batch ROTI-EXP; pemantauan 2 batch; buang 1; 7 rincian riwayat; reopen saldo 3/minimum 5/7 rincian. |
| Pencarian/filter Riwayat | Pencarian `rusak`/`ROTI-exp` tidak membedakan huruf besar-kecil; filter jenis, tanggal inklusif, gabungan, rentang tidak valid, empty state, reset, dan pembersihan saat pindah barang lulus. |
| Detail FEFO | FEFO A=5/B=7, keluar 8: satu transaksi, dua rincian A=5/B=3; memilih baris A atau B membuka dialog dua rincian; setelah sorting dan filter batch tetap lengkap. |
| Pemilih sidebar | Satu dropdown barang aktif berada di sidebar; nama panjang memakai tooltip dan lebar 207 px; konteks lima halaman diperbarui saat barang berubah. |
| Konfirmasi barang | Form Stok Masuk berisi kode; konfirmasi batal mempertahankan Roti dan isian; konfirmasi setuju memindahkan ke Susu dan membersihkan form. Reload setelah simpan tidak memunculkan konfirmasi. |
| Tanpa barang | Database tanpa barang membuka halaman Barang dengan dropdown nonaktif dan pesan sidebar; barang pertama dapat ditambahkan dan langsung menjadi aktif. |
| Reopen | Barang aktif Susu tetap aktif setelah jendela ditutup dan dibuka kembali melalui preferensi UI sidecar. |
| Ekspor CSV | Ekspor tanpa filter 6 rincian, filter FEFO 2, filter batch 1, urutan mengikuti sorting; text koma/kutip/baris baru, formula, BOM, CRLF, dan jumlah benar. |
| Kegagalan ekspor | Target read-only gagal ditulis, file lama tetap `FILE-LAMA`, dan file sementara dibersihkan. Batal pilih/timpa tidak membuat file atau pesan gagal. |
| Ekspor PDF | A4 842×595, teks/filter/jumlah benar, DejaVu Sans, 4 halaman uji dengan header berulang dan nomor halaman, baris sangat panjang terpecah tanpa loop. |
| Ikon SVG | Tujuh menu memuat `FlatSVGIcon` 20×20 dari classpath; ikon aktif berwarna biru dan label teks tetap ada. |
| Ukuran jendela | Default 1180×720, minimum 960×640; ketujuh halaman dapat dinavigasi pada 1366×768 dan 960×640, dengan scroll dan form adaptif. |
| Demo lama | Saldo 5, 5, 3, 3 dan riwayat 1, 1, 2, 2. |

### Pemeriksaan penyimpanan

- Penerimaan 10, pengeluaran manual 3, dan FEFO 10 menghasilkan saldo 4.
- Jumlah historis penerimaan tetap 10 meskipun batch sudah berkurang.
- Rincian FEFO tetap dua baris setelah reopen.
- Stok minimum 5 tetap tersimpan setelah reopen.
- Trigger `BEFORE INSERT ON transaksi` membuat penyimpanan gagal; rollback
  mengembalikan sisa batch 5 dan riwayat 1.
- Isolasi barang: Susu=3 dan Roti=2; riwayat dan batch tidak tercampur.

## 7. Temuan dan perbaikan

1. **Duplikat kode batch** sebelumnya lolos ke constraint SQLite sehingga pesan
   GUI/konsol hanya “Gagal menyimpan transaksi”. Sekarang `PenerimaanStok`
   menolak duplikat sebelum batch ditambahkan dengan pesan
   `Kode batch sudah digunakan: ...`.
2. **Tanggal pengeluaran/pembuangan sebelum tanggal masuk** sebelumnya tidak
   diperiksa secara eksplisit. Sekarang manual, FEFO, pembuangan, dan
   `PenyimpananSQLite.simpanTransaksi()` menolak transaksi tersebut sebelum
   mengubah stok.

Tidak ditemukan masalah lain pada pemeriksaan ini. Rollback, minimum, rincian
FEFO, jumlah historis, dan isolasi barang tetap lulus.

## 8. Batas aplikasi

- Aplikasi menyimpan data secara lokal di SQLite; tidak ada sinkronisasi
  antarkomputer, autentikasi, atau multi-pengguna bersamaan.
- Jumlah masih bilangan bulat; belum ada satuan pecahan.
- Satu transaksi hanya untuk satu barang aktif.
- Stok minimum hanya menghasilkan status informatif; belum ada pembelian
  otomatis atau notifikasi eksternal.
- Tanggal memakai format ISO `yyyy-MM-dd`; tidak ada pemilih tanggal khusus.
- Pengeluaran manual memakai tanggal sistem saat tombol ditekan.
- Belum ada CI otomatis; regression test domain/persistence dijalankan manual,
  sedangkan alur GUI diuji dengan harness Swing.
- Migrasi saat ini hanya penambahan kolom `stok_minimum`; perubahan skema
  berikutnya perlu strategi versi/migrasi tersendiri.
- GUI membutuhkan `flatlaf-3.8.jar`, `flatlaf-extras-3.8.jar`, dan
  `jsvg-2.2.0.jar` di `lib/`; setup menyiapkannya. Menjalankan `java` manual
  tanpa `lib/*` akan gagal memuat tema/ikon.
- Filter, detail, ekspor CSV, dan ekspor PDF Riwayat tersedia; belum ada login atau grafik.
- Barang aktif diingat sebagai preferensi UI sidecar `<database>.ui`; ini
  bukan tabel/skema domain dan tidak mengubah data stok pengguna.
- Harness GUI ada di `tests/SwingRiwayatHarness.java`,
  `tests/SwingActiveBarangHarness.java`, `tests/SwingEmptyBarangHarness.java`,
  `tests/CsvExportHarness.java`, `tests/SwingExportHarness.java`,
  `tests/PdfExportHarness.java`, dan `tests/SwingPdfExportHarness.java`;
  semuanya menggunakan database/folder uji terpisah.
- Ekspor PDF memakai Apache PDFBox 3.0.8 (Apache-2.0) dan font DejaVu Sans
  (lisensi di `src/main/resources/kantinflow/fonts/LICENSE-DejaVu.txt`).
- Peringatan native access SQLite dapat muncul pada JDK 25 jika dijalankan
  tanpa `--enable-native-access=ALL-UNNAMED`; `run-gui.cmd` tidak memaksa flag
  tersebut agar tetap kompatibel dengan JDK lain.
