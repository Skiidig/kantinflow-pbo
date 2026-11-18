import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import kantinflow.gui.RiwayatCsvExporter;

/**
 * Pemeriksaan kontrak file CSV: BOM, CRLF, header, escaping, formula, dan
 * perilaku file tujuan saat validasi gagal.
 */
public class CsvExportHarness {
    private static int gagal;

    public static void main(String[] args) throws Exception {
        Path target = Path.of("data/csv-helper-test.csv");
        Path failureTarget = Path.of("data/csv-helper-failure.csv");
        Files.deleteIfExists(target);
        Files.deleteIfExists(failureTarget);

        List<RiwayatCsvExporter.Baris> rows = List.of(
                new RiwayatCsvExporter.Baris("Roti, Manis", "bungkus",
                        LocalDate.of(2026, 11, 5), "Pengeluaran FEFO",
                        "A\"1", 5, "Baris\nbaru"),
                new RiwayatCsvExporter.Baris("Roti", "bungkus",
                        LocalDate.of(2026, 11, 5), "Pengeluaran FEFO",
                        "B-2", 3, ""),
                new RiwayatCsvExporter.Baris("=SUM(A1:A2)", "@satuan",
                        LocalDate.of(2026, 11, 6), "+Jenis",
                        "-batch", 1, "@rumus"));

        RiwayatCsvExporter.tulis(rows, target);
        byte[] bytes = Files.readAllBytes(target);
        check("BOM_UTF8", bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xEF
                && (bytes[1] & 0xFF) == 0xBB
                && (bytes[2] & 0xFF) == 0xBF, "3 byte awal");
        String text = new String(bytes, StandardCharsets.UTF_8);
        check("HEADER_EKSAK", text.startsWith(
                "\uFEFFBarang,Satuan,Tanggal transaksi,Jenis transaksi,"
                + "Kode batch,Jumlah,Alasan\r\n"), "header");
        check("CRLF", text.contains("\r\n") && !text.contains("\n\n"),
                "line ending");
        check("ESCAPE_KOMA", text.contains("\"Roti, Manis\""), "koma");
        check("ESCAPE_KUTIP", text.contains("\"A\"\"1\""), "kutip");
        check("ESCAPE_BARIS_BARU", text.contains("\"Baris\nbaru\""),
                "newline");
        check("FORMULA_DINETRALKAN",
                text.contains("'=SUM(A1:A2)")
                && text.contains("'@satuan")
                && text.contains("'+Jenis")
                && text.contains("'-batch")
                && text.contains("'@rumus"), "apostrof");
        check("ALASAN_KOSONG", text.contains(",3,\r\n"), "sel kosong");
        check("TANPA_METADATA", text.indexOf("Barang,Satuan")
                == "\uFEFF".length(), "header awal");

        Files.writeString(failureTarget, "JANGAN-DIHAPUS",
                StandardCharsets.UTF_8);
        boolean gagalValidasi = false;
        try {
            RiwayatCsvExporter.tulis(List.of(), failureTarget);
        } catch (IllegalArgumentException exception) {
            gagalValidasi = true;
        }
        check("GAGAL_VALIDASI", gagalValidasi, "exception");
        check("FILE_LAMA_TETAP", "JANGAN-DIHAPUS".equals(
                Files.readString(failureTarget)), "isi lama");

        Path gagalTulis = Path.of("data/csv-helper-readonly.csv");
        Files.deleteIfExists(gagalTulis);
        Files.writeString(gagalTulis, "FILE-LAMA", StandardCharsets.UTF_8);
        Files.setAttribute(gagalTulis, "dos:readonly", true);
        boolean gagalMenulis = false;
        try {
            RiwayatCsvExporter.tulis(rows, gagalTulis);
        } catch (java.io.IOException exception) {
            gagalMenulis = true;
        } finally {
            Files.setAttribute(gagalTulis, "dos:readonly", false);
        }
        check("GAGAL_TULIS_TIDAK_MERUSAK",
                gagalMenulis && "FILE-LAMA".equals(
                        Files.readString(gagalTulis)),
                "readonly");
        Files.deleteIfExists(gagalTulis);

        Files.deleteIfExists(target);
        Files.deleteIfExists(failureTarget);
        System.out.println(gagal == 0
                ? "CSV_HARNESS_LULUS" : "CSV_HARNESS_GAGAL=" + gagal);
        System.exit(gagal == 0 ? 0 : 1);
    }

    private static void check(String nama, boolean lulus, String detail) {
        System.out.println((lulus ? "LULUS " : "GAGAL ") + nama + "=" + detail);
        if (!lulus) {
            gagal++;
        }
    }
}
