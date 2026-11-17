package kantinflow.penyimpanan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kantinflow.Barang;
import kantinflow.BatchStok;
import kantinflow.PembuanganStok;
import kantinflow.PenerimaanStok;
import kantinflow.PengeluaranStok;
import kantinflow.RincianTransaksi;
import kantinflow.Transaksi;

/**
 * Akses SQLite untuk satu barang. Seluruh akses JDBC berada di kelas ini,
 * terpisah dari MenuKonsol dan aturan domain.
 */
public class PenyimpananSQLite implements AutoCloseable {
    private static final int BARANG_ID = 1;
    private final Connection koneksi;

    public PenyimpananSQLite(String lokasiDatabase) {
        if (lokasiDatabase == null || lokasiDatabase.isBlank()) {
            throw new IllegalArgumentException("Lokasi database wajib diisi");
        }

        try {
            if (!":memory:".equals(lokasiDatabase)) {
                Path file = Paths.get(lokasiDatabase).toAbsolutePath();
                Path direktori = file.getParent();
                if (direktori != null) {
                    Files.createDirectories(direktori);
                }
            }
            Class.forName("org.sqlite.JDBC");
            this.koneksi = DriverManager.getConnection("jdbc:sqlite:" + lokasiDatabase);
            try (Statement statement = koneksi.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
            }
            siapkanSkema();
        } catch (ClassNotFoundException exception) {
            throw new PenyimpananException(
                    "Driver SQLite JDBC tidak ditemukan. Tambahkan sqlite-jdbc ke classpath.",
                    exception);
        } catch (IOException | SQLException exception) {
            throw new PenyimpananException(
                    "Gagal membuka database: " + lokasiDatabase,
                    exception);
        }
    }

    private void siapkanSkema() throws SQLException {
        try (Statement statement = koneksi.createStatement()) {
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS barang ("
                    + "id INTEGER PRIMARY KEY, "
                    + "nama TEXT NOT NULL, "
                    + "satuan TEXT NOT NULL)");

            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS batch ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "barang_id INTEGER NOT NULL, "
                    + "kode TEXT NOT NULL, "
                    + "tanggal_masuk TEXT NOT NULL, "
                    + "tanggal_kedaluwarsa TEXT NOT NULL, "
                    + "sisa_jumlah INTEGER NOT NULL CHECK (sisa_jumlah >= 0), "
                    + "UNIQUE (barang_id, kode), "
                    + "FOREIGN KEY (barang_id) REFERENCES barang(id))");

            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS transaksi ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "barang_id INTEGER NOT NULL, "
                    + "jenis TEXT NOT NULL, "
                    + "tanggal TEXT NOT NULL, "
                    + "alasan TEXT, "
                    + "FOREIGN KEY (barang_id) REFERENCES barang(id))");

            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS rincian_transaksi ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "transaksi_id INTEGER NOT NULL, "
                    + "batch_id INTEGER NOT NULL, "
                    + "jumlah INTEGER NOT NULL CHECK (jumlah > 0), "
                    + "UNIQUE (transaksi_id, batch_id), "
                    + "FOREIGN KEY (transaksi_id) REFERENCES transaksi(id) "
                    + "ON DELETE CASCADE, "
                    + "FOREIGN KEY (batch_id) REFERENCES batch(id))");

            statement.executeUpdate(
                    "INSERT INTO barang(id, nama, satuan) "
                    + "SELECT 1, 'Susu Kotak', 'kotak' "
                    + "WHERE NOT EXISTS (SELECT 1 FROM barang WHERE id = 1)");
        }
    }

    public Barang muatBarang() {
        try {
            String nama;
            String satuan;
            try (Statement statement = koneksi.createStatement();
                    ResultSet result = statement.executeQuery(
                            "SELECT nama, satuan FROM barang WHERE id = " + BARANG_ID)) {
                if (!result.next()) {
                    throw new PenyimpananException("Data barang tidak ditemukan di database");
                }
                nama = result.getString("nama");
                satuan = result.getString("satuan");
            }

            Barang barang = new Barang(nama, satuan);
            try (Statement statement = koneksi.createStatement();
                    ResultSet result = statement.executeQuery(
                            "SELECT kode, tanggal_masuk, tanggal_kedaluwarsa, sisa_jumlah "
                            + "FROM batch WHERE barang_id = " + BARANG_ID + " ORDER BY id")) {
                while (result.next()) {
                    BatchStok batch = new BatchStok(
                            result.getString("kode"),
                            LocalDate.parse(result.getString("tanggal_masuk")),
                            LocalDate.parse(result.getString("tanggal_kedaluwarsa")),
                            result.getInt("sisa_jumlah"));
                    barang.tambahBatch(batch);
                }
            }
            return barang;
        } catch (SQLException exception) {
            throw new PenyimpananException("Gagal memuat barang dari database", exception);
        }
    }

    public List<Transaksi> muatRiwayat(Barang barang) {
        try {
            Map<String, BatchStok> batchBerdasarkanKode = new HashMap<>();
            for (BatchStok batch : barang.getDaftarBatch()) {
                batchBerdasarkanKode.put(batch.getKode(), batch);
            }

            Map<Integer, BatchStok> batchBerdasarkanId = new HashMap<>();
            try (Statement statement = koneksi.createStatement();
                    ResultSet result = statement.executeQuery(
                            "SELECT id, kode FROM batch WHERE barang_id = " + BARANG_ID)) {
                while (result.next()) {
                    BatchStok batch = batchBerdasarkanKode.get(result.getString("kode"));
                    if (batch == null) {
                        throw new PenyimpananException("Batch rujukan tidak ditemukan: "
                                + result.getString("kode"));
                    }
                    batchBerdasarkanId.put(result.getInt("id"), batch);
                }
            }

            List<Transaksi> riwayat = new ArrayList<>();
            int idTransaksi = -1;
            String jenis = null;
            LocalDate tanggal = null;
            String alasan = null;
            List<RincianTransaksi> rincian = new ArrayList<>();

            String sql = "SELECT t.id, t.jenis, t.tanggal, t.alasan, "
                    + "r.batch_id, r.jumlah "
                    + "FROM transaksi t "
                    + "JOIN rincian_transaksi r ON r.transaksi_id = t.id "
                    + "WHERE t.barang_id = " + BARANG_ID + " "
                    + "ORDER BY t.id, r.id";
            try (Statement statement = koneksi.createStatement();
                    ResultSet result = statement.executeQuery(sql)) {
                while (result.next()) {
                    int id = result.getInt("id");
                    if (id != idTransaksi) {
                        if (idTransaksi != -1) {
                            riwayat.add(buatTransaksi(
                                    idTransaksi, jenis, tanggal, alasan, rincian));
                        }
                        idTransaksi = id;
                        jenis = result.getString("jenis");
                        tanggal = LocalDate.parse(result.getString("tanggal"));
                        alasan = result.getString("alasan");
                        rincian = new ArrayList<>();
                    }
                    BatchStok batch = batchBerdasarkanId.get(result.getInt("batch_id"));
                    if (batch == null) {
                        throw new PenyimpananException("Batch rincian tidak ditemukan");
                    }
                    rincian.add(new RincianTransaksi(batch, result.getInt("jumlah")));
                }
            }
            if (idTransaksi != -1) {
                riwayat.add(buatTransaksi(idTransaksi, jenis, tanggal, alasan, rincian));
            }
            return riwayat;
        } catch (SQLException exception) {
            throw new PenyimpananException("Gagal memuat riwayat dari database", exception);
        }
    }

    private Transaksi buatTransaksi(
            int id,
            String jenis,
            LocalDate tanggal,
            String alasan,
            List<RincianTransaksi> rincian) {
        if ("Penerimaan".equals(jenis)) {
            if (rincian.size() != 1) {
                throw new PenyimpananException("Rincian penerimaan tidak valid: " + id);
            }
            RincianTransaksi detail = rincian.get(0);
            return PenerimaanStok.dariPenyimpanan(
                    tanggal, detail.getBatch(), detail.getJumlah());
        }
        if ("Pengeluaran".equals(jenis) || "Pengeluaran FEFO".equals(jenis)) {
            return PengeluaranStok.dariPenyimpanan(
                    tanggal, List.copyOf(rincian), "Pengeluaran FEFO".equals(jenis));
        }
        if ("Pembuangan".equals(jenis)) {
            if (rincian.size() != 1) {
                throw new PenyimpananException("Rincian pembuangan tidak valid: " + id);
            }
            RincianTransaksi detail = rincian.get(0);
            return PembuanganStok.dariPenyimpanan(
                    tanggal, detail.getBatch(), detail.getJumlah(), alasan);
        }
        throw new PenyimpananException("Jenis transaksi tidak dikenal: " + jenis);
    }

    public void simpanTransaksi(Transaksi transaksi) {
        if (transaksi == null) {
            throw new IllegalArgumentException("Transaksi wajib diisi");
        }
        if (transaksi.getRincian().isEmpty()) {
            throw new IllegalArgumentException("Transaksi tanpa rincian tidak dapat disimpan");
        }

        try {
            boolean autoCommit = koneksi.getAutoCommit();
            koneksi.setAutoCommit(false);
            try {
                Map<String, Integer> idBatch = new HashMap<>();
                for (RincianTransaksi rincian : transaksi.getRincian()) {
                    idBatch.put(
                            rincian.getBatch().getKode(),
                            sinkronBatch(rincian.getBatch()));
                }

                int idTransaksi = insertTransaksi(transaksi);
                for (RincianTransaksi rincian : transaksi.getRincian()) {
                    insertRincian(
                            idTransaksi,
                            idBatch.get(rincian.getBatch().getKode()),
                            rincian.getJumlah());
                }
                koneksi.commit();
            } catch (SQLException | RuntimeException exception) {
                rollback();
                throw new PenyimpananException("Gagal menyimpan transaksi", exception);
            } finally {
                try {
                    koneksi.setAutoCommit(autoCommit);
                } catch (SQLException ignored) {
                    // Koneksi tetap dapat ditutup; kegagalan utama sudah dilaporkan.
                }
            }
        } catch (SQLException exception) {
            throw new PenyimpananException("Gagal memulai transaksi database", exception);
        }
    }

    private int sinkronBatch(BatchStok batch) throws SQLException {
        Integer id = cariIdBatch(batch.getKode());
        if (id != null) {
            try (PreparedStatement statement = koneksi.prepareStatement(
                    "UPDATE batch SET tanggal_masuk = ?, tanggal_kedaluwarsa = ?, "
                    + "sisa_jumlah = ? WHERE id = ?")) {
                statement.setString(1, batch.getTanggalMasuk().toString());
                statement.setString(2, batch.getTanggalKedaluwarsa().toString());
                statement.setInt(3, batch.getSisaJumlah());
                statement.setInt(4, id);
                statement.executeUpdate();
            }
            return id;
        }

        try (PreparedStatement statement = koneksi.prepareStatement(
                "INSERT INTO batch(barang_id, kode, tanggal_masuk, "
                + "tanggal_kedaluwarsa, sisa_jumlah) VALUES (?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, BARANG_ID);
            statement.setString(2, batch.getKode());
            statement.setString(3, batch.getTanggalMasuk().toString());
            statement.setString(4, batch.getTanggalKedaluwarsa().toString());
            statement.setInt(5, batch.getSisaJumlah());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("ID batch baru tidak diperoleh");
    }

    private Integer cariIdBatch(String kode) throws SQLException {
        try (PreparedStatement statement = koneksi.prepareStatement(
                "SELECT id FROM batch WHERE barang_id = ? AND kode = ?")) {
            statement.setInt(1, BARANG_ID);
            statement.setString(2, kode);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getInt("id");
                }
                return null;
            }
        }
    }

    private int insertTransaksi(Transaksi transaksi) throws SQLException {
        try (PreparedStatement statement = koneksi.prepareStatement(
                "INSERT INTO transaksi(barang_id, jenis, tanggal, alasan) "
                + "VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, BARANG_ID);
            statement.setString(2, transaksi.getJenis());
            statement.setString(3, transaksi.getTanggal().toString());
            String alasan = transaksi.getAlasan();
            if (alasan == null || alasan.isBlank()) {
                statement.setNull(4, Types.VARCHAR);
            } else {
                statement.setString(4, alasan);
            }
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("ID transaksi baru tidak diperoleh");
    }

    private void insertRincian(int idTransaksi, int idBatch, int jumlah)
            throws SQLException {
        try (PreparedStatement statement = koneksi.prepareStatement(
                "INSERT INTO rincian_transaksi(transaksi_id, batch_id, jumlah) "
                + "VALUES (?, ?, ?)")) {
            statement.setInt(1, idTransaksi);
            statement.setInt(2, idBatch);
            statement.setInt(3, jumlah);
            statement.executeUpdate();
        }
    }

    private void rollback() {
        try {
            koneksi.rollback();
        } catch (SQLException ignored) {
            // Rollback terbaik yang dapat dilakukan; exception utama tetap dilempar.
        }
    }

    @Override
    public void close() {
        try {
            koneksi.close();
        } catch (SQLException exception) {
            throw new PenyimpananException("Gagal menutup database", exception);
        }
    }
}
