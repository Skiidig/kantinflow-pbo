# Panduan Demo KantinFlow

Panduan singkat untuk mencoba GUI Swing KantinFlow dari clone baru. GUI utama
memakai Swing + FlatLaf. Contoh di bawah memakai
tanggal demo **18 November 2026**. Jika dijalankan pada hari lain, gunakan tanggal
hari itu sebagai tanggal masuk/pengecekan, lalu sesuaikan tanggal kedaluwarsa:
batch A = tanggal demo + 2 hari, batch B = tanggal demo + 27 hari.

## 1. Siapkan dan jalankan

1. Clone repo, lalu buka foldernya.
2. Pasang JDK 25. Pastikan `java -version` dan `javac -version` berhasil.
3. Dari PowerShell di akar repo:
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\setup.ps1
   ```
   Jika unduhan HTTPS PowerShell gagal, `setup.ps1` mencoba jalur Java. Driver
   harus lolos pemeriksaan sebelum dipakai.
4. Buka `run-gui.cmd` dengan klik dua kali.

**Hasil yang diharapkan:** GUI Swing KantinFlow terbuka dengan Susu Kotak sebagai
barang aktif pertama. Jika JDK atau driver belum siap, skrip menampilkan pesan
yang jelas dan tidak membuka GUI.

## 2. Gunakan database demo terpisah

Tutup GUI jika baru dibuka, lalu di PowerShell:

```powershell
$env:KANTINFLOW_DB = "$PWD\data\kantinflow-demo-roti.db"
.\run-gui.cmd
```

Untuk memulai dari data kosong, tutup GUI lalu hapus database demo tersebut
lebih dahulu. Data pada `data/kantinflow.db` milik pengguna tidak tersentuh.

**Hasil yang diharapkan:** GUI memakai database demo, bukan database utama.

## 3. Tambahkan Roti

Buka halaman **Barang**, lalu klik **+ Tambah Barang**. Di dialog modal, isi:

- Nama barang: `Roti`
- Satuan: `bungkus`

Klik **Tambah Barang** di dialog. Jika memilih `Lainnya`, isi satuan khusus
pada field yang muncul. Satuan kosong atau hanya angka ditolak.

**Hasil yang diharapkan:** Roti menjadi barang aktif bila belum ada barang
aktif, saldo `0 bungkus`, dan pilihan barang aktif menampilkan
`Roti (bungkus)`. Tidak ada transaksi penerimaan otomatis.

## 4. Terima dua batch

Buka halaman **Stok Masuk**, lalu gunakan **Form Penerimaan** untuk batch pertama:

- Kode batch: `ROTI-A`
- Jumlah: `5`
- Tanggal masuk: `2026-11-18`
- Tanggal kedaluwarsa: `2026-11-20`

Klik **Simpan Penerimaan**. Ulangi untuk batch kedua:

- Kode batch: `ROTI-B`
- Jumlah: `7`
- Tanggal masuk: `2026-11-18`
- Tanggal kedaluwarsa: `2026-12-15`

**Hasil yang diharapkan:** saldo fisik `12 bungkus`, dua batch (`ROTI-A=5`,
`ROTI-B=7`), dan dua baris riwayat penerimaan. Dengan tanggal demo di atas,
`ROTI-A` berstatus **Segera kedaluwarsa** dan `ROTI-B` **Aman** jika diperiksa
pada `2026-11-18`.

## 5. Keluarkan 8 lewat FEFO

Buka halaman **Stok Keluar**, lalu pada panel **Pengeluaran FEFO**:

- Jumlah: `8`
- Tanggal: `2026-11-18`

Klik **Keluarkan FEFO**.

**Hasil yang diharapkan:**

```text
Pengeluaran FEFO berhasil: ROTI-A 5 bungkus, ROTI-B 3 bungkus. Saldo: 4 bungkus.
ROTI-A = 0 bungkus
ROTI-B = 4 bungkus
```

Riwayat menampilkan satu transaksi FEFO dengan dua rincian batch.

## 6. Minta 5 lagi

Masih di halaman **Stok Keluar** panel FEFO, isi jumlah `5` dan tanggal `2026-11-18`, lalu klik
**Keluarkan FEFO**.

**Hasil yang diharapkan:**

```text
Stok layak tidak cukup: diminta 5 bungkus, tersedia 4 bungkus
```

Saldo tetap `4 bungkus`, `ROTI-B` tetap `4`, dan riwayat tidak bertambah.

## 7. Periksa dashboard, pemantauan, dan stok minimum

Buka **Ringkasan** untuk melihat empat kartu statistik, peringatan batch
kedaluwarsa, tabel semua barang dengan badge status, dan grafik stok fisik.
Gunakan tombol **Mode gelap/Mode terang** di sidebar untuk mencoba tema; pilihan
disimpan dan dipulihkan saat aplikasi dibuka kembali.

Buka halaman **Barang**, pilih baris **Roti**, lalu klik **Atur Minimum**.
Isi stok minimum `5` pada dialog, lalu klik **Simpan Minimum**. Nilai `0`
adalah nilai sah dan berarti peringatan restok muncul saat stok layak sudah
habis atau kurang. Mengatur minimum tidak mengganti barang aktif.
Kemudian buka halaman **Pemantauan**:

1. Isi tanggal pengecekan `2026-11-18`.
2. Klik **Periksa Kedaluwarsa**.

**Hasil yang diharapkan:**

```text
Fisik: 4 bungkus
Layak: 4 bungkus
Minimum: 5 bungkus
Status: Perlu restok
```

Tabel pemantauan hanya menampilkan batch bersisa:

```text
ROTI-B | 4 bungkus | 2026-12-15 | Aman
```

`ROTI-A` yang bersisa 0 tidak ditampilkan.

## 8. Tutup dan buka kembali

Tutup GUI, lalu jalankan lagi:

```powershell
.\run-gui.cmd
```

Pilih **Roti** pada pemilih barang aktif di sidebar.

**Hasil yang diharapkan:**

- Saldo Roti tetap `4 bungkus`.
- `ROTI-A` tetap `0`, `ROTI-B` tetap `4`.
- Riwayat tetap memuat dua penerimaan dan rincian FEFO A/B.
- Stok minimum Roti tetap `5 bungkus` pada tabel Barang dan dialog minimum.
- Setelah tanggal `2026-11-18` diperiksa lagi, status kembali **Perlu restok**;
  hasil pemeriksaan tanggal tidak disimpan sebagai transaksi.

## 9. Ekspor CSV

Buka halaman **Riwayat**. Untuk mencoba filter dan ekspor:

1. Isi pencarian atau pilih jenis/tanggal, lalu klik **Terapkan Filter**.
2. Biarkan kolom tanggal kosong jika ingin seluruh riwayat barang aktif.
3. Klik **Ekspor CSV** di area aksi tabel.
4. Pilih nama dan folder, lalu setujui. Jika file sudah ada, konfirmasi
   penimpaan.
5. Buka file CSV di Excel atau editor teks. File memakai UTF-8 BOM, header
   `Barang,Satuan,Tanggal transaksi,Jenis transaksi,Kode batch,Jumlah,Alasan`,
   dan urutan baris sesuai tabel saat tombol ditekan.

Jika memakai FEFO dua batch, ekspor tanpa filter batch berisi dua baris
rincian. Setelah filter batch hanya menyisakan satu baris, ekspor juga hanya
berisi satu rincian; dialog **Lihat Detail** tetap menampilkan transaksi FEFO
lengkap.

Membatalkan pemilihan file atau konfirmasi timpa tidak membuat file dan tidak
dianggap kegagalan. Data stok/riwayat tidak berubah karena ekspor hanya
membaca.

Untuk ekspor PDF, klik **Ekspor PDF** di samping Ekspor CSV. File awal memakai
nama `kantinflow-riwayat-<nama-barang>-<tanggal-ekspor>.pdf`. PDF A4 landscape
memuat ringkasan filter, jumlah transaksi/rincian, tabel dengan header berulang,
dan nomor halaman. Setelah berhasil, pilih **Buka PDF** bila ingin membukanya;
aplikasi tidak membuka file secara otomatis. Contoh file uji tersedia di
`docs/bukti-gui/contoh-riwayat.pdf`.

## Ringkasan hasil demo

| Pemeriksaan | Hasil |
|---|---|
| Setelah penerimaan | 12 bungkus, 2 batch, 2 riwayat |
| Setelah FEFO 8 | 4 bungkus, A=0, B=4 |
| FEFO 5 berikutnya | Ditolak, stok tetap 4 |
| Stok minimum | 5 bungkus |
| Status saat diperiksa | Perlu restok |
| Setelah reopen | Saldo, batch, riwayat, dan minimum tetap |
