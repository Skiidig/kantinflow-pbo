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

Font utama adalah **Inter** dari `flatlaf-fonts-inter` 3.19
(`FlatInterFont`), dengan fallback Segoe UI/Arial bila font gagal dimuat.
Ukuran aktual yang dipakai:

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
| Lebar maksimum konten | 1200 px |
| Lebar maksimum Ringkasan | 1400 px |
| Lebar maksimum input form | 460 px |
| Lebar minimum kolom tabel | 84 px |
| Jarak konten dari sidebar | 24 px |
| Padding halaman | 24 px atas/bawah/kiri/kanan |
| Padding kartu | 20 px |
| Jarak internal kartu / padding | 20 px |
| Jarak grid form | 16 px |
| Tinggi input | 34 px |
| Tinggi tombol | 36 px |
| Tinggi baris tabel | 40 px (minimum dari metrik font) |
| Tinggi header tabel | 44 px (minimum dari metrik font) |
| Padding horizontal sel | 12 px |
| Radius kartu | 16 px |
| Radius kontrol/tombol | 8 px |
| Ambang form satu kolom | lebar < 760 px |
| Ambang filter Riwayat dua kolom | lebar < 520 px |
| Ambang ringkasan Pemantauan | 4 kolom ≥ 900 px; 2 kolom ≥ 520 px; 1 kolom < 520 px |

## 2. Pola komponen

### Sidebar

- Latar permukaan tema (putih di mode terang, gelap di mode gelap), lebar
  240 px, border kanan `Ui.GARIS`.
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
- Permukaan putih, border `Ui.GARIS`, radius 16 px, padding 20 px.
- Kartu nilai memakai `Ui.kartuNilai(...)` agar urutan label–nilai–detail
  konsisten.
- Jangan membuat kartu untuk elemen kecil; grupkan kontrol dalam satu kartu.

### CardPanel, StatCard, Badge, dan EmptyState

- `CardPanel` adalah permukaan kartu reusable: arc 16, border tipis, padding 20.
- `StatCard` menampilkan ikon dalam lingkaran berwarna, angka besar, label, dan
  strip status. Ringkasan memakai empat StatCard.
- `Badge` adalah pill status/jenis. Gunakan `Badge.status(teks)` atau
  `Ui.pasangBadge(tabel, kolom...)` agar warna konsisten.
- `BadgeRenderer` dipakai ulang per kolom: badge disusun dengan
  `GridBagLayout` dan `anchor=CENTER`, tinggi/padding mengikuti metrik font,
  bukan tinggi baris yang dikunci. Renderer menyimpan cache badge per teks
  dan satu border, sehingga tidak membuat komponen baru pada setiap paint.
- Warna semantik: Penerimaan hijau; Pengeluaran Manual/FEFO oranye;
  Pembuangan merah; Aman/Cukup hijau; Segera kedaluwarsa/Perlu restok
  kuning; Kedaluwarsa merah; Habis netral. Teks tetap wajib ada, warna
  bukan satu-satunya pembeda.
- Badge harus terbaca pada keadaan normal, hover, baris terpilih, dan
  nonaktif. Jangan menimpa warna teks badge dengan warna teks tabel.
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
- Gunakan `Ui.gridForm(2, Ui.AMBANG_FORM_SEMPIT, ...)` untuk form dua
  kolom yang berubah menjadi satu kolom pada jendela sempit. Setiap sel
  input dibatasi `Ui.LEBAR_INPUT_MAKS` (460 px), tetapi tetap mengisi tinggi
  sel agar sejajar dengan label/pesan.
- Jangan memakai grid form untuk toolbar aksi atau dialog; di sana gunakan
  `FlowLayout`/`BorderLayout` agar tombol dan label tidak melebar tanpa
  batas.
- Filter Riwayat memakai `Ui.gridForm(2, 520, ...)`: dua kolom pada 960,
  satu kolom pada jendela yang lebih sempit. Ringkasan Pemantauan memakai
  `Ui.gridBertahap(4, 900, 520, ...)`.
- Tombol utama memakai `Ui.utama`; pemeriksaan memakai `Ui.sekunder`; tindakan
  pembuangan memakai `Ui.bahaya`.
- Pesan validasi memakai `Ui.pesan()` dan diletakkan di bawah field atau
  formulir yang bermasalah.
- Setelah sukses/gagal, pesan hasil tampil sebagai notifikasi/toast; status
  aksesibilitas tersembunyi tetap menyimpan teks yang sama untuk harness.
  Jangan menggantinya dengan warna saja.

### Tabel

- Gunakan `Ui.model(...)`, `Ui.tabel(model)`, dan `Ui.scrollTabel(tabel, tinggi)`.
- Tetapkan lebar kolom secara eksplisit dengan `Ui.aturKolom(tabel, ...)`,
  bukan membiarkan semua kolom sama lebar. Kolom angka lebih sempit dan
  rata kanan; teks panjang (Alasan, Nama) mendapat porsi lebih besar;
  batas minimum kolom 84 px agar tanggal/status tidak terpotong pada 960.
- Tabel mengisi lebar kontainer; `JScrollPane` tabel hanya menggulir
  vertikal. Halaman tidak boleh menggulir horizontal.
- Tinggi tabel per halaman memakai konstanta `Ui.TINGGI_TABEL_*`.
- Angka rata kanan dengan `Ui.rataKanan(tabel, indeksKolom...)`.
- Header memakai latar halaman dan teks sekunder; tingginya mengikuti
  metrik font dengan minimum 44 px.
- Baris terpilih memakai `Ui.AKSEN_LEMBUT`; teks terpilih harus memakai
  `Ui.TEKS` agar tetap kontras di tema terang dan gelap.
- Baris aktif pada tabel Barang memakai `Ui.AKSEN_LEMBUT`; pada tema gelap
  teksnya memakai `Ui.TEKS`, bukan aksen biru.
- Sediakan `Ui.kosong("...")` dan tampilkan hanya saat model kosong; jangan
  menampilkan tabel kosong dan EmptyState bersamaan.
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

### Halaman Pemantauan

- Saat halaman pertama dibuka, tanggal acuan default adalah hari ini dan
  tabel langsung terisi tanpa menunggu tombol **Periksa**.
- Tanggal yang dipilih pengguna dipertahankan saat berpindah halaman,
  mengganti barang, atau memuat ulang panel. Jangan mengosongkan field
  tanggal di `perbarui()`.
- Tombol **Periksa Kedaluwarsa** tetap ada. Tanggal yang tidak valid
  menampilkan pesan di dekat field dan **mempertahankan hasil valid
  sebelumnya**; jangan menggantinya dengan tabel kosong.
- Hasil menampilkan tepat satu kondisi: tabel batch dengan `sisa > 0`
  (termasuk batch yang sudah kedaluwarsa), EmptyState "Tidak ada batch
  bersisa", atau EmptyState "Belum ada barang aktif" dengan arahan ke
  halaman Barang. Jangan menampilkan tabel dan EmptyState bersamaan.
- Batch dengan sisa 0 tidak ditampilkan; semua batch bersisa ditampilkan.
- Ringkasan stok fisik, stok layak, minimum, dan status memakai tanggal
  acuan yang sama. Batch yang kedaluwarsa tepat pada tanggal acuan masih
  dihitung layak sesuai aturan domain; jangan mengubah aturan kedaluwarsa.
- Memeriksa pemantauan tidak boleh mengubah saldo, batch, atau riwayat.

### Pesan dan status

- Error field: `Ui.pesan()` berwarna `Ui.MERAH`, 11 px.
- Status sukses/gagal global: `konteks.tampilkanPesan(...)` memunculkan
  toast; status aksesibilitas tersembunyi tetap menyimpan teks yang sama
  dan tidak hanya mengandalkan warna.
- Status proses: teks `Memproses...`, `Menyimpan...`, atau `Memuat...` selama
  operasi database; kontrol terkait dinonaktifkan.
- Empty state: kalimat penjelas dan tindakan yang relevan, bukan tabel kosong
  tanpa konteks.

## 3. Perilaku layout

### 1366 × 768

- Sidebar mengikuti metrik dan skala; area konten luas.
- Halaman dibungkus `HalamanScroll`: konten rata kiri, berjarak 24 px dari
  area kerja setelah sidebar, dan lebarnya `min(viewport, 1200)`; Ringkasan
  memakai batas 1400. Halaman tetap dapat menggulir vertikal.
- Form dua kolom bila lebarnya ≥ 760 px; tiap input maksimal 460 px.
- Kartu Ringkasan tersusun 2 × 2; ringkasan Pemantauan empat kolom bila
  lebarnya ≥ 900 px. Filter Riwayat memakai dua kolom (ambang 520 px).
- Barang menampilkan petunjuk, pencarian, tabel, dan area aksi terpilih;
  Stok Masuk menampilkan form dan tabel; Stok Keluar memakai tab Manual/FEFO.
- Halaman tidak memiliki scrollbar horizontal; konten panjang digulir
  vertikal.

### 960 × 640

- Panel mengikuti lebar viewport (sekitar 696 px setelah sidebar dan
  padding), bukan lebar tetap.
- Grid form berubah menjadi satu kolom pada lebar < 760 px; filter Riwayat
  tetap dua kolom karena ambangnya 520 px.
- Ringkasan Pemantauan berubah menjadi 2 × 2; jangan memaksa empat kolom
  yang membuat label terpotong.
- Sidebar tetap dapat digulir; dropdown barang aktif dan seluruh menu tetap
  terjangkau.
- Tombol dan field tetap berada dalam alur layout, bukan posisi absolut.
- Tombol aksi Barang harus terbaca penuh; tabel memakai scroll vertikal bila
  daftar panjang.
- Tabel tidak boleh menggulir horizontal; lebar kolom minimum 84 px dan
  kolom teks panjang mendapat porsi lebih besar.
- Dialog tidak boleh melampaui area layar; isi dialog dapat digulir.

### 2560 × 1000 (lebar)

- Konten tetap rata kiri dengan jarak 24 px setelah sidebar; lebar maksimum
  1200 px (Ringkasan 1400 px). Sisa ruang di kanan adalah latar halaman,
  bukan stretched card atau input.
- Form tetap dua kolom dengan input maksimal 460 px; tabel mengisi lebar
  konten, bukan lebar monitor.
- Tidak ada scrollbar horizontal pada halaman maupun tabel. Ukuran ini
  adalah uji ukuran jendela, bukan simulasi DPI.

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
