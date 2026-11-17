# KantinFlow PBO

Proyek Java untuk mata kuliah Pemrograman Berorientasi Objek. Tahap ini menyediakan penerimaan stok, pengeluaran dari satu batch, riwayat transaksi, menu konsol interaktif, dan demo empat skenario dengan data di memori.

## Persiapan lokal

Pastikan `java -version` dan `javac -version` berjalan. Proyek memakai Java standar dan dapat dikompilasi dengan `javac`.

Kode Java berada di `src/main/java/kantinflow/` dengan kelas `kantinflow.Main` sebagai titik masuk. Setelah kelas dibuat, jalankan dari akar repo di PowerShell.

```powershell
$javaSources = Get-ChildItem -Path src/main/java -Recurse -Filter *.java -File
javac -d out $javaSources.FullName
if ($LASTEXITCODE -eq 0) { java -cp out kantinflow.Main }
```

Menu interaktif berjalan tanpa argumen. Untuk memeriksa ulang demo empat skenario:

```powershell
java -cp out kantinflow.Main demo
```

Baca [rancangan dan skenario uji Stok Masuk](docs/rancangan-stok-masuk.md) sebelum menulis kode.
