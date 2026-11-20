# KantinFlow

KantinFlow adalah aplikasi desktop Java (Swing + FlatLaf) untuk manajemen
persediaan kantin berbasis **batch** dan **FEFO** (*First Expired, First Out*).
Setiap barang menyimpan beberapa batch dengan tanggal masuk, tanggal
kedaluwarsa, dan sisa jumlah; pengeluaran otomatis memakai batch yang paling
dekat kedaluwarsa lebih dahulu.

Data disimpan di SQLite lokal. Aplikasi menyediakan GUI tujuh halaman, menu
konsol interaktif, dan mode demo empat skenario yang berjalan di memori tanpa
mengakses database.

## Fitur Utama

- **Ringkasan** — dashboard dengan filter barang dan tanggal acuan. Untuk
  barang tertentu menampilkan **Stok fisik**, **Stok layak** (batch belum
  kedaluwarsa), **Stok minimum**, dan status; untuk semua barang menampilkan
  jumlah barang, barang perlu restok, batch segera kedaluwarsa, dan batch
  kedaluwarsa. Tersedia tabel peringatan kedaluwarsa, tabel seluruh barang,
  serta kartu pintasan ke halaman lain.
- **Master Barang** — pencarian nama, tabel stok fisik/minimum, dialog tambah
  barang (satuan preset atau khusus), dan dialog atur stok minimum. Nama dan
  satuan barang yang sudah tersimpan belum dapat diubah dari UI.
- **Stok Masuk** — penerimaan batch baru dengan kode batch otomatis berformat
  `AWALAN-yyyyMMdd-NN`, validasi tanggal, dan tabel batch barang terpilih
  berstatus badge.
- **Stok Keluar** — dua mode: **Manual** (pilih batch + jumlah) dan
  **Otomatis FEFO** (jumlah + tanggal pengeluaran, dengan pratinjau alokasi per
  batch sebelum disimpan). Batch kedaluwarsa tidak dapat dikeluarkan manual.
  Kartu **Batch Tersedia** menampilkan batch bersisa diurutkan FEFO.
- **Pemantauan** — filter barang, status kedaluwarsa, dan tanggal pemeriksaan;
  tabel batch bersisa beserta statusnya.
- **Pembuangan** — membuang stok kedaluwarsa atau rusak. Batch kedaluwarsa
  boleh tanpa alasan (otomatis dicatat `Kedaluwarsa`); batch belum kedaluwarsa
  wajib diberi alasan. Aksi memakai dialog konfirmasi dan tercatat di riwayat.
- **Riwayat** — filter barang, pencarian kode batch/alasan, jenis transaksi, dan
  rentang tanggal; detail per transaksi. Ekspor **CSV** (UTF-8 BOM, escaping
  CSV, penulisan atomik) dan **PDF** (A4 landscape, header tabel berulang,
  nomor halaman, font DejaVu Sans) mengikuti baris yang sedang ditampilkan.
- **UI** — tema terang minimalis Zinc/Slate berbasis FlatLaf, sidebar tujuh
  halaman, layout full-width mengikuti lebar jendela (ukuran awal 1280×800,
  minimum 960×640), kartu KPI, pill badge, empty state, toast notification,
  dan date picker.
- **Mode konsol** — 10 menu: pilih barang, tambah barang, terima stok, keluar
  manual, keluar FEFO, saldo/riwayat, pemantauan, pembuangan, stok minimum,
  keluar.

Aturan stok yang dipakai aplikasi:

- Status batch pada tanggal acuan: **Kedaluwarsa** jika tanggal kedaluwarsa
  sudah lewat; **Segera kedaluwarsa** jika tinggal ≤ 3 hari; selain itu
  **Aman**.
- **Stok layak** = total sisa batch bersisa yang belum kedaluwarsa.
- FEFO mengurutkan batch layak berdasarkan tanggal kedaluwarsa, lalu tanggal
  masuk, lalu kode batch.
- Barang dianggap **perlu restok** jika stok layak ≤ stok minimum.

## Teknologi & Prasyarat

- **JDK 25** (diuji dengan Temurin 25.0.5). `java` dan `javac` harus ada di
  `PATH`.
- Windows: PowerShell (dipakai `setup.ps1` dan `run-gui.cmd`).
- Library di `lib/` (diunduh oleh `setup.ps1`, tidak di-commit):

| JAR | Fungsi |
| --- | --- |
| `sqlite-jdbc-3.54.1.0.jar` | Driver SQLite untuk persistensi |
| `flatlaf-3.8.jar` | Look-and-feel Swing |
| `flatlaf-extras-3.8.jar` | Ikon SVG (`FlatSVGIcon`) |
| `jsvg-2.2.0.jar` | Parser SVG untuk FlatLaf Extras |
| `flatlaf-fonts-inter-3.19.jar` | Font antarmuka Inter |
| `LGoodDatePicker-11.2.1.jar` | Komponen pemilih tanggal |
| `swing-toast-notifications-1.0.4.jar` | Toast notification |
| `pdfbox-3.0.8.jar`, `pdfbox-io-3.0.8.jar`, `fontbox-3.0.8.jar` | Ekspor PDF |
| `commons-logging-1.4.0.jar` | Dependensi logging PDFBox |
| `xchart-3.8.8.jar` | Tersedia di `lib/`, belum dipakai kode saat ini |

## Struktur Proyek

```text
kantinflow-pbo/
├─ src/main/java/kantinflow/
│  ├─ Main.java, MenuKonsol.java          # entry point + menu konsol
│  ├─ Barang, BatchStok, Transaksi, DetailTransaksi, RincianTransaksi
│  ├─ PenerimaanStok, PengeluaranStok, PembuanganStok
│  ├─ FefoAllocator, KodeBatch, StatusKedaluwarsa, StokTidakCukupException
│  ├─ penyimpanan/
│  │  ├─ PenyimpananSQLite.java           # seluruh akses JDBC + skema
│  │  └─ PenyimpananException.java
│  └─ gui/
│     ├─ JendelaKantinFlow, KonteksKantin, PanelKantin, PanelBarangTerpilih
│     ├─ PanelRingkasan … PanelRiwayat    # tujuh halaman
│     ├─ Ui, Tema, CardPanel, StatCard, Badge, BadgeRenderer
│     ├─ EmptyState, DialogKonfirmasi, Notifikasi, ToastNotification
│     ├─ TabelKantin, PemilihTanggal, FormatTanggal
│     └─ RiwayatCsvExporter, RiwayatPdfExporter
├─ src/main/resources/kantinflow/
│  ├─ KantinFlow.properties               # custom defaults FlatLaf
│  ├─ icons/                              # ikon SVG
│  └─ fonts/                              # DejaVu Sans untuk PDF
├─ data/kantinflow.db                     # database lokal (ikut repo)
├─ lib/                                   # JAR dependensi (tidak di-commit)
├─ out/                                   # hasil kompilasi (tidak di-commit)
├─ setup.ps1                              # unduh/verifikasi dependensi
└─ run-gui.cmd                            # kompilasi + salin resource + GUI
```

## Cara Kompilasi & Menjalankan

Semua perintah dijalankan dari akar proyek.

### 1. Siapkan dependensi (sekali)

```powershell
powershell -ExecutionPolicy Bypass -File .\setup.ps1
```

`setup.ps1` memvalidasi JAR di `lib/` dan mengunduh yang belum ada. Untuk
memeriksa saja tanpa mengunduh, tambahkan `-CheckOnly`.

### 2. Kompilasi dan salin resource

```powershell
$src = Get-ChildItem -Path src\main\java -Recurse -Filter *.java -File
javac -encoding UTF-8 -cp "lib/*" -d out $src.FullName
Copy-Item -Path "src\main\resources\*" -Destination out -Recurse -Force
```

Resource (`KantinFlow.properties`, ikon SVG, font DejaVu) harus ikut ke `out/`
agar ikon, tema FlatLaf, dan font PDF tersedia. Mode konsol dan demo tidak
memerlukannya.

### 3. Jalankan

```powershell
# GUI
java --enable-native-access=ALL-UNNAMED -cp "out;lib/*" kantinflow.Main gui

# Menu konsol
java --enable-native-access=ALL-UNNAMED -cp "out;lib/*" kantinflow.Main

# Demo empat skenario (tanpa database)
java -cp "out;lib/*" kantinflow.Main demo
```

Alternatif sekali klik di Windows: `run-gui.cmd` (memeriksa dependensi,
mengompilasi, menyalin resource, lalu membuka GUI).

Catatan:

- `--enable-native-access=ALL-UNNAMED` mencegah peringatan akses native driver
  SQLite pada JDK 25. Jika JDK Anda tidak mengenali flag ini, hilangkan.
- Classpath memakai `;` di Windows. Di Linux/macOS ganti menjadi `:` dan
  sesuaikan pemisah path perintah kompilasi.
- GUI memerlukan lingkungan grafis; bila headless, aplikasi menyarankan mode
  konsol atau demo.

## Penyimpanan Data

- Lokasi default: `data/kantinflow.db`, relatif terhadap direktori kerja.
  Folder dan file dibuat otomatis saat pertama kali dibuka.
- Lokasi lain dapat dipakai lewat environment variable `KANTINFLOW_DB`:

  ```powershell
  $env:KANTINFLOW_DB = "data/kantinflow-uji.db"
  java --enable-native-access=ALL-UNNAMED -cp "out;lib/*" kantinflow.Main gui
  ```

- Skema SQLite (dibuat dengan `CREATE TABLE IF NOT EXISTS`, foreign key aktif):

  | Tabel | Kolom utama |
  | --- | --- |
  | `barang` | `id`, `nama`, `satuan`, `stok_minimum` (≥ 0) |
  | `batch` | `id`, `barang_id`, `kode`, `tanggal_masuk`, `tanggal_kedaluwarsa`, `sisa_jumlah` (≥ 0); unik `(barang_id, kode)` |
  | `transaksi` | `id`, `barang_id`, `jenis`, `tanggal`, `alasan` |
  | `rincian_transaksi` | `id`, `transaksi_id`, `batch_id`, `jumlah` (> 0); unik `(transaksi_id, batch_id)` |

- Database baru yang masih kosong otomatis diisi satu barang contoh
  (`Susu Kotak`, satuan `kotak`). Database lama yang belum punya kolom
  `stok_minimum` dimigrasikan otomatis.
- Operasi tulis memakai transaksi SQLite; penyimpanan barang dan transaksi
  memakai `commit`/`rollback` eksplisit.
- File `data/kantinflow.db` disertakan di repositori. Perubahan data bersifat
  lokal pada mesin masing-masing; Git tidak menggabungkan isi SQLite. Tutup
  aplikasi sebelum menyalin atau mencadangkan file database.
- `.gitignore` hanya mengecualikan `/lib/` dan `/out/`.
