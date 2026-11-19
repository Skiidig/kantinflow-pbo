import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Menghapus dataset bertanda "contoh" beserta transaksi dan rinciannya
 * dalam satu transaksi database. Data lain tidak disentuh.
 */
public final class BersihkanDataContoh {
    private BersihkanDataContoh() {
    }

    public static void main(String[] args) throws Exception {
        Path lokasi = Paths.get(args.length > 0
                ? args[0] : "data/kantinflow.db");
        Class.forName("org.sqlite.JDBC");
        try (Connection koneksi = DriverManager.getConnection(
                "jdbc:sqlite:" + lokasi.toAbsolutePath())) {
            try (Statement perintah = koneksi.createStatement()) {
                perintah.execute("PRAGMA foreign_keys = ON");
            }
            boolean autoCommit = koneksi.getAutoCommit();
            koneksi.setAutoCommit(false);
            try {
                int barangMarker = hitung(koneksi,
                        "SELECT count(*) FROM barang "
                        + "WHERE lower(nama) LIKE '%contoh%'");
                int teksMarker = hitung(koneksi,
                        "SELECT count(*) FROM transaksi "
                        + "WHERE lower(coalesce(jenis,'')) LIKE '%contoh%' "
                        + "OR lower(coalesce(alasan,'')) LIKE '%contoh%'");
                if (barangMarker > 0 || teksMarker > 0) {
                    throw new IllegalStateException(
                            "Penanda contoh pada nama barang/teks transaksi "
                            + "tidak dapat dibersihkan otomatis tanpa "
                            + "berisiko menghapus data lain.");
                }

                List<Integer> batchMarker = idBatchMarker(koneksi);
                if (batchMarker.isEmpty()) {
                    koneksi.rollback();
                    System.out.println("[LEWATI] Tidak ada batch contoh.");
                    return;
                }
                List<Integer> transaksi = idTransaksiTerkait(
                        koneksi, batchMarker);
                int campuran = hitung(koneksi,
                        "SELECT count(*) FROM ("
                        + "SELECT r.transaksi_id FROM rincian_transaksi r "
                        + "JOIN batch b ON b.id = r.batch_id "
                        + "GROUP BY r.transaksi_id "
                        + "HAVING SUM(CASE WHEN lower(b.kode) LIKE "
                        + "'%contoh%' THEN 1 ELSE 0 END) > 0 "
                        + "AND SUM(CASE WHEN lower(b.kode) NOT LIKE "
                        + "'%contoh%' THEN 1 ELSE 0 END) > 0)");
                if (campuran > 0) {
                    throw new IllegalStateException(
                            "Ada " + campuran + " transaksi FEFO campuran; "
                            + "pembersihan dihentikan agar rincian valid "
                            + "tidak terhapus sebagian.");
                }

                System.out.println("[INVENTARIS] batch_contoh="
                        + batchMarker.size() + " transaksi_terkait="
                        + transaksi.size());
                hapusRincian(koneksi, transaksi);
                hapusTransaksi(koneksi, transaksi);
                hapusBatch(koneksi, batchMarker);
                koneksi.commit();
                System.out.println("[OK] Dihapus: batch="
                        + batchMarker.size() + " transaksi="
                        + transaksi.size());
            } catch (Exception exception) {
                koneksi.rollback();
                throw exception;
            } finally {
                koneksi.setAutoCommit(autoCommit);
            }

            try (Statement perintah = koneksi.createStatement()) {
                int sisa = hitung(koneksi,
                        "SELECT count(*) FROM batch "
                        + "WHERE lower(kode) LIKE '%contoh%'");
                System.out.println("[VERIFIKASI] batch_contoh_tersisa=" + sisa);
                try (ResultSet hasil = perintah.executeQuery(
                        "PRAGMA foreign_key_check")) {
                    int masalah = 0;
                    while (hasil.next()) {
                        masalah++;
                    }
                    System.out.println("[VERIFIKASI] foreign_key_masalah="
                            + masalah);
                }
            }
        }
    }

    private static int hitung(Connection koneksi, String sql)
            throws Exception {
        try (Statement perintah = koneksi.createStatement();
                ResultSet hasil = perintah.executeQuery(sql)) {
            hasil.next();
            return hasil.getInt(1);
        }
    }

    private static List<Integer> idBatchMarker(Connection koneksi)
            throws Exception {
        List<Integer> hasil = new ArrayList<>();
        try (Statement perintah = koneksi.createStatement();
                ResultSet baris = perintah.executeQuery(
                        "SELECT id FROM batch "
                        + "WHERE lower(kode) LIKE '%contoh%' ORDER BY id")) {
            while (baris.next()) {
                hasil.add(baris.getInt(1));
            }
        }
        return hasil;
    }

    private static List<Integer> idTransaksiTerkait(Connection koneksi,
            List<Integer> batchMarker) throws Exception {
        List<Integer> hasil = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT DISTINCT transaksi_id FROM rincian_transaksi "
                + "WHERE batch_id IN (");
        for (int i = 0; i < batchMarker.size(); i++) {
            sql.append(i == 0 ? "?" : ",?");
        }
        sql.append(") ORDER BY transaksi_id");
        try (PreparedStatement perintah = koneksi.prepareStatement(
                sql.toString())) {
            for (int i = 0; i < batchMarker.size(); i++) {
                perintah.setInt(i + 1, batchMarker.get(i));
            }
            try (ResultSet baris = perintah.executeQuery()) {
                while (baris.next()) {
                    hasil.add(baris.getInt(1));
                }
            }
        }
        return hasil;
    }

    private static void hapusRincian(Connection koneksi,
            List<Integer> transaksi) throws Exception {
        try (PreparedStatement perintah = koneksi.prepareStatement(
                "DELETE FROM rincian_transaksi WHERE transaksi_id = ?")) {
            for (int id : transaksi) {
                perintah.setInt(1, id);
                perintah.executeUpdate();
            }
        }
    }

    private static void hapusTransaksi(Connection koneksi,
            List<Integer> transaksi) throws Exception {
        try (PreparedStatement perintah = koneksi.prepareStatement(
                "DELETE FROM transaksi WHERE id = ?")) {
            for (int id : transaksi) {
                perintah.setInt(1, id);
                perintah.executeUpdate();
            }
        }
    }

    private static void hapusBatch(Connection koneksi,
            List<Integer> batch) throws Exception {
        try (PreparedStatement perintah = koneksi.prepareStatement(
                "DELETE FROM batch WHERE id = ?")) {
            for (int id : batch) {
                perintah.setInt(1, id);
                perintah.executeUpdate();
            }
        }
    }
}
