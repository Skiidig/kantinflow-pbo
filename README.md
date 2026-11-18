# KantinFlow PBO

Proyek Java untuk mata kuliah Pemrograman Berorientasi Objek. Aplikasi konsol ini menyediakan penerimaan stok, pengeluaran manual, pengeluaran otomatis FEFO, pemantauan kedaluwarsa, pembuangan stok kedaluwarsa, saldo, riwayat transaksi, dan stok minimum untuk beberapa barang. Menu interaktif menyimpan data ke SQLite lokal; mode demo empat skenario tetap berjalan sepenuhnya di memori.

GUI utama menggunakan **Swing + FlatLaf** dengan tujuh halaman: Ringkasan, Barang, Stok Masuk, Stok Keluar, Pemantauan, Pembuangan, dan Riwayat. Aplikasi dapat dijalankan tanpa build tambahan.

## Tujuan dan berkas kritis

Tujuan proyek mengikuti deskripsi di README ini: menyediakan penerimaan stok,
pengeluaran manual, pengeluaran otomatis FEFO, pemantauan kedaluwarsa,
pembuangan stok kedaluwarsa, saldo, riwayat transaksi, dan stok minimum untuk
beberapa barang. Menu interaktif menyimpan data ke SQLite lokal; mode demo empat
skenario berjalan sepenuhnya di memori. GUI utama adalah Swing dengan tema
FlatLaf dan tujuh halaman.

Berkas yang paling menentukan perilaku aplikasi:

- `src/main/java/kantinflow/Barang.java`, `src/main/java/kantinflow/BatchStok.java`, dan `src/main/java/kantinflow/FefoAllocator.java` — aturan barang, batch, status kedaluwarsa, dan urutan alokasi FEFO.
- `src/main/java/kantinflow/PenerimaanStok.java`, `src/main/java/kantinflow/PengeluaranStok.java`, dan `src/main/java/kantinflow/PembuanganStok.java` — transaksi domain.
- `src/main/java/kantinflow/penyimpanan/PenyimpananSQLite.java` — seluruh akses JDBC dan skema `barang`, `batch`, `transaksi`, `rincian_transaksi`.
- `src/main/java/kantinflow/Main.java` — entry point konsol, demo, dan GUI Swing; memasang `FlatLightLaf`.
- `src/main/java/kantinflow/gui/JendelaKantinFlow.java`, panel halaman di `src/main/java/kantinflow/gui/`, dan `Ui.java` — shell, tujuh halaman, serta gaya FlatLaf.
- `run-gui.cmd` dan `setup.ps1` — peluncur sekali klik dan penyiapan dependensi SQLite/FlatLaf.
- `tests/RegresiKantinFlow.java` — regression domain dan penyimpanan.

## Persiapan lokal

Pastikan `java -version` dan `javac -version` berjalan. Proyek diuji dengan JDK 25.0.5 dan memakai SQLite JDBC 3.54.1.0 serta FlatLaf 3.8. Tidak diperlukan dependensi GUI tambahan.

### Menjalankan GUI sekali klik di Windows

Persiapan awal untuk teman kelompok:

1. Clone repo ini.
2. Pasang JDK 25, lalu pastikan perintah `java` dan `javac` dapat dijalankan dari Command Prompt. Cek dengan `java -version` dan `javac -version`.
3. Buka PowerShell di akar repo, lalu siapkan dependensi sekali:
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\setup.ps1
   ```
4. Buka `run-gui.cmd` dengan klik dua kali. Skrip akan memeriksa JDK, SQLite JDBC, dan FlatLaf, mengompilasi seluruh sumber Java ke `out`, lalu membuka GUI.

`run-gui.cmd` selalu bekerja dari folder tempat skrip berada, jadi tetap dapat dijalankan dari folder kerja lain atau lewat shortcut. Jika Java, `javac`, atau dependensi belum siap, skrip menampilkan petunjuk dan tidak melanjutkan kompilasi. Jika kompilasi atau peluncuran gagal, pesan kesalahan ditampilkan sebelum jendela ditutup.

`setup.ps1` menyiapkan SQLite JDBC `3.54.1.0` dan FlatLaf `3.8` dari Maven Central. Jika JAR di `lib/` sudah ada dan valid, file itu dipakai tanpa mengunduh ulang. Unduhan baru ditulis ke file sementara, diperiksa isinya, lalu dipindahkan ke nama akhir; file parsial atau rusak tidak dianggap sebagai dependensi siap pakai. Jika jalur HTTPS PowerShell gagal, skrip mencoba unduhan melalui Java sebelum melaporkan kegagalan.

### Memasang dependensi secara manual

Cara termudah adalah `setup.ps1` di atas. Jika ingin manual, unduh kedua JAR ke folder `lib`:

```powershell
New-Item -ItemType Directory -Force lib | Out-Null
Invoke-WebRequest `
  -Uri "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.54.1.0/sqlite-jdbc-3.54.1.0.jar" `
  -OutFile "lib/sqlite-jdbc-3.54.1.0.jar"
Invoke-WebRequest `
  -Uri "https://repo1.maven.org/maven2/com/formdev/flatlaf/3.8/flatlaf-3.8.jar" `
  -OutFile "lib/flatlaf-3.8.jar"
```

Alternatif: salin manual kedua JAR ke `lib/`. Seluruh kode Java berada di `src/main/java/kantinflow/`, dengan `kantinflow.Main` sebagai titik masuk.

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

GUI utama memakai database yang sama dengan menu konsol (default `data/kantinflow.db`). Sidebar berisi brand KantinFlow, navigasi tujuh halaman berikon, dan footer status; header berisi judul halaman serta pemilih barang aktif. Halaman **Ringkasan** menampilkan tanggal acuan stok layak, kartu stok fisik/layak/minimum, dan status kebutuhan restok. Halaman **Barang** memuat daftar barang, tambah barang, dan minimum. **Stok Masuk** memuat form penerimaan dengan satuan di dekat jumlah dan daftar batch. **Stok Keluar** memisahkan mode Manual dan FEFO; sisa batch terpilih tampil dekat input jumlah. **Pemantauan** menampilkan tanggal, ringkasan stok, dan tabel status kedaluwarsa. **Pembuangan** menampilkan form dan ringkasan tindakan dengan tombol bahaya. **Riwayat** menampilkan tanggal, jenis, batch, jumlah, dan alasan.

Tema memakai FlatLaf Light dengan latar `#F5F6F8`, kartu putih, aksen biru `#014BAA`, border tipis, tabel dengan angka rata kanan, serta status teks berwarna. Halaman memakai scroll dan layout responsif; jendela default 1180×720 dan minimum 960×640. Mengganti barang memuat ulang seluruh halaman dan membersihkan pilihan batch serta hasil pemantauan lama.

Bukti antarmuka FlatLaf: [Ringkasan](docs/bukti-gui/gui-flatlaf-ringkasan.png), [Stok Masuk](docs/bukti-gui/gui-flatlaf-stok-masuk.png), [Riwayat](docs/bukti-gui/gui-flatlaf-riwayat.png), [Stok Keluar](docs/bukti-gui/gui-flatlaf-stok-keluar.png), dan [jendela kecil](docs/bukti-gui/gui-flatlaf-jendela-kecil.png).

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
