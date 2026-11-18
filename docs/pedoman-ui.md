# Pedoman UI KantinFlow Swing + FlatLaf

Pedoman ini adalah acuan tampilan aplikasi desktop KantinFlow. Semua panel
Swing wajib memakai token dan komponen bersama di
`src/main/java/kantinflow/gui/Ui.java`; hindari menulis warna, font, radius,
atau padding baru langsung di panel kecuali benar-benar diperlukan.

## 1. Palet, tipografi, dan ukuran

### Palet

| Peran | Nilai | Pemakaian |
|---|---|---|
| Latar aplikasi | `#F4F6FA` | area halaman dan viewport scroll |
| Permukaan | `#FFFFFF` | sidebar, kartu, header, status bar |
| Border | `#E2E8F0` | kartu, tabel, header, sidebar |
| Border kuat | `#CBD5E1` | tombol sekunder dan fokus |
| Teks utama | `#0F172A` | judul, nilai, isi tabel |
| Teks sekunder | `#64748B` | label bantuan, header tabel, keterangan |
| Biru utama | `#1D4ED8` | tombol utama, aksen, navigasi aktif |
| Biru hover | `#1E40AF` | hover kontrol biru |
| Biru lembut | `#DBEAFE` | latar navigasi aktif dan baris terpilih |
| Sukses | `#16A34A` | status cukup/aman dan pesan berhasil |
| Bahaya | `#DC2626` | error, status perlu restok, tindakan pembuangan |
| Peringatan | `#D97706` | status segera kedaluwarsa |
| Oranye | `#EA580C` | pembuangan dan kategori khusus |
| Hover netral | `#EEF2FF` | hover baris/navigasi yang tidak aktif |

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
| Jarak internal kartu / padding | 20 px |
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
- Di bawah brand, sebelum menu, ada label **Barang aktif** dan dropdown
  barang aktif. Dropdown mengikuti lebar sidebar, bukan header.
- Dropdown menampilkan `Nama (satuan)`, memiliki tooltip nama lengkap, dan
  tidak boleh memperlebar sidebar. Bila nama panjang, teks dipotong oleh
  lebar kontrol; tooltip tetap menampilkan nama lengkap.
- Sidebar memakai `JScrollPane`; dropdown dan tujuh menu tetap dapat
  dijangkau pada 960 × 640. Jangan membuat dropdown kedua di header/panel.
- Navigasi memakai ikon SVG outline dari `src/main/resources/kantinflow/icons/`
  dengan ukuran 20 px; label teks tetap tampil di samping ikon.
- Satu halaman aktif; ikon dan label berubah biru saat aktif, netral saat biasa.
- Halaman aktif memakai latar `Ui.AKSEN_LEMBUT`, teks `Ui.AKSEN`, dan garis
  aksen kiri 3 px.
- Hover halaman tidak aktif memakai `Ui.HOVER`.
- Footer menampilkan `SQLite lokal · FlatLaf 3.8`.
- Saat belum ada barang, dropdown dinonaktifkan, tampilkan pesan singkat
  untuk membuka halaman Barang, dan halaman aktif diarahkan ke Barang.

### Header

- Latar putih dengan border bawah.
- Hanya berisi eyebrow, judul halaman 24 px, dan deskripsi singkat.
- Pemilih barang aktif berada di sidebar; **jangan** menambahkan dropdown
  barang kedua di header atau panel halaman.
- Judul dan deskripsi berasal dari `PanelKantin.getJudul()` dan
  `getKeterangan()`; jangan menduplikasi judul di dalam panel.

### Konteks barang per halaman

- Halaman Stok Masuk, Stok Keluar, Pemantauan, Pembuangan, dan Riwayat
  menampilkan keterangan singkat di dekat area kerja, misalnya
  `Penerimaan untuk Roti · bungkus`.
- Keterangan diperbarui oleh `perbarui()` saat barang berubah.
- Halaman Barang tetap menampilkan seluruh barang dan form tambah/minimum.
- Jangan membawa pilihan batch, hasil alokasi, detail transaksi, filter
  Riwayat, atau pesan sukses barang lama ke barang baru.

### Kartu

- Gunakan `Ui.kartu()` atau `Ui.kartu("Judul")`.
- Permukaan putih, border `Ui.GARIS`, radius 12 px, padding 16/18 px.
- Kartu nilai memakai `Ui.kartuNilai(...)` agar urutan label–nilai–detail
  konsisten.
- Jangan membuat kartu untuk elemen kecil; grupkan kontrol dalam satu kartu.

### CardPanel, StatCard, Badge, dan EmptyState

- `CardPanel` adalah permukaan kartu reusable: arc 16, border tipis, padding 20.
- `StatCard` menampilkan ikon dalam lingkaran berwarna, angka besar, label, dan
  strip status. Ringkasan memakai empat StatCard.
- `Badge` adalah pill status/jenis. Gunakan `Badge.status(teks)` atau
  `Ui.pasangBadge(tabel, kolom...)` agar warna konsisten.
- `EmptyState` menampilkan ikon besar, judul, deskripsi, dan aksi opsional.
  Gunakan saat tabel/kartu tidak memiliki data.
- Tabel memakai `TabelKantin` dari `Ui.tabel`; hover baris memakai
  `Ui.HOVER`, bukan warna baru.

### Tema dan notifikasi

- Sumber tema ada di `src/main/resources/kantinflow/KantinFlow.properties`;
  panggil `FlatLaf.registerCustomDefaultsSource("kantinflow")` sebelum setup.
- Font antarmuka adalah Inter (`FlatInterFont`), bukan font sistem.
- Tombol mode gelap/terang berada di sidebar; preferensi disimpan di
  `data/kantinflow-tema.properties`. Jangan mengubah preferensi DPI Windows.
- Pesan hasil memakai `Notifikasi`/swing-toast-notifications; status footer
  tidak lagi menjadi tempat pesan utama. Status aksesibilitas tetap disimpan
  secara tersembunyi untuk harness.
- Animasi halaman memakai fade 150 ms; indikator sidebar bergeser dengan
  interpolasi pendek. Jangan menambah animasi yang menutupi interaksi lama.

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
- Header memakai latar halaman dan teks sekunder; tingginya mengikuti
  metrik font dengan minimum 44 px.
- Baris terpilih memakai `Ui.AKSEN_LEMBUT`; jangan memilih warna baru.
- Sediakan `Ui.kosong("...")` dan tampilkan hanya saat model kosong.
- Gunakan `yyyy-MM-dd` untuk tanggal dan sertakan satuan pada jumlah.

### Halaman Barang

- Urutan halaman: petunjuk singkat, pencarian + tombol `+ Tambah Barang`,
  tabel `Nama`, `Satuan`, `Stok fisik`, `Stok minimum`, lalu kartu
  **Aksi Barang**.
- Klik baris hanya memilih baris. Jangan mengganti barang aktif dari
  listener seleksi.
- `Jadikan Barang Aktif` wajib memakai mekanisme `konteks.gantiBarang` yang
  sudah ada, termasuk konfirmasi isian transaksi belum disimpan.
- `Atur Minimum` membuka dialog untuk barang yang dipilih, bukan selalu
  barang aktif. Simpan minimum tidak boleh mengganti barang aktif.
- Tandai barang aktif dengan label teks dan penanda baris; jangan hanya
  mengandalkan warna.
- Cari nama barang tanpa membedakan huruf besar/kecil. Aksi harus memakai
  `convertRowIndexToModel` setelah filter/sort.
- Bedakan empty state belum ada barang dan pencarian tanpa hasil.
- Form tambah berada di dialog modal, bukan kartu permanen. Nama barang,
  pilihan satuan (`buah`, `kotak`, `bungkus`, `botol`, `kaleng`, `Lainnya`),
  dan input satuan khusus bila memilih `Lainnya`.
- Tolak satuan kosong atau hanya angka dengan pesan yang menyebut jenis
  hitungan. Jangan mengubah satuan barang lama otomatis.
- Barang baru selalu dimulai tanpa stok/batch; arahkan pengguna ke Stok Masuk.
- Dialog minimum menampilkan nama/satuan, nilai awal yang ada, dan `0` sebagai
  nilai sah. Normalisasi whitespace sebelum parsing; tolak kosong, pecahan,
  nonangka, negatif, dan di luar rentang int dengan pesan dekat field.
- Dialog memakai `pack()`, dibatasi area layar yang dapat digunakan, dan
  memiliki scroll vertikal bila perlu. Fokus awal di field pertama; Escape
  membatalkan; Enter memakai default button dialog saja.
- Nama/satuan barang yang sudah tersimpan belum memiliki API ubah. Jangan
  menambahkan tombol Edit yang tidak berfungsi.

### Toolbar filter dan dialog detail

- Toolbar Riwayat memakai `Ui.kartu("Filter riwayat")`, `Ui.field(...)`,
  `Ui.utama("Terapkan Filter")`, dan `Ui.sekunder("Reset")`.
- Pencarian kode batch/alasan tidak membedakan huruf besar/kecil.
- Filter jenis dan tanggal bekerja bersama; tanggal memakai `yyyy-MM-dd` dan
  rentang inklusif. Tanggal mulai setelah tanggal akhir ditolak lewat
  `Ui.pesan()` dekat filter.
- Status jumlah memakai format `X transaksi · Y rincian ditampilkan`; satu
  transaksi FEFO tetap satu transaksi walau memiliki beberapa rincian.
- Dialog detail memakai `Ui.kartu`, `Ui.tabel`, dan `Ui.scrollTabel`; judul
  dialog `Detail Transaksi`.
- Tabel utama boleh berisi baris per rincian, tetapi dialog harus memakai
  objek transaksi asal agar rincian FEFO lengkap. Jangan menggabungkan
  transaksi berdasarkan tanggal/jenis.
- Sediakan empty state berbeda untuk belum ada riwayat dan tidak ada hasil
  filter.
- Tombol **Ekspor CSV** berada di area aksi tabel Riwayat, memakai ikon
  `export.svg` dan `Ui.sekunder`. Tombol nonaktif bila tidak ada rincian.
- Ekspor mengambil snapshot urutan tampilan tabel (termasuk sorting) saat
  tombol ditekan. Filter batch yang menyisakan satu rincian FEFO hanya
  mengekspor rincian tersebut; dialog detail tetap lengkap.
- CSV memakai header `Barang,Satuan,Tanggal transaksi,Jenis transaksi,
  Kode batch,Jumlah,Alasan`, UTF-8 BOM, CRLF, jumlah bilangan bulat, dan
  alasan kosong sebagai sel kosong.
- Nilai teks yang diawali `=`, `+`, `-`, atau `@` diberi apostrof di awal
  untuk mencegah eksekusi formula spreadsheet. Semua nilai teks yang
  membutuhkan escaping diberi kutip ganda.
- Penulisan dilakukan ke file sementara di folder tujuan, lalu dipindahkan
  secara atomik; file lama tidak boleh rusak bila penulisan gagal.
- Tombol **Ekspor PDF** berada di samping Ekspor CSV, memakai ikon `pdf.svg`
  dan `Ui.sekunder`. Keduanya tetap terjangkau pada 960 × 640.
- PDF memakai A4 landscape, margin, ringkasan filter, nama barang/satuan,
  waktu, jumlah transaksi/rincian, tabel dengan header berulang, nomor
  halaman, dan penjelasan rincian FEFO. Jangan menampilkan saldo hasil
  penjumlahan kolom Jumlah.
- PDF memakai Apache PDFBox dan font DejaVu Sans/Bold dari
  `src/main/resources/kantinflow/fonts/`; lisensi font disertakan di folder
  tersebut. Jangan memakai data identitas kampus atau logo tak terkonfigurasi.
- Setelah ekspor PDF berhasil, tampilkan opsi **Buka PDF** dan **Tutup**;
  jangan membuka otomatis.
- Saat form transaksi berisi isian dan pengguna mengganti barang melalui
  sidebar atau tabel Barang, tampilkan konfirmasi. Jika pengguna memilih
  batal, kembalikan pilihan dropdown dan pertahankan isian. Jika setuju,
  form dibersihkan. Reload internal setelah simpan tidak boleh memunculkan
  konfirmasi.

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

- Sidebar mengikuti metrik dan skala; area konten luas.
- Form dua kolom bila lebarnya ≥ 760 px.
- Kartu Ringkasan tersusun 2 × 2.
- Barang menampilkan petunjuk, pencarian, tabel, dan area aksi terpilih;
  Stok Masuk menampilkan form dan tabel; Stok Keluar memakai tab Manual/FEFO.
- Halaman tetap berada di dalam `JScrollPane`; konten panjang dapat digulir.

### 960 × 640

- Panel mengimplementasikan `Scrollable` dan mengikuti lebar viewport.
- Grid form berubah menjadi satu kolom pada lebar < 760 px.
- Sidebar tetap dapat digulir; dropdown barang aktif dan seluruh menu tetap
  terjangkau.
- Tombol dan field tetap berada dalam alur layout, bukan posisi absolut.
- Tombol aksi Barang harus terbaca penuh; tabel memakai scroll vertikal bila
  daftar panjang.
- Tabel yang lebih lebar dapat digulir di dalam `JScrollPane` tabel.
- Dialog tidak boleh melampaui area layar; isi dialog dapat digulir.

### Skala Windows 100%, 125%, dan 150%

- Jangan mengunci tinggi tombol/input ke piksel yang lebih kecil dari metrik
  font. Gunakan `Ui.skala`, padding FlatLaf, dan tinggi tabel yang diturunkan
  dari `FontMetrics`.
- Label tombol utama harus tetap utuh; jika ruang sempit, susun ulang atau
  biarkan halaman menggulir, bukan memotong teks.
- Dialog diuji dengan `pack()` dan batas `getMaximumWindowBounds()`.
- Pengujian skala dilakukan dengan properti proses
  `-Dsun.java2d.uiScale=1.25`/`1.5` (simulasi HiDPI), bukan dengan mengubah
  konfigurasi DPI Windows pengguna.
- Hasil simulasi tidak menggantikan uji monitor fisik; catat skala mana yang
  benar-benar diuji.

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
