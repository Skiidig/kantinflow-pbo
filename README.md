# KantinFlow PBO

Proyek Java untuk mata kuliah Pemrograman Berorientasi Objek. Aplikasi konsol ini menyediakan penerimaan stok, pengeluaran manual, pengeluaran otomatis FEFO, pemantauan kedaluwarsa, pembuangan stok kedaluwarsa, saldo, dan riwayat transaksi. Menu interaktif menyimpan data ke SQLite lokal; mode demo empat skenario tetap berjalan sepenuhnya di memori. GUI Swing menyediakan jendela penerimaan, pengeluaran manual dari batch yang dipilih, pengeluaran otomatis FEFO, tabel batch, dan riwayat transaksi untuk Susu Kotak. GUI dan login untuk fitur lain menyusul.

## Persiapan lokal

Pastikan `java -version` dan `javac -version` berjalan. Proyek diuji dengan JDK 25.0.5 dan memakai SQLite JDBC 3.54.1.0.

### Memasang SQLite JDBC

Driver tidak disertakan dalam riwayat Git karena `.jar` diabaikan. Unduh sekali ke folder `lib`:

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

```powershell
java --enable-native-access=ALL-UNNAMED -cp "out;lib/*" kantinflow.Main gui
```

GUI memakai database yang sama dengan menu konsol (default `data/kantinflow.db`). Jendela menampilkan saldo fisik, tabel batch, formulir penerimaan, pengeluaran manual dari batch yang dipilih, form FEFO dengan jumlah dan tanggal pengeluaran, serta riwayat transaksi. Perubahan langsung disimpan ke SQLite; setelah simpan atau gagal, tabel, saldo, dan riwayat dimuat ulang dari database.

Tanpa argumen aplikasi membuka menu konsol. Argumen `demo` tetap menjalankan empat skenario di memori.

Pada Java 25, flag `--enable-native-access=ALL-UNNAMED` mencegah peringatan akses native dari driver SQLite. Bila memakai JDK yang lebih lama dan flag tersebut tidak dikenal, hilangkan flag itu.

Menu yang tersedia:

1. Terima stok
2. Keluarkan stok dari batch yang dipilih (manual)
3. Keluarkan stok otomatis (FEFO)
4. Lihat saldo dan riwayat
5. Pemantauan Kedaluwarsa
6. Pembuangan Stok Kedaluwarsa
7. Keluar

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

File dan foldernya dibuat otomatis saat menu atau GUI pertama kali dijalankan. Database berisi tabel `barang`, `batch`, `transaksi`, dan `rincian_transaksi`. Folder `data/` diabaikan Git agar data lokal tidak ikut tersimpan di repo.

Untuk memakai database uji terpisah:

```powershell
$env:KANTINFLOW_DB = "data/kantinflow-uji.db"
java --enable-native-access=ALL-UNNAMED -cp "out;lib/*" kantinflow.Main
```

Untuk mengulang pengujian dari awal, hentikan aplikasi lalu hapus database uji:

```powershell
Remove-Item data/kantinflow-uji.db
```

Baca [rancangan dan skenario uji](docs/rancangan-stok-masuk.md) untuk konteks pengembangan. Bukti tangkapan layar pengujian GUI tersedia di `docs/bukti-gui/`.
