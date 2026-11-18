# Pedoman UI KantinFlow Swing + FlatLaf

Pedoman ini adalah acuan tampilan aplikasi desktop KantinFlow. Semua panel
Swing wajib memakai token dan komponen bersama di
`src/main/java/kantinflow/gui/Ui.java`; hindari menulis warna, font, radius,
atau padding baru langsung di panel kecuali benar-benar diperlukan.

## 1. Palet, tipografi, dan ukuran

### Palet

| Peran | Nilai | Pemakaian |
|---|---|---|
| Latar aplikasi | `#F5F6F8` | area halaman dan viewport scroll |
| Permukaan | `#FFFFFF` | sidebar, kartu, header, status bar |
| Border | `#E2E5EA` | kartu, tabel, header, sidebar |
| Border kuat | `#CFD5DE` | tombol sekunder dan fokus |
| Teks utama | `#20242C` | judul, nilai, isi tabel |
| Teks sekunder | `#606978` | label bantuan, header tabel, keterangan |
| Biru utama | `#014BAA` | tombol utama, aksen, navigasi aktif |
| Biru hover | `#013E8C` | hover kontrol biru |
| Biru lembut | `#E9EFF8` | latar navigasi aktif dan baris terpilih |
| Sukses | `#0F7A4D` | status cukup/aman dan pesan berhasil |
| Bahaya | `#B42318` | error, status perlu restok, tindakan pembuangan |
| Peringatan | `#8A5A00` | status segera kedaluwarsa |
| Hover netral | `#F8F3F0` | hover navigasi yang tidak aktif |

Warna status selalu disertai teks: `Perlu restok`, `Cukup`, `Aman`,
`Segera kedaluwarsa`, `Kedaluwarsa`, atau pesan validasi yang eksplisit.

### Tipografi

Font utama mengikuti font FlatLaf yang tersedia; pada Windows gunakan
**Segoe UI** (fallback Arial). Ukuran aktual yang dipakai:

| Peran | Ukuran | Gaya |
|---|---:|---|
| Judul halaman | 24 px | bold |
| Judul kartu | 16 px | bold |
| Isi, input, tabel | 13 px | regular |
| Label field | 11.5 px | bold |
| Catatan/deskripsi | 12 px | regular |
| Pesan error/empty/bantuan kecil | 11 px | regular |
| Nilai kartu/status | 22 px | bold |
| Brand sidebar | 20 px | bold |
| Navigasi | 13 px | regular; bold saat aktif |
| Eyebrow header | 9.5 px | bold |
| Status bar | 12 px | regular |
| Footer sidebar | 10 px | regular |

### Ukuran dan jarak

| Token | Nilai |
|---|---:|
| Lebar sidebar | 240 px |
| Jendela awal | 1180 × 720 |
| Ukuran minimum | 960 × 640 |
| Padding halaman | 22 px atas/bawah, 24 px kiri/kanan |
| Padding kartu | 16 px atas/bawah, 18 px kiri/kanan |
| Jarak internal kartu | 10 px |
| Jarak grid form | 16 px |
| Tinggi input | 34 px |
| Tinggi tombol | 36 px |
| Tinggi baris tabel | 36 px |
| Tinggi header tabel | 38 px |
| Padding horizontal sel | 12 px |
| Radius kartu | 12 px |
| Radius kontrol/tombol | 8 px |
| Ambang form satu kolom | lebar < 760 px |

## 2. Pola komponen

### Sidebar

- Latar putih, lebar 240 px, border kanan `Ui.GARIS`.
- Brand: kotak biru `KF` 32 × 32, nama **KantinFlow**, subjudul
  **Manajemen Persediaan**.
- Navigasi berikon teks sederhana; satu halaman aktif.
- Halaman aktif memakai latar `Ui.AKSEN_LEMBUT`, teks `Ui.AKSEN`, dan garis
  aksen kiri 3 px.
- Hover halaman tidak aktif memakai `Ui.HOVER`.
- Footer menampilkan `SQLite lokal · FlatLaf 3.8`.

### Header

- Latar putih dengan border bawah.
- Kiri: eyebrow, judul halaman 24 px, deskripsi singkat.
- Kanan: label **Barang aktif** dan `JComboBox` selebar 230 px; pilihan
  menampilkan `Nama (satuan)`.
- Judul dan deskripsi berasal dari `PanelKantin.getJudul()` dan
  `getKeterangan()`; jangan menduplikasi judul di dalam panel.

### Kartu

- Gunakan `Ui.kartu()` atau `Ui.kartu("Judul")`.
- Permukaan putih, border `Ui.GARIS`, radius 12 px, padding 16/18 px.
- Kartu nilai memakai `Ui.kartuNilai(...)` agar urutan label–nilai–detail
  konsisten.
- Jangan membuat kartu untuk elemen kecil; grupkan kontrol dalam satu kartu.

### Formulir

- Gunakan `Ui.field(label, komponen, labelPesan)`.
- Label selalu terlihat; tanda `*` untuk wajib isi.
- Placeholder hanya contoh, bukan pengganti label.
- Gunakan `Ui.gridResponsif(2, Ui.AMBANG_FORM_SEMPIT, ...)` untuk form dua
  kolom yang berubah menjadi satu kolom pada jendela sempit.
- Tombol utama memakai `Ui.utama`; pemeriksaan memakai `Ui.sekunder`; tindakan
  pembuangan memakai `Ui.bahaya`.
- Pesan validasi memakai `Ui.pesan()` dan diletakkan di bawah field atau
  formulir yang bermasalah.
- Setelah sukses/gagal, pesan hasil tetap tampil di status bar; jangan
  menggantinya dengan warna saja.

### Tabel

- Gunakan `Ui.model(...)`, `Ui.tabel(model)`, dan `Ui.scrollTabel(tabel, tinggi)`.
- Tinggi tabel per halaman memakai konstanta `Ui.TINGGI_TABEL_*`.
- Angka rata kanan dengan `Ui.rataKanan(tabel, indeksKolom...)`.
- Header memakai latar halaman, teks sekunder, dan tinggi 38 px.
- Baris terpilih memakai `Ui.AKSEN_LEMBUT`; jangan memilih warna baru.
- Sediakan `Ui.kosong("...")` dan tampilkan hanya saat model kosong.
- Gunakan `yyyy-MM-dd` untuk tanggal dan sertakan satuan pada jumlah.

### Pesan dan status

- Error field: `Ui.pesan()` berwarna `Ui.MERAH`, 11 px.
- Status sukses/gagal global: `konteks.tampilkanPesan(...)`, teks tetap
  terbaca di status bar dan tidak hanya mengandalkan warna.
- Status proses: teks `Memproses...`, `Menyimpan...`, atau `Memuat...` selama
  operasi database; kontrol terkait dinonaktifkan.
- Empty state: kalimat penjelas dan tindakan yang relevan, bukan tabel kosong
  tanpa konteks.

## 3. Perilaku layout

### 1366 × 768

- Sidebar 240 px; area konten luas.
- Form dua kolom bila lebarnya ≥ 760 px.
- Kartu Ringkasan tersusun 2 × 2.
- Barang menampilkan daftar dan dua form; Stok Masuk menampilkan form dan
  tabel; Stok Keluar memakai tab Manual/FEFO.
- Halaman tetap berada di dalam `JScrollPane`; konten panjang dapat digulir.

### 960 × 640

- Panel mengimplementasikan `Scrollable` dan mengikuti lebar viewport.
- Grid form berubah menjadi satu kolom pada lebar < 760 px.
- Sidebar tetap 240 px; konten dapat digulir vertikal.
- Tombol dan field tetap berada dalam alur layout, bukan posisi absolut.
- Tabel yang lebih lebar dapat digulir di dalam `JScrollPane` tabel.

## 4. Membuat panel baru

1. Buat kelas panel yang mewarisi `PanelKantin` dan panggil
   `super(konteks, "Judul", "Deskripsi singkat")`.
2. Gunakan `Ui.kartu`, `Ui.field`, `Ui.utama`/`Ui.sekunder`/`Ui.bahaya`,
   `Ui.model`, `Ui.tabel`, `Ui.scrollTabel`, `Ui.rataKanan`, `Ui.pesan`, dan
   `Ui.kosong`; jangan menulis warna/ukuran baru.
3. Susun konten dengan pola: kontrol utama di utara, konten/tabel di tengah,
   pesan hasil di selatan atau status bar.
4. Daftarkan panel di `JendelaKantinFlow.buatKonten()` dan tambahkan tombol
   navigasi di `buatSidebar()`.
5. Beri `setName(...)` yang stabil untuk komponen yang dipakai harness.
6. Jalankan kompilasi, regression, dan pemeriksaan halaman pada 1366×768 serta
   960×640.
