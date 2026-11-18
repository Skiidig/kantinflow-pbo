# KantinFlow PBO

Proyek Java untuk mata kuliah Pemrograman Berorientasi Objek. Aplikasi konsol ini menyediakan penerimaan stok, pengeluaran manual, pengeluaran otomatis FEFO, pemantauan kedaluwarsa, pembuangan stok kedaluwarsa, saldo, riwayat transaksi, dan stok minimum untuk beberapa barang. Menu interaktif menyimpan data ke SQLite lokal; mode demo empat skenario tetap berjalan sepenuhnya di memori.

GUI utama menggunakan **Swing + FlatLaf** dengan tujuh halaman: Ringkasan, Barang, Stok Masuk, Stok Keluar, Pemantauan, Pembuangan, dan Riwayat. Aplikasi dapat dijalankan tanpa build tambahan.

## Tujuan dan berkas kritis

Tujuan proyek mengikuti deskripsi di README ini: menyediakan penerimaan stok,
pengeluaran manual, pengeluaran otomatis FEFO, pemantauan kedaluwarsa,
pembuangan stok kedaluwarsa, saldo, riwayat transaksi, dan stok minimum untuk
beberapa barang. Menu interaktif menyimpan data ke SQLite lokal; mode demo empat
skenario berjalan sepenuhnya di memori. GUI utama adalah Swing dengan tema
FlatLaf, dashboard modern, tema terang/gelap, dan tujuh halaman.

Berkas yang paling menentukan perilaku aplikasi:

- `src/main/java/kantinflow/Barang.java`, `src/main/java/kantinflow/BatchStok.java`, dan `src/main/java/kantinflow/FefoAllocator.java` — aturan barang, batch, status kedaluwarsa, dan urutan alokasi FEFO.
- `src/main/java/kantinflow/PenerimaanStok.java`, `src/main/java/kantinflow/PengeluaranStok.java`, dan `src/main/java/kantinflow/PembuanganStok.java` — transaksi domain.
- `src/main/java/kantinflow/penyimpanan/PenyimpananSQLite.java` — seluruh akses JDBC dan skema `barang`, `batch`, `transaksi`, `rincian_transaksi`.
- `src/main/java/kantinflow/Main.java` — entry point konsol, demo, dan GUI Swing; memasang tema FlatLaf terang/gelap.
- `src/main/java/kantinflow/gui/JendelaKantinFlow.java`, panel halaman di `src/main/java/kantinflow/gui/`, dan `Ui.java` — shell, sidebar, tema, notifikasi, animasi, serta komponen visual bersama.
- `src/main/resources/kantinflow/KantinFlow.properties` — custom defaults FlatLaf: aksen `#1D4ED8`, arc, scrollbar, tabel, dan baris.
- `src/main/java/kantinflow/gui/CardPanel.java`, `StatCard.java`, `Badge.java`, `EmptyState.java`, `Tema.java`, dan `Notifikasi.java` — komponen dashboard reusable.
- `run-gui.cmd` dan `setup.ps1` — peluncur sekali klik, penyiapan dependensi, dan penyalinan resource ikon.
- `tests/RegresiKantinFlow.java` — regression domain dan penyimpanan.
- `tests/SwingRiwayatHarness.java` — harness Swing untuk pencarian/filter/detail Riwayat dan pemeriksaan dua ukuran.
- `tests/SwingActiveBarangHarness.java` — harness pemilih sidebar, konfirmasi form, konteks barang, dan reopen.
- `tests/SwingEmptyBarangHarness.java` — harness kondisi tanpa barang dan penambahan barang pertama.
- `tests/SwingBarangHarness.java` — harness pencarian, pemilihan baris, dialog tambah/minimum, validasi, persistensi, dan layout Barang.
- `tests/CsvExportHarness.java` — pemeriksaan format CSV dan kegagalan penulisan.
- `tests/SwingExportHarness.java` — harness tombol ekspor CSV, snapshot filter/sort, overwrite, dan pembatalan.
- `tests/PdfExportHarness.java` — pemeriksaan PDF, header berulang, halaman banyak, dan render halaman.
- `tests/SwingPdfExportHarness.java` — harness tombol Ekspor PDF, chooser, overwrite, dan opsi Buka PDF.
- `tests/VisualThemeHarness.java` — screenshot dan pemeriksaan mode terang/gelap pada 1366×768 dan 1000×650.

## Persiapan lokal

Pastikan `java -version` dan `javac -version` berjalan. Proyek diuji dengan JDK 25.0.5 dan memakai SQLite JDBC 3.54.1.0, FlatLaf 3.8, FlatLaf Extras 3.8, JSVG 2.2.0, Apache PDFBox 3.0.8, FlatLaf Inter Fonts 3.19, XChart 3.8.8, serta Swing Toast Notifications 1.0.4. Font PDF DejaVu Sans tetap disertakan untuk ekspor PDF.

### Menjalankan GUI sekali klik di Windows

Persiapan awal untuk teman kelompok:

1. Clone repo ini.
2. Pasang JDK 25, lalu pastikan perintah `java` dan `javac` dapat dijalankan dari Command Prompt. Cek dengan `java -version` dan `javac -version`.
3. Buka PowerShell di akar repo, lalu siapkan dependensi sekali:
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\setup.ps1
   ```
4. Buka `run-gui.cmd` dengan klik dua kali. Skrip akan memeriksa JDK, seluruh dependensi, mengompilasi sumber Java ke `out`, menyalin resource ikon ke classpath output, lalu membuka GUI.

`run-gui.cmd` selalu bekerja dari folder tempat skrip berada, jadi tetap dapat dijalankan dari folder kerja lain atau lewat shortcut. Jika Java, `javac`, atau dependensi belum siap, skrip menampilkan petunjuk dan tidak melanjutkan kompilasi. Jika kompilasi atau peluncuran gagal, pesan kesalahan ditampilkan sebelum jendela ditutup.

`setup.ps1` menyiapkan SQLite JDBC `3.54.1.0`, FlatLaf `3.8`, FlatLaf Extras `3.8`, JSVG `2.2.0`, FlatLaf Inter Fonts `3.19`, XChart `3.8.8`, Swing Toast Notifications `1.0.4`, Apache PDFBox `3.0.8`, PDFBox IO `3.0.8`, FontBox `3.0.8`, dan Commons Logging `1.4.0`. Jika JAR di `lib/` sudah ada dan valid, file itu dipakai tanpa mengunduh ulang. Unduhan baru ditulis ke file sementara, diperiksa isinya, lalu dipindahkan ke nama akhir; file parsial atau rusak tidak dianggap sebagai dependensi siap pakai. Jika jalur HTTPS PowerShell gagal, skrip mencoba unduhan melalui Java sebelum melaporkan kegagalan.

### Memasang dependensi secara manual

Cara termudah adalah `setup.ps1` di atas. Jika ingin manual, unduh JAR berikut ke folder `lib`:

```powershell
New-Item -ItemType Directory -Force lib | Out-Null
Invoke-WebRequest `
  -Uri "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.54.1.0/sqlite-jdbc-3.54.1.0.jar" `
  -OutFile "lib/sqlite-jdbc-3.54.1.0.jar"
Invoke-WebRequest `
  -Uri "https://repo1.maven.org/maven2/com/formdev/flatlaf/3.8/flatlaf-3.8.jar" `
  -OutFile "lib/flatlaf-3.8.jar"
Invoke-WebRequest `
  -Uri "https://repo1.maven.org/maven2/com/formdev/flatlaf-extras/3.8/flatlaf-extras-3.8.jar" `
  -OutFile "lib/flatlaf-extras-3.8.jar"
Invoke-WebRequest `
  -Uri "https://repo1.maven.org/maven2/com/github/weisj/jsvg/2.2.0/jsvg-2.2.0.jar" `
  -OutFile "lib/jsvg-2.2.0.jar"
Invoke-WebRequest `
  -Uri "https://repo1.maven.org/maven2/com/formdev/flatlaf-fonts-inter/3.19/flatlaf-fonts-inter-3.19.jar" `
  -OutFile "lib/flatlaf-fonts-inter-3.19.jar"
Invoke-WebRequest `
  -Uri "https://repo1.maven.org/maven2/org/knowm/xchart/xchart/3.8.8/xchart-3.8.8.jar" `
  -OutFile "lib/xchart-3.8.8.jar"
Invoke-WebRequest `
  -Uri "https://raw.githubusercontent.com/DJ-Raven/swing-toast-notifications/main/library/swing-toast-notifications-1.0.4.jar" `
  -OutFile "lib/swing-toast-notifications-1.0.4.jar"
Invoke-WebRequest `
  -Uri "https://repo1.maven.org/maven2/org/apache/pdfbox/pdfbox/3.0.8/pdfbox-3.0.8.jar" `
  -OutFile "lib/pdfbox-3.0.8.jar"
Invoke-WebRequest `
  -Uri "https://repo1.maven.org/maven2/org/apache/pdfbox/pdfbox-io/3.0.8/pdfbox-io-3.0.8.jar" `
  -OutFile "lib/pdfbox-io-3.0.8.jar"
Invoke-WebRequest `
  -Uri "https://repo1.maven.org/maven2/org/apache/pdfbox/fontbox/3.0.8/fontbox-3.0.8.jar" `
  -OutFile "lib/fontbox-3.0.8.jar"
Invoke-WebRequest `
  -Uri "https://repo1.maven.org/maven2/commons-logging/commons-logging/1.4.0/commons-logging-1.4.0.jar" `
  -OutFile "lib/commons-logging-1.4.0.jar"
```

Alternatif: salin semua JAR di atas ke `lib/`. Seluruh kode Java berada di `src/main/java/kantinflow/`, dengan `kantinflow.Main` sebagai titik masuk. Ikon SVG berada di `src/main/resources/kantinflow/icons/` dan font PDF berada di `src/main/resources/kantinflow/fonts/`; `run-gui.cmd` menyalin resource ke `out/` agar terbaca dari classpath.

### Kompilasi

```powershell
$javaSources = Get-ChildItem -Path src/main/java -Recurse -Filter *.java -File
javac -cp "lib/*" -d out $javaSources.FullName
```

### Menjalankan menu dengan SQLite

```powershell
java --enable-native-access=ALL-UNNAMED -cp "out;lib/*" kantinflow.Main
```

### Menjalankan GUI Swing

Cara praktis di Windows adalah `run-gui.cmd` seperti di atas. Untuk menjalankan manual dari terminal:

```powershell
java --enable-native-access=ALL-UNNAMED -cp "out;lib/*" kantinflow.Main gui
```

GUI utama memakai database yang sama dengan menu konsol (default `data/kantinflow.db`). Sidebar berisi brand KantinFlow, pemilih **Barang aktif** tepat di bawah brand, navigasi tujuh halaman berikon, tombol mode gelap/terang, dan footer versi. Header hanya berisi judul serta deskripsi halaman. Halaman **Ringkasan** adalah dashboard: empat `StatCard` (stok fisik/layak/minimum/status), daftar peringatan batch kedaluwarsa, tabel semua barang dengan badge status, dan grafik batang stok per barang (XChart). Halaman **Barang** memuat pencarian nama, tabel **Nama/Satuan/Stok fisik/Stok minimum**, tombol **+ Tambah Barang**, serta area aksi untuk baris terpilih. Klik baris hanya memilih; **Jadikan Barang Aktif** memakai konfirmasi pergantian barang yang sudah ada, dan **Atur Minimum** membuka dialog terpisah untuk barang tersebut. Barang aktif ditandai pada baris dan label. Form tambah dan minimum berada di dialog modal, bukan kartu permanen. **Stok Masuk** memuat form penerimaan dengan satuan di dekat jumlah dan daftar batch berbadge status. **Stok Keluar** memisahkan mode Manual dan FEFO; sisa batch terpilih tampil dekat input jumlah. **Pemantauan** menampilkan tanggal, ringkasan stok, dan tabel status kedaluwarsa berbadge. **Pembuangan** menampilkan form dan ringkasan tindakan dengan konfirmasi modal modern. **Riwayat** menampilkan tanggal, jenis, batch, jumlah, dan alasan, serta tombol **Ekspor CSV** dan **Ekspor PDF** untuk menyimpan rincian yang sedang tampil. CSV memakai UTF-8 BOM; PDF memakai A4 landscape, header tabel berulang, nomor halaman, font DejaVu Sans yang tertanam, dan ringkasan filter. Keduanya mengikuti filter dan urutan sorting serta menulis ke file sementara sebelum dipindahkan.

Tema memakai custom defaults FlatLaf `KantinFlow.properties`: aksen `#1D4ED8`, latar terang `#F4F6FA`, arc 10–16, scrollbar tipis, tabel hanya garis horizontal, dan tinggi baris 40 px. Font antarmuka memakai **Inter** dari `flatlaf-fonts-inter`. Tombol mode gelap/terang memakai `FlatLightLaf`/`FlatDarkLaf`, membangun ulang panel dengan palet gelap, dan menyimpan pilihan di `data/kantinflow-tema.properties`. Toast sukses/gagal/peringatan menggantikan teks status footer; status aksesibilitas tetap tersedia secara tersembunyi untuk pengujian. Halaman memakai scroll, layout responsif, dan komponen `CardPanel`, `StatCard`, `Badge`, serta `EmptyState`. Jendela default 1180×720 dan minimum 960×640. Mengganti barang memuat ulang seluruh halaman dan membersihkan pilihan batch serta hasil pemantauan lama.

Bukti antarmuka dashboard: [Dashboard terang 1366](docs/bukti-gui/dashboard-1366-light.png), [Dashboard gelap 1366](docs/bukti-gui/dashboard-1366-dark.png), [Dashboard terang 1000](docs/bukti-gui/dashboard-1000-light.png), [Dashboard gelap 1000](docs/bukti-gui/dashboard-1000-dark.png), [Riwayat gelap](docs/bukti-gui/riwayat-1366-dark.png). Bukti halaman lain: [Barang](docs/bukti-gui/gui-flatlaf-barang.png), [Barang pada jendela kecil](docs/bukti-gui/gui-flatlaf-barang-kecil.png), [Dialog Tambah Barang](docs/bukti-gui/gui-flatlaf-dialog-tambah-barang.png), [Dialog Atur Minimum](docs/bukti-gui/gui-flatlaf-dialog-minimum.png), [Dialog Tambah pada jendela kecil](docs/bukti-gui/gui-flatlaf-dialog-tambah-kecil.png), [Dialog Minimum pada jendela kecil](docs/bukti-gui/gui-flatlaf-dialog-minimum-kecil.png), [Ringkasan dashboard](docs/bukti-gui/dashboard-1366-light.png), [Stok Masuk](docs/bukti-gui/gui-flatlaf-stok-masuk.png), [Riwayat](docs/bukti-gui/gui-flatlaf-riwayat.png), [Riwayat dengan filter](docs/bukti-gui/gui-flatlaf-riwayat-filter.png), [Detail FEFO](docs/bukti-gui/gui-flatlaf-riwayat-detail-fefo.png), [Ekspor CSV berhasil](docs/bukti-gui/gui-flatlaf-ekspor-csv.png), [Ekspor PDF berhasil](docs/bukti-gui/gui-flatlaf-ekspor-pdf.png), [Stok Keluar](docs/bukti-gui/gui-flatlaf-stok-keluar.png), dan [jendela kecil](docs/bukti-gui/gui-flatlaf-jendela-kecil.png). Contoh laporan PDF uji: [contoh-riwayat.pdf](docs/bukti-gui/contoh-riwayat.pdf).

### Database lokal setiap komputer

Database default berada di dalam folder repo:

```text
data/kantinflow.db
```

File dan foldernya dibuat otomatis saat menu atau GUI pertama kali dijalankan. Database ini **lokal untuk komputer tersebut**: hasil transaksi di satu komputer tidak otomatis muncul di komputer lain. Jika repo di-clone ulang atau dipindahkan, gunakan database yang sesuai atau salin file database secara sengaja.

Untuk pengujian, gunakan database terpisah dengan environment variable `KANTINFLOW_DB` sebelum membuka `run-gui.cmd`:

```powershell
$env:KANTINFLOW_DB = "data/kantinflow-uji.db"
.\run-gui.cmd
```

`run-gui.cmd` berjalan dari akar repo, sehingga path relatif mengikuti folder proyek. Gunakan path absolut jika database disimpan di luar repo. Variabel ini tidak diubah atau ditimpa oleh skrip.

Setiap barang memiliki batch dan riwayat sendiri. Mengganti barang aktif akan mengosongkan pilihan batch dan hasil pemantauan lama, lalu memuat data barang tersebut dari SQLite. Batch hanya boleh dipakai oleh barang pemiliknya, baik di domain maupun di lapisan penyimpanan.

Tanpa argumen aplikasi membuka menu konsol. Argumen `demo` tetap menjalankan empat skenario di memori.

Pada Java 25, flag `--enable-native-access=ALL-UNNAMED` mencegah peringatan akses native dari driver SQLite. Bila memakai JDK yang lebih lama dan flag tersebut tidak dikenal, hilangkan flag itu.

Menu yang tersedia:

1. Pilih barang aktif
2. Tambah barang
3. Terima stok
4. Keluarkan stok dari batch yang dipilih (manual)
5. Keluarkan stok otomatis (FEFO)
6. Lihat saldo dan riwayat
7. Pemantauan Kedaluwarsa
8. Pembuangan Stok Kedaluwarsa
9. Atur stok minimum
10. Keluar

### Mode demo di memori

```powershell
java -cp "out;lib/*" kantinflow.Main demo
```

Mode demo tidak membuka atau mengubah database.

## Lokasi database

Database default berada di:

```text
data/kantinflow.db
```

File dan foldernya dibuat otomatis saat menu atau GUI pertama kali dijalankan. Database berisi tabel `barang`, `batch`, `transaksi`, dan `rincian_transaksi`; satu baris `barang` dapat memiliki banyak batch dan transaksi serta menyimpan `stok_minimum`. Database lama dimigrasikan secara transaksional dengan menambahkan kolom `stok_minimum` bernilai awal 0; batch dan riwayat lama tidak diubah. Folder `data/` diabaikan Git agar data lokal tidak ikut tersimpan di repo.
Nama dan satuan barang yang sudah tersimpan belum memiliki API ubah; halaman Barang tidak menampilkan tombol Edit palsu. Perubahan identitas barang harus menjadi tahap terpisah dengan migrasi/persetujuan data historis.

Untuk memakai database uji terpisah:

```powershell
$env:KANTINFLOW_DB = "data/kantinflow-uji.db"
java --enable-native-access=ALL-UNNAMED -cp "out;lib/*" kantinflow.Main
```

Untuk mengulang pengujian dari awal, hentikan aplikasi lalu hapus database uji:

```powershell
Remove-Item data/kantinflow-uji.db
```

Baca [rancangan dan skenario uji](docs/rancangan-stok-masuk.md) untuk konteks pengembangan. Untuk mencoba aplikasi dari clone baru, ikuti [panduan demo](docs/panduan-demo.md). Ringkasan kelas, skema, exception, dan hasil pengujian tersedia di [ringkasan implementasi PBO](docs/ringkasan-implementasi-pbo.md). Aturan visual Swing/FlatLaf tersedia di [pedoman UI](docs/pedoman-ui.md). Bukti tangkapan layar pengujian GUI tersedia di `docs/bukti-gui/`.

Regression test domain dan penyimpanan dapat dijalankan dengan:

```powershell
javac -cp "out;lib/*" -d tests-out tests/RegresiKantinFlow.java
java --enable-native-access=ALL-UNNAMED -cp "tests-out;out;lib/*" RegresiKantinFlow
```

Harness GUI menggunakan database uji terpisah dan dapat dijalankan dengan:

```powershell
javac -cp "out;lib/*" -d tests-out tests/SwingRiwayatHarness.java tests/SwingActiveBarangHarness.java tests/SwingEmptyBarangHarness.java tests/SwingBarangHarness.java tests/CsvExportHarness.java tests/SwingExportHarness.java tests/PdfExportHarness.java tests/SwingPdfExportHarness.java tests/VisualThemeHarness.java
java --enable-native-access=ALL-UNNAMED -cp "tests-out;out;lib/*" CsvExportHarness
java --enable-native-access=ALL-UNNAMED -cp "tests-out;out;lib/*" SwingExportHarness
java --enable-native-access=ALL-UNNAMED -cp "tests-out;out;lib/*" PdfExportHarness
java --enable-native-access=ALL-UNNAMED -cp "tests-out;out;lib/*" SwingPdfExportHarness
java --enable-native-access=ALL-UNNAMED -cp "tests-out;out;lib/*" SwingRiwayatHarness
java --enable-native-access=ALL-UNNAMED -cp "tests-out;out;lib/*" SwingActiveBarangHarness
java --enable-native-access=ALL-UNNAMED -cp "tests-out;out;lib/*" SwingEmptyBarangHarness
java --enable-native-access=ALL-UNNAMED -cp "tests-out;out;lib/*" SwingBarangHarness
java --enable-native-access=ALL-UNNAMED -cp "tests-out;out;lib/*" VisualThemeHarness
```

Resource ikon, `KantinFlow.properties`, font PDF, dan font Inter (dari JAR) dibaca dari classpath. Jika mengompilasi manual, salin juga `src/main/resources/*` ke `out/` sebelum menjalankan GUI. Font DejaVu dan lisensinya ikut tersalin ke `out/kantinflow/fonts/`.

### Dependensi Maven/Gradle

Maven (tambahkan `swing-toast-notifications` sebagai system/local JAR karena tidak tersedia di Maven Central):

```xml
<dependency><groupId>com.formdev</groupId><artifactId>flatlaf</artifactId><version>3.8</version></dependency>
<dependency><groupId>com.formdev</groupId><artifactId>flatlaf-extras</artifactId><version>3.8</version></dependency>
<dependency><groupId>com.formdev</groupId><artifactId>flatlaf-fonts-inter</artifactId><version>3.19</version></dependency>
<dependency><groupId>com.github.weisj</groupId><artifactId>jsvg</artifactId><version>2.2.0</version></dependency>
<dependency><groupId>org.knowm.xchart</groupId><artifactId>xchart</artifactId><version>3.8.8</version></dependency>
<dependency><groupId>org.xerial</groupId><artifactId>sqlite-jdbc</artifactId><version>3.54.1.0</version></dependency>
```

Gradle:

```gradle
implementation 'com.formdev:flatlaf:3.8'
implementation 'com.formdev:flatlaf-extras:3.8'
implementation 'com.formdev:flatlaf-fonts-inter:3.19'
implementation 'com.github.weisj:jsvg:2.2.0'
implementation 'org.knowm.xchart:xchart:3.8.8'
implementation 'org.xerial:sqlite-jdbc:3.54.1.0'
// lib/swing-toast-notifications-1.0.4.jar (lokal, bukan Maven Central)
```

PDFBox 3.0.8/FontBox/PDFBox IO/Commons Logging tetap diperlukan untuk ekspor PDF seperti pada `setup.ps1`.
