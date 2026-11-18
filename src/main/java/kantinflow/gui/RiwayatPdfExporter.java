package kantinflow.gui;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/**
 * Laporan PDF A4 landscape untuk snapshot Riwayat. Tidak ada penulisan byte
 * PDF manual; seluruh dokumen dibangun dengan Apache PDFBox.
 */
public final class RiwayatPdfExporter {
    public record Info(
            String barang,
            String satuan,
            LocalDateTime dibuat,
            String pencarian,
            String jenis,
            LocalDate tanggalMulai,
            LocalDate tanggalAkhir,
            int jumlahTransaksi,
            List<RiwayatCsvExporter.Baris> rincian) {
    }

    private static final float LEBAR = 842f;
    private static final float TINGGI = 595f;
    private static final float MARGIN = 36f;
    private static final float FOOTER = 24f;
    private static final float LEBAR_TABEL = LEBAR - (2 * MARGIN);
    private static final float[] KOLOM = {24f, 62f, 105f, 105f, 46f, 55f, 0f};
    private static final String[] JUDUL_KOLOM = {
        "No", "Tanggal", "Jenis transaksi", "Kode batch",
        "Jumlah", "Satuan", "Alasan"
    };
    private static final float LINE = 9.5f;
    private static final float PADDING = 3f;
    private static final float HEADER = 18f;
    private static final DateTimeFormatter WAKTU =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

    private RiwayatPdfExporter() {
    }

    public static void tulis(Info info, Path tujuan) throws IOException {
        if (info == null || info.rincian() == null || info.rincian().isEmpty()) {
            throw new IllegalArgumentException("Tidak ada rincian untuk diekspor");
        }
        if (tujuan == null) {
            throw new IllegalArgumentException("Tujuan ekspor wajib diisi");
        }
        Path file = tujuan.toAbsolutePath().normalize();
        Path folder = file.getParent();
        if (folder == null) {
            throw new IOException("Folder tujuan tidak valid");
        }
        Files.createDirectories(folder);
        Path sementara = Files.createTempFile(
                folder, ".kantinflow-riwayat-", ".tmp");
        boolean berhasil = false;
        try {
            try (PDDocument dokumen = new PDDocument()) {
                dokumen.getDocumentInformation().setTitle(
                        "Laporan Riwayat Stok - KantinFlow");
                dokumen.getDocumentInformation().setCreator("KantinFlow");
                new Laporan(dokumen, info).tulis();
                dokumen.save(sementara.toFile());
            }
            pindahkan(sementara, file);
            berhasil = true;
        } finally {
            if (!berhasil) {
                Files.deleteIfExists(sementara);
            }
        }
    }

    private static void pindahkan(Path sumber, Path tujuan) throws IOException {
        try {
            Files.move(sumber, tujuan,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(sumber, tujuan, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static final class Laporan {
        private final PDDocument dokumen;
        private final Info info;
        private final PDFont biasa;
        private final PDFont tebal;
        private PDPage halaman;
        private PDPageContentStream isi;
        private int nomorHalaman;
        private float y;

        private Laporan(PDDocument dokumen, Info info) {
            this.dokumen = dokumen;
            this.info = info;
            PDFont biasaDimuat = muatFont(dokumen, "DejaVuSans.ttf");
            PDFont tebalDimuat = muatFont(dokumen, "DejaVuSans-Bold.ttf");
            this.biasa = biasaDimuat != null ? biasaDimuat
                    : new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            this.tebal = tebalDimuat != null ? tebalDimuat
                    : new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        }

        private static PDFont muatFont(PDDocument dokumen, String nama) {
            try (InputStream input = RiwayatPdfExporter.class.getResourceAsStream(
                    "/kantinflow/fonts/" + nama)) {
                return input == null ? null : PDType0Font.load(dokumen, input, true);
            } catch (IOException | RuntimeException exception) {
                return null;
            }
        }

        private void tulis() throws IOException {
            halamanBaru(true);
            for (int index = 0; index < info.rincian().size(); index++) {
                tulisRincian(index + 1, info.rincian().get(index));
            }
            isi.close();
        }

        private void halamanBaru(boolean pertama) throws IOException {
            if (isi != null) {
                isi.close();
            }
            halaman = new PDPage(new PDRectangle(LEBAR, TINGGI));
            dokumen.addPage(halaman);
            isi = new PDPageContentStream(dokumen, halaman);
            nomorHalaman++;
            y = TINGGI - MARGIN;
            if (!pertama) {
                tulisTeks("Laporan Riwayat Stok - KantinFlow", tebal, 9f,
                        MARGIN, y);
                y -= 14f;
            } else {
                tulisInfo();
            }
            gambarHeaderTabel();
            gambarFooter();
        }

        private void tulisInfo() throws IOException {
            tulisTeks("Laporan Riwayat Stok", tebal, 20f, MARGIN, y);
            y -= 20f;
            tulisTeks("KantinFlow", biasa, 10f, MARGIN, y);
            y -= 17f;
            tulisTeks("Barang: " + info.barang() + " (" + info.satuan() + ")",
                    tebal, 10f, MARGIN, y);
            y -= 14f;
            tulisTeks("Dibuat: " + info.dibuat().format(WAKTU),
                    biasa, 8.5f, MARGIN, y);
            y -= 14f;
            String filter = "Pencarian: " + nilai(info.pencarian())
                    + "   |   Jenis: " + nilaiJenis()
                    + "   |   Tanggal mulai: " + tanggal(info.tanggalMulai())
                    + "   |   Tanggal akhir: " + tanggal(info.tanggalAkhir());
            y = tulisParagraf(filter, biasa, 8.5f, MARGIN, y, LEBAR_TABEL);
            y -= 2f;
            tulisTeks("Transaksi: " + info.jumlahTransaksi()
                    + "   |   Rincian: " + info.rincian().size(),
                    tebal, 9f, MARGIN, y);
            y -= 14f;
            y = tulisParagraf(
                    "Satu transaksi FEFO dapat memiliki beberapa rincian batch. "
                    + "Jumlah transaksi tidak dihitung dari jumlah baris.",
                    biasa, 8f, MARGIN, y, LEBAR_TABEL);
            y -= 6f;
        }

        private void gambarHeaderTabel() throws IOException {
            if (y - HEADER < batasBawah()) {
                halamanBaru(false);
                return;
            }
            float atas = y;
            float kiri = MARGIN;
            isi.setNonStrokingColor(new Color(0xE9EFF8));
            isi.addRect(kiri, atas - HEADER, LEBAR_TABEL, HEADER);
            isi.fill();
            isi.setNonStrokingColor(new Color(0x20242C));
            float x = kiri;
            for (int i = 0; i < JUDUL_KOLOM.length; i++) {
                float lebar = lebarKolom(i);
                tulisSel(JUDUL_KOLOM[i], tebal, 8f, x, atas, lebar,
                        i == 4 ? 1 : 0);
                x += lebar;
            }
            garisHorizontal(atas, kiri, LEBAR_TABEL);
            garisHorizontal(atas - HEADER, kiri, LEBAR_TABEL);
            y = atas - HEADER;
        }

        private void tulisRincian(int nomor, RiwayatCsvExporter.Baris baris)
                throws IOException {
            String[] nilai = {
                Integer.toString(nomor),
                baris.tanggal().toString(),
                baris.jenis(),
                baris.kodeBatch(),
                Integer.toString(baris.jumlah()),
                baris.satuan(),
                "".equals(baris.alasan() == null ? "" : baris.alasan())
                        ? "-" : baris.alasan()
            };
            List<List<String>> barisTerbungkus = new ArrayList<>();
            int jumlahBaris = 1;
            for (int i = 0; i < nilai.length; i++) {
                List<String> potongan = pecah(
                        nilai[i], i == 4 ? tebal : biasa, 8f,
                        lebarKolom(i) - (2 * PADDING));
                barisTerbungkus.add(potongan);
                jumlahBaris = Math.max(jumlahBaris, potongan.size());
            }

            float tinggiPenuh = (jumlahBaris * LINE) + (2 * PADDING);
            float kapasitasHalaman = TINGGI - MARGIN - HEADER
                    - batasBawah() - 14f;
            if (tinggiPenuh <= kapasitasHalaman) {
                if (y - tinggiPenuh < batasBawah()) {
                    halamanBaru(false);
                }
                gambarBaris(barisTerbungkus, 0, jumlahBaris, nomor, false);
                return;
            }

            int offset = 0;
            while (offset < jumlahBaris) {
                if (y - batasBawah() < (LINE + (2 * PADDING))) {
                    halamanBaru(false);
                }
                int dapat = (int) ((y - batasBawah() - (2 * PADDING)) / LINE);
                if (dapat <= 0) {
                    halamanBaru(false);
                    dapat = (int) ((y - batasBawah() - (2 * PADDING)) / LINE);
                }
                if (dapat <= 0) {
                    throw new IOException("Tinggi halaman tidak cukup untuk baris");
                }
                int ambil = Math.min(dapat, jumlahBaris - offset);
                gambarBaris(barisTerbungkus, offset, ambil, nomor,
                        offset > 0);
                offset += ambil;
                if (offset < jumlahBaris) {
                    halamanBaru(false);
                }
            }
        }

        private void gambarBaris(List<List<String>> nilai, int offset,
                int jumlah, int nomor, boolean lanjutan) throws IOException {
            float tinggi = (jumlah * LINE) + (2 * PADDING);
            float atas = y;
            float x = MARGIN;
            for (int i = 0; i < KOLOM.length; i++) {
                float lebar = lebarKolom(i);
                List<String> semua = nilai.get(i);
                List<String> potongan;
                if (offset >= semua.size()) {
                    potongan = List.of();
                } else {
                    int akhir = Math.min(semua.size(), offset + jumlah);
                    potongan = semua.subList(offset, akhir);
                }
                if (i == 0 && lanjutan) {
                    potongan = List.of("");
                }
                tulisSel(potongan, i == 4 ? tebal : biasa, 8f, x, atas,
                        lebar, i == 4 ? 1 : 0);
                x += lebar;
            }
            garisHorizontal(atas, MARGIN, LEBAR_TABEL);
            garisHorizontal(atas - tinggi, MARGIN, LEBAR_TABEL);
            garisVertikal(atas, atas - tinggi, MARGIN);
            x = MARGIN;
            for (float lebar : KOLOM) {
                x += lebar;
                garisVertikal(atas, atas - tinggi, x);
            }
            y -= tinggi;
        }

        private void tulisSel(List<String> baris, PDFont font, float ukuran,
                float x, float atas, float lebar, int rata)
                throws IOException {
            float baseline = atas - PADDING - ukuran;
            float kiri = x + PADDING;
            for (String teks : baris) {
                float posisi = kiri;
                if (rata == 1) {
                    posisi = x + lebar - PADDING - hitungLebar(teks, font, ukuran);
                }
                tulisTeks(teks, font, ukuran, posisi, baseline);
                baseline -= LINE;
            }
        }

        private void tulisSel(String teks, PDFont font, float ukuran,
                float x, float atas, float lebar, int rata)
                throws IOException {
            tulisSel(List.of(teks), font, ukuran, x, atas, lebar, rata);
        }

        private float lebarKolom(int indeks) {
            if (indeks == KOLOM.length - 1) {
                float tetap = 0f;
                for (int i = 0; i < KOLOM.length - 1; i++) {
                    tetap += KOLOM[i];
                }
                return LEBAR_TABEL - tetap;
            }
            return KOLOM[indeks];
        }

        private List<String> pecah(String teks, PDFont font, float ukuran,
                float lebar) throws IOException {
            List<String> hasil = new ArrayList<>();
            String semua = teks == null ? "" : teks.replace("\r\n", "\n")
                    .replace('\r', '\n');
            for (String paragraf : semua.split("\n", -1)) {
                if (paragraf.isEmpty()) {
                    hasil.add("");
                    continue;
                }
                String[] kata = paragraf.split(" ", -1);
                StringBuilder baris = new StringBuilder();
                for (String bagian : kata) {
                    String calon = baris.length() == 0
                            ? bagian : baris + " " + bagian;
                    if (hitungLebar(calon, font, ukuran) <= lebar) {
                        baris.setLength(0);
                        baris.append(calon);
                        continue;
                    }
                    if (baris.length() > 0) {
                        hasil.add(baris.toString());
                        baris.setLength(0);
                    }
                    if (hitungLebar(bagian, font, ukuran) <= lebar) {
                        baris.append(bagian);
                    } else {
                        pecahKata(bagian, font, ukuran, lebar, hasil);
                    }
                }
                if (baris.length() > 0) {
                    hasil.add(baris.toString());
                }
            }
            return hasil.isEmpty() ? List.of("") : hasil;
        }

        private void pecahKata(String kata, PDFont font, float ukuran,
                float lebar, List<String> hasil) throws IOException {
            StringBuilder potongan = new StringBuilder();
            for (int i = 0; i < kata.length(); i++) {
                char huruf = kata.charAt(i);
                String calon = potongan.toString() + huruf;
                if (hitungLebar(calon, font, ukuran) > lebar
                        && potongan.length() > 0) {
                    hasil.add(potongan.toString());
                    potongan.setLength(0);
                }
                potongan.append(huruf);
            }
            if (potongan.length() > 0) {
                hasil.add(potongan.toString());
            }
        }

        private float tulisParagraf(String teks, PDFont font, float ukuran,
                float x, float awal, float lebar) throws IOException {
            List<String> baris = pecah(teks, font, ukuran, lebar);
            float posisi = awal;
            for (String item : baris) {
                tulisTeks(item, font, ukuran, x, posisi);
                posisi -= ukuran + 2f;
            }
            return posisi;
        }

        private void tulisTeks(String teks, PDFont font, float ukuran,
                float x, float y) throws IOException {
            String aman = saring(teks);
            isi.beginText();
            isi.setFont(font, ukuran);
            isi.newLineAtOffset(x, y);
            isi.showText(aman);
            isi.endText();
        }

        private void garisHorizontal(float y, float x, float lebar)
                throws IOException {
            isi.setStrokingColor(new Color(0xCFD5DE));
            isi.setLineWidth(0.5f);
            isi.moveTo(x, y);
            isi.lineTo(x + lebar, y);
            isi.stroke();
        }

        private void garisVertikal(float atas, float bawah, float x)
                throws IOException {
            isi.setStrokingColor(new Color(0xCFD5DE));
            isi.setLineWidth(0.5f);
            isi.moveTo(x, atas);
            isi.lineTo(x, bawah);
            isi.stroke();
        }

        private void gambarFooter() throws IOException {
            String teks = "Halaman " + nomorHalaman;
            float lebar = hitungLebar(teks, biasa, 8f);
            tulisTeks(teks, biasa, 8f, LEBAR - MARGIN - lebar, MARGIN - 7f);
        }

        private float batasBawah() {
            return MARGIN + FOOTER;
        }

        private String saring(String teks) {
            String nilai = teks == null ? "" : teks;
            if (biasa instanceof PDType0Font) {
                return nilai;
            }
            StringBuilder hasil = new StringBuilder();
            for (char huruf : nilai.toCharArray()) {
                if (huruf == '\n' || huruf == '\r') {
                    hasil.append(huruf);
                    continue;
                }
                try {
                    biasa.encode(String.valueOf(huruf));
                    hasil.append(huruf);
                } catch (IllegalArgumentException | IOException exception) {
                    hasil.append('?');
                }
            }
            return hasil.toString();
        }

        private float hitungLebar(String teks, PDFont font, float ukuran)
                throws IOException {
            return font.getStringWidth(saring(teks)) / 1000f * ukuran;
        }

        private String nilai(String teks) {
            return teks == null || teks.isBlank() ? "-" : teks;
        }

        private String nilaiJenis() {
            return info.jenis() == null || info.jenis().isBlank()
                    ? "Semua" : info.jenis();
        }

        private String tanggal(LocalDate tanggal) {
            return tanggal == null ? "-" : tanggal.toString();
        }
    }
}
