package kantinflow;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Pembentuk kode batch internal: awalan nama barang, tanggal masuk, dan
 * nomor urut. Kode ini bukan nomor lot dari kemasan pabrik.
 */
public final class KodeBatch {
    private static final DateTimeFormatter TANGGAL =
            DateTimeFormatter.BASIC_ISO_DATE;

    private KodeBatch() {
    }

    public static String awalan(String namaBarang) {
        if (namaBarang == null) {
            return "BARANG";
        }
        String normal = Normalizer.normalize(namaBarang, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", " ")
                .trim();
        if (normal.isEmpty()) {
            return "BARANG";
        }
        String awalan = normal.split("\\s+")[0];
        return awalan.length() > 12 ? awalan.substring(0, 12) : awalan;
    }

    public static String format(String awalan, LocalDate tanggal, int urutan) {
        if (awalan == null || awalan.isBlank()) {
            throw new IllegalArgumentException("Awalan kode batch wajib diisi");
        }
        if (tanggal == null) {
            throw new IllegalArgumentException("Tanggal kode batch wajib diisi");
        }
        if (urutan < 1) {
            throw new IllegalArgumentException("Urutan kode batch minimal 1");
        }
        return awalan + "-" + tanggal.format(TANGGAL) + "-"
                + String.format(Locale.ROOT, "%02d", urutan);
    }
}
