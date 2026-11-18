package kantinflow.gui;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.List;

/**
 * Penulisan CSV Riwayat. Semua aturan CSV, encoding, dan penulisan atomik
 * berada di kelas ini agar PanelRiwayat hanya menangani snapshot dan dialog.
 */
public final class RiwayatCsvExporter {
    private static final String HEADER = "Barang,Satuan,Tanggal transaksi,"
            + "Jenis transaksi,Kode batch,Jumlah,Alasan";
    private static final String FORMULA_AWAL = "=+-@";

    private RiwayatCsvExporter() {
    }

    public record Baris(
            String barang,
            String satuan,
            LocalDate tanggal,
            String jenis,
            String kodeBatch,
            int jumlah,
            String alasan) {
    }

    public static void tulis(List<Baris> baris, Path tujuan) throws IOException {
        if (tujuan == null) {
            throw new IllegalArgumentException("Tujuan ekspor wajib diisi");
        }
        if (baris == null || baris.isEmpty()) {
            throw new IllegalArgumentException("Tidak ada rincian untuk diekspor");
        }

        Path fileTujuan = tujuan.toAbsolutePath().normalize();
        Path direktori = fileTujuan.getParent();
        if (direktori == null) {
            throw new IOException("Folder tujuan tidak valid");
        }
        Files.createDirectories(direktori);

        Path sementara = Files.createTempFile(
                direktori, ".kantinflow-riwayat-", ".tmp");
        boolean berhasil = false;
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(
                    sementara, StandardCharsets.UTF_8,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                writer.write('\uFEFF');
                writer.write(HEADER);
                writer.write("\r\n");
                for (Baris item : baris) {
                    writer.write(sel(item.barang()));
                    writer.write(',');
                    writer.write(sel(item.satuan()));
                    writer.write(',');
                    writer.write(item.tanggal().toString());
                    writer.write(',');
                    writer.write(sel(item.jenis()));
                    writer.write(',');
                    writer.write(sel(item.kodeBatch()));
                    writer.write(',');
                    writer.write(Integer.toString(item.jumlah()));
                    writer.write(',');
                    writer.write(sel(item.alasan()));
                    writer.write("\r\n");
                }
            }
            pindahkan(sementara, fileTujuan);
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

    /**
     * Teks selalu di-escape bila mengandung koma, kutip, atau baris baru.
     * Nilai teks yang berpotensi dieksekusi spreadsheet diberi apostrof
     * sebagai awalan setelah whitespace awal.
     */
    static String sel(String nilai) {
        String teks = nilai == null ? "" : nilai;
        if (diawaliFormula(teks)) {
            teks = "'" + teks;
        }
        return escape(teks);
    }

    private static boolean diawaliFormula(String nilai) {
        int indeks = 0;
        while (indeks < nilai.length()
                && Character.isWhitespace(nilai.charAt(indeks))) {
            indeks++;
        }
        return indeks < nilai.length()
                && FORMULA_AWAL.indexOf(nilai.charAt(indeks)) >= 0;
    }

    private static String escape(String nilai) {
        boolean perluKutip = nilai.indexOf(',') >= 0
                || nilai.indexOf('"') >= 0
                || nilai.indexOf('\n') >= 0
                || nilai.indexOf('\r') >= 0;
        if (!perluKutip) {
            return nilai;
        }
        return '"' + nilai.replace("\"", "\"\"") + '"';
    }
}
