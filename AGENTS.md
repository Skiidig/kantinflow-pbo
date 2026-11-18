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
- Pertahankan `setName(...)` komponen yang dipakai harness/pengujian dan
  istilah UI yang sudah ada.
- Untuk perubahan GUI, jalankan kompilasi, regression
  `tests/RegresiKantinFlow.java`, dan pemeriksaan tujuh halaman pada ukuran
  1366×768 serta 960×640.
- Jangan mengubah domain, skema SQLite, aturan stok, atau alur konsol hanya
  untuk menyesuaikan tampilan. Perubahan perilaku harus terpisah dan diuji.
