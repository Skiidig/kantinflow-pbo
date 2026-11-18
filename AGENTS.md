# Instruksi Kontribusi KantinFlow

## GUI Swing + FlatLaf

- Baca `docs/pedoman-ui.md` sebelum mengubah tampilan.
- Gunakan token dan komponen bersama di
  `src/main/java/kantinflow/gui/Ui.java` (`Ui.LATAR`, `Ui.AKSEN`,
  `Ui.PADDING_HALAMAN`, `Ui.TINGGI_INPUT`, `Ui.utama`, `Ui.tabel`, dan
  sebagainya). Jangan menulis warna, font, radius, tinggi kontrol, atau
  padding baru langsung di panel kecuali ada alasan yang jelas.
- Pertahankan pola `PanelKantin`: judul dan deskripsi dari konstruktor,
  kontrol utama, konten/tabel, lalu pesan hasil atau status.
- Daftarkan panel baru di `JendelaKantinFlow` dan gunakan kembali
  `Ui.gridResponsif`, `Ui.field`, `Ui.scrollTabel`, serta `Ui.rataKanan`.
- Pemilih barang aktif hanya berada di sidebar; header tidak memiliki
  dropdown barang. Panel yang membutuhkan konteks menampilkan nama/satuan
  barang aktif melalui `perbarui()`.
- Jika form transaksi berubah, gunakan `PanelKantin.adaIsianBelumDisimpan()`
  agar shell meminta konfirmasi sebelum mengganti barang; reload internal
  tidak boleh meminta konfirmasi.
- Pertahankan `setName(...)` komponen yang dipakai harness/pengujian dan
  istilah UI yang sudah ada.
- Ikon sidebar memakai SVG di `src/main/resources/kantinflow/icons/` melalui
  `Ui.ikonMenu(...)`; jangan memakai path absolut atau simbol teks/emoji.
- Ekspor Riwayat memakai `RiwayatCsvExporter` dan `RiwayatPdfExporter`;
  jangan menulis aturan CSV/PDF di panel. Pertahankan snapshot urutan view,
  CSV UTF-8 BOM/CRLF, PDF A4 landscape, font DejaVu, header berulang, nomor
  halaman, dan penulisan atomik.
- Halaman Barang memakai `SwingBarangHarness`: klik baris hanya memilih,
  aksi memakai view→model, dan dialog tambah/minimum tidak boleh mengganti
  barang aktif secara diam-diam.
- Setelah mengubah GUI, jalankan `tests/SwingRiwayatHarness.java`,
  `tests/SwingActiveBarangHarness.java`, `tests/SwingEmptyBarangHarness.java`,
  `tests/SwingBarangHarness.java`, `tests/CsvExportHarness.java`,
  `tests/SwingExportHarness.java`, `tests/PdfExportHarness.java`,
  `tests/SwingPdfExportHarness.java`, dan `tests/VisualThemeHarness.java`;
  salin
  resource termasuk font PDF ke `out`, dan uji peluncur dari folder kerja lain
  bila mengubah `run-gui.cmd`.
- Untuk perubahan GUI, jalankan kompilasi, regression
  `tests/RegresiKantinFlow.java`, `tests/VisualThemeHarness.java`, dan
  pemeriksaan tujuh halaman pada ukuran 1366×768 serta 960×640.
- Tema visual memakai `KantinFlow.properties`, Inter, `Tema`, `CardPanel`,
  `StatCard`, `Badge`, `EmptyState`, dan `Notifikasi`; jangan menduplikasi
  warna/tipe kartu di panel.
- Jangan mengubah domain, skema SQLite, aturan stok, atau alur konsol hanya
  untuk menyesuaikan tampilan. Perubahan perilaku harus terpisah dan diuji.
