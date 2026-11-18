import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

import kantinflow.gui.RiwayatCsvExporter;
import kantinflow.gui.RiwayatPdfExporter;

/**
 * Pemeriksaan PDF: isi pendek, header berulang, nomor halaman, wrapping,
 * halaman banyak, render, dan kegagalan penulisan.
 */
public class PdfExportHarness {
    private static int gagal;

    public static void main(String[] args) throws Exception {
        Path pendek = Path.of("docs/bukti-gui/contoh-riwayat.pdf");
        Path banyak = Path.of("data/pdf-banyak.pdf");
        Path barisBesar = Path.of("data/pdf-baris-besar.pdf");
        Path readonly = Path.of("data/pdf-readonly.pdf");
        for (Path path : List.of(pendek, banyak, barisBesar, readonly)) {
            Files.deleteIfExists(path);
        }

        List<RiwayatCsvExporter.Baris> dua = List.of(
                new RiwayatCsvExporter.Baris("Roti", "bungkus",
                        LocalDate.of(2026, 11, 1), "Penerimaan",
                        "ROTI-A", 5, ""),
                new RiwayatCsvExporter.Baris("Roti", "bungkus",
                        LocalDate.of(2026, 11, 5), "Pengeluaran FEFO",
                        "ROTI-B", 3,
                        "Baris\nbaru Indonesia: kaña, café, —, kutip \"x\""));
        RiwayatPdfExporter.Info infoPendek = new RiwayatPdfExporter.Info(
                "Roti", "bungkus",
                LocalDateTime.of(2026, 11, 18, 23, 50),
                "ROTI", "Semua",
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 18), 2, dua);
        RiwayatPdfExporter.tulis(infoPendek, pendek);

        try (PDDocument dokumen = Loader.loadPDF(pendek.toFile())) {
            check("A4_LANDSCAPE",
                    Math.round(dokumen.getPage(0).getMediaBox().getWidth()) == 842
                    && Math.round(dokumen.getPage(0).getMediaBox().getHeight()) == 595,
                    "page");
            String teks = new PDFTextStripper().getText(dokumen);
            check("JUDUL", teks.contains("Laporan Riwayat Stok")
                    && teks.contains("KantinFlow"), "judul");
            check("BARANG", teks.contains("Roti (bungkus)"), "barang");
            check("FILTER", teks.contains("Pencarian: ROTI")
                    && teks.contains("Jenis: Semua")
                    && teks.contains("Tanggal mulai: 2026-11-01"),
                    "filter");
            check("RINGKASAN", teks.contains("Transaksi: 2")
                    && teks.contains("Rincian: 2"), "ringkasan");
            check("FEFO_PENJELASAN", teks.contains(
                    "Satu transaksi FEFO dapat memiliki beberapa rincian"),
                    "penjelasan");
            check("HEADER_TABEL", teks.contains("Kode batch")
                    && teks.contains("Jumlah") && teks.contains("Alasan"),
                    "header");
            check("NOMOR_HALAMAN", teks.contains("Halaman 1"), "halaman");
            check("BUKAN_SALDO", !teks.toLowerCase().contains("saldo akhir"),
                    "saldo");
            check("TEKS_INDONESIA",
                    teks.contains("kaña") && teks.contains("café")
                    && teks.contains("—") && teks.contains("Baris")
                    && teks.contains("baru"), "teks");
            renderSemua(dokumen, "pdf-pendek", 1);
        }

        List<RiwayatCsvExporter.Baris> banyakBaris = new ArrayList<>();
        for (int i = 1; i <= 80; i++) {
            banyakBaris.add(new RiwayatCsvExporter.Baris("Roti", "bungkus",
                    LocalDate.of(2026, 11, 1).plusDays(i % 20),
                    i % 2 == 0 ? "Penerimaan" : "Pengeluaran FEFO",
                    "BATCH-PANJANG-" + i, i,
                    i % 3 == 0
                            ? "Alasan panjang nomor " + i
                            + " dengan koma, tanda kutip \" dan baris\nbaru."
                            : ""));
        }
        RiwayatPdfExporter.Info infoBanyak = new RiwayatPdfExporter.Info(
                "Roti", "bungkus", LocalDateTime.now(), "", "Semua",
                null, null, 80, banyakBaris);
        RiwayatPdfExporter.tulis(infoBanyak, banyak);
        try (PDDocument dokumen = Loader.loadPDF(banyak.toFile())) {
            int halaman = dokumen.getNumberOfPages();
            check("BANYAK_HALAMAN", halaman > 1, "pages=" + halaman);
            String teks = new PDFTextStripper().getText(dokumen);
            check("HEADER_BERULANG",
                    jumlah(teks, "Kode batch") >= halaman,
                    "header=" + jumlah(teks, "Kode batch")
                    + " pages=" + halaman);
            boolean nomor = true;
            for (int i = 1; i <= halaman; i++) {
                nomor &= teks.contains("Halaman " + i);
            }
            check("NOMOR_SEMUA_HALAMAN", nomor, "pages=" + halaman);
            renderSemua(dokumen, "pdf-banyak", halaman);
        }

        StringBuilder besar = new StringBuilder();
        for (int i = 0; i < 80; i++) {
            besar.append("Baris panjang ").append(i)
                    .append(" teks Indonesia dan tanda kutip \"x\"\n");
        }
        List<RiwayatCsvExporter.Baris> satuBarisBesar = List.of(
                new RiwayatCsvExporter.Baris("Roti", "bungkus",
                        LocalDate.of(2026, 11, 5), "Pembuangan",
                        "ROTI-EXP", 1, besar.toString()));
        RiwayatPdfExporter.tulis(new RiwayatPdfExporter.Info(
                "Roti", "bungkus", LocalDateTime.now(), "", "Pembuangan",
                null, null, 1, satuBarisBesar), barisBesar);
        try (PDDocument dokumen = Loader.loadPDF(barisBesar.toFile())) {
            check("BARIS_BESAR_TIDAK_LOOP", dokumen.getNumberOfPages() >= 2,
                    "pages=" + dokumen.getNumberOfPages());
            String teks = new PDFTextStripper().getText(dokumen);
            check("BARIS_BESAR_LENGKAP",
                    teks.contains("Baris panjang 0")
                    && teks.contains("Baris panjang 79"),
                    "awal/akhir");
            renderSemua(dokumen, "pdf-baris-besar", dokumen.getNumberOfPages());
        }

        Files.writeString(readonly, "FILE-LAMA");
        Files.setAttribute(readonly, "dos:readonly", true);
        boolean gagalTulis = false;
        try {
            RiwayatPdfExporter.tulis(infoPendek, readonly);
        } catch (java.io.IOException exception) {
            gagalTulis = true;
        } finally {
            Files.setAttribute(readonly, "dos:readonly", false);
        }
        check("GAGAL_TULIS_TIDAK_MERUSAK",
                gagalTulis && "FILE-LAMA".equals(Files.readString(readonly)),
                "readonly");

        Files.deleteIfExists(banyak);
        Files.deleteIfExists(barisBesar);
        Files.deleteIfExists(readonly);
        System.out.println(gagal == 0
                ? "PDF_HARNESS_LULUS" : "PDF_HARNESS_GAGAL=" + gagal);
        System.exit(gagal == 0 ? 0 : 1);
    }

    private static void renderSemua(PDDocument dokumen, String nama,
            int halaman) throws Exception {
        PDFRenderer renderer = new PDFRenderer(dokumen);
        boolean semua = true;
        for (int i = 0; i < halaman; i++) {
            BufferedImage image = renderer.renderImageWithDPI(i, 90);
            semua &= image.getWidth() > 0 && image.getHeight() > 0;
            Path file = Path.of(System.getProperty("java.io.tmpdir"),
                    nama + "-" + (i + 1) + ".png");
            ImageIO.write(image, "png", file.toFile());
        }
        check("RENDER_" + nama, semua, "pages=" + halaman);
    }

    private static int jumlah(String teks, String bagian) {
        int total = 0;
        int posisi = 0;
        while ((posisi = teks.indexOf(bagian, posisi)) >= 0) {
            total++;
            posisi += bagian.length();
        }
        return total;
    }

    private static void check(String nama, boolean lulus, String detail) {
        System.out.println((lulus ? "LULUS " : "GAGAL ") + nama + "=" + detail);
        if (!lulus) {
            gagal++;
        }
    }
}
