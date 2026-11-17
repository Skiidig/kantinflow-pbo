# Ringkasan Implementasi KantinFlow PBO

Dokumen ini merangkum kode terbaru setelah pemeriksaan akhir. Fokusnya adalah
struktur kelas, relasi data, penerapan PBO, penanganan exception, skema SQLite,
hasil pengujian, dan batas aplikasi.

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
| `JendelaKantinFlow` | `barang`, `daftarBarang`, `riwayat`, model tabel, field form, `penyimpanan` | `tampilkan()`, aksi form, `muatUlang()` | Antarmuka Swing dan barang aktif. |
| `Main` | — | `main()` | Memilih mode konsol, demo, atau GUI. |

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
- Driver SQLite dapat menimbulkan peringatan native access pada JDK 25 jika
  dijalankan tanpa `--enable-native-access=ALL-UNNAMED`; `run-gui.cmd` tidak
  memaksa flag tersebut agar tetap kompatibel dengan JDK lain.
