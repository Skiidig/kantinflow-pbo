# KantinFlow PBO

Proyek Java untuk mata kuliah Pemrograman Berorientasi Objek. Aplikasi konsol ini menyediakan penerimaan stok, pengeluaran manual, pengeluaran otomatis FEFO, pemantauan kedaluwarsa, pembuangan stok kedaluwarsa, saldo, riwayat transaksi, dan stok minimum untuk beberapa barang. Menu interaktif menyimpan data ke SQLite lokal; mode demo empat skenario tetap berjalan sepenuhnya di memori. GUI Swing menyediakan jendela penerimaan, pengeluaran manual, pengeluaran otomatis FEFO, pemantauan kedaluwarsa, pembuangan stok kedaluwarsa, pemilihan barang aktif, penambahan barang, pengaturan stok minimum, tabel batch, dan riwayat transaksi.

## Persiapan lokal

Pastikan `java -version` dan `javac -version` berjalan. Proyek diuji dengan JDK 25.0.5 dan memakai SQLite JDBC 3.54.1.0.

### Menjalankan GUI sekali klik di Windows

Persiapan awal untuk teman kelompok:

1. Clone repo ini.
2. Pasang JDK 25, lalu pastikan perintah `java` dan `javac` dapat dijalankan dari Command Prompt. Cek dengan `java -version` dan `javac -version`.
3. Buka PowerShell di akar repo, lalu siapkan driver SQLite sekali:
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\setup.ps1
   ```
4. Buka `run-gui.cmd` dengan klik dua kali. Skrip akan memeriksa JDK dan driver, mengompilasi seluruh sumber Java ke `out`, lalu membuka GUI.

`run-gui.cmd` selalu bekerja dari folder tempat skrip berada, jadi tetap dapat dijalankan dari folder kerja lain atau lewat shortcut. Jika Java, `javac`, atau driver belum siap, skrip menampilkan petunjuk dan tidak melanjutkan kompilasi. Jika kompilasi atau peluncuran gagal, pesan kesalahan ditampilkan sebelum jendela ditutup.

`setup.ps1` memakai driver SQLite JDBC versi `3.54.1.0` dari Maven Central. Jika file di `lib/` sudah ada dan valid, file itu dipakai tanpa mengunduh ulang. Unduhan baru ditulis ke file sementara, diperiksa isinya, lalu dipindahkan ke nama akhir; file parsial atau rusak tidak dianggap sebagai driver siap pakai. Jika jalur HTTPS PowerShell gagal, skrip mencoba unduhan melalui Java sebelum melaporkan kegagalan.

### Memasang SQLite JDBC secara manual

Cara termudah adalah `setup.ps1` di atas. Jika ingin manual, unduh sekali ke folder `lib`:

```powershell
New-Item -ItemType Directory -Force lib | Out-Null
$sqliteJar = "lib/sqlite-jdbc-3.54.1.0.jar"
Invoke-WebRequest `
  -Uri "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.54.1.0/sqlite-jdbc-3.54.1.0.jar" `
  -OutFile $sqliteJar
```

Alternatif: salin manual `sqlite-jdbc-3.54.1.0.jar` ke `lib/`. Seluruh kode Java berada di `src/main/java/kantinflow/`, dengan `kantinflow.Main` sebagai titik masuk.

### Kompilasi

Jalankan dari akar repo di PowerShell:

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

GUI memakai database yang sama dengan menu konsol (default `data/kantinflow.db`). Header memiliki pemilih barang aktif; tab **Tambah Barang** menyediakan isian nama dan satuan. Jendela menampilkan saldo, batch, formulir penerimaan, pengeluaran manual, form FEFO, serta tab Riwayat Transaksi, Pemantauan Kedaluwarsa, Pembuangan Stok, dan Tambah Barang. Tab pemantauan juga menampilkan stok fisik, stok layak pada tanggal pengecekan, stok minimum, dan status **Perlu restok**/**Cukup**; stok minimum dapat diubah dari tab tersebut. Perubahan langsung disimpan ke SQLite; setelah simpan atau gagal, tabel, saldo, dan riwayat dimuat ulang dari database.

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

Baca [rancangan dan skenario uji](docs/rancangan-stok-masuk.md) untuk konteks pengembangan. Untuk mencoba aplikasi dari clone baru, ikuti [panduan demo](docs/panduan-demo.md). Bukti tangkapan layar pengujian GUI tersedia di `docs/bukti-gui/`.
