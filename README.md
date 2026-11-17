# KantinFlow PBO

Proyek Java untuk mata kuliah Pemrograman Berorientasi Objek. Tahap pertama merancang penerimaan stok dan mendemonstrasikannya lewat konsol dengan data di memori.

## Persiapan lokal

Pastikan `java -version` dan `javac -version` berjalan. Tahap pertama memakai Java standar dan dapat dikompilasi dengan `javac`.

Kode Java akan ditempatkan di `src/main/java/kantinflow/` dengan kelas `kantinflow.Main` sebagai titik masuk demo. Setelah kelas dibuat, jalankan dari akar repo di PowerShell.

```powershell
$javaSources = Get-ChildItem -Path src/main/java -Recurse -Filter *.java -File
javac -d out $javaSources.FullName
if ($LASTEXITCODE -eq 0) { java -cp out kantinflow.Main }
```

Baca [rancangan dan skenario uji Stok Masuk](docs/rancangan-stok-masuk.md) sebelum menulis kode.
