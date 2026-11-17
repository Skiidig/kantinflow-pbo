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
 * Akses SQLite untuk banyak barang. Seluruh akses JDBC berada di kelas ini,
 * terpisah dari MenuKonsol dan aturan domain. Setiap transaksi eksplisit
 * menyimpan barang_id dari barang aktif.
 */
public class PenyimpananSQLite implements AutoCloseable {
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

    /**
     * Skema lama sudah memiliki barang_id pada batch dan transaksi. Tidak ada
     * perubahan tabel yang diperlukan; seed hanya dibuat jika belum ada barang.
     */
    private void siapkanSkema() throws SQLException {
        try (Statement statement = koneksi.createStatement()) {
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS barang ("
                    + "id INTEGER PRIMARY KEY, "
                    + "nama TEXT NOT NULL, "
                    + "satuan TEXT NOT NULL, "
                    + "stok_minimum INTEGER NOT NULL DEFAULT 0 "
                    + "CHECK (stok_minimum >= 0))");

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
                    + "WHERE NOT EXISTS (SELECT 1 FROM barang)");
        }
        migrasiStokMinimum();
    }

    /**
     * Database versi sebelumnya belum memiliki stok_minimum. Penambahan
     * kolom dilakukan dalam satu transaksi; nilai lama otomatis 0.
     */
    private void migrasiStokMinimum() throws SQLException {
        if (punyaKolomStokMinimum()) {
            return;
        }

        boolean autoCommit = koneksi.getAutoCommit();
        koneksi.setAutoCommit(false);
        try (Statement statement = koneksi.createStatement()) {
            statement.executeUpdate(
                    "ALTER TABLE barang ADD COLUMN stok_minimum "
                    + "INTEGER NOT NULL DEFAULT 0 CHECK (stok_minimum >= 0)");
            koneksi.commit();
        } catch (SQLException exception) {
            rollback();
            throw exception;
        } finally {
            try {
                koneksi.setAutoCommit(autoCommit);
            } catch (SQLException ignored) {
                // Kegagalan utama tetap dilempar.
            }
        }
    }

    private boolean punyaKolomStokMinimum() throws SQLException {
        try (Statement statement = koneksi.createStatement();
                ResultSet result = statement.executeQuery(
                        "PRAGMA table_info(barang)")) {
            while (result.next()) {
                if ("stok_minimum".equals(result.getString("name"))) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Memuat seluruh barang beserta batch-nya. Data lama dengan id=1 ikut
     * terambil tanpa penyalinan.
     */
    public List<Barang> muatSemuaBarang() {
        List<Barang> daftar = new ArrayList<>();
        try (Statement statement = koneksi.createStatement();
                ResultSet result = statement.executeQuery(
                        "SELECT id FROM barang ORDER BY id")) {
            while (result.next()) {
                daftar.add(muatBarang(result.getInt("id")));
            }
            return daftar;
        } catch (SQLException exception) {
            throw new PenyimpananException("Gagal memuat daftar barang", exception);
        }
    }

    /**
     * Kompatibilitas untuk pemanggil lama: barang pertama pada database.
     */
    public Barang muatBarang() {
        List<Barang> daftar = muatSemuaBarang();
        if (daftar.isEmpty()) {
            throw new PenyimpananException("Data barang tidak ditemukan di database");
        }
        return daftar.get(0);
    }

    public Barang muatBarang(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID barang tidak valid");
        }
        try {
            String nama;
            String satuan;
            int stokMinimum;
            try (PreparedStatement statement = koneksi.prepareStatement(
                    "SELECT nama, satuan, stok_minimum FROM barang WHERE id = ?")) {
                statement.setInt(1, id);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        throw new PenyimpananException(
                                "Data barang tidak ditemukan: " + id);
                    }
                    nama = result.getString("nama");
                    satuan = result.getString("satuan");
                    stokMinimum = result.getInt("stok_minimum");
                }
            }

            Barang barang = new Barang(id, nama, satuan, stokMinimum);
            try (PreparedStatement statement = koneksi.prepareStatement(
                    "SELECT kode, tanggal_masuk, tanggal_kedaluwarsa, sisa_jumlah "
                    + "FROM batch WHERE barang_id = ? ORDER BY id")) {
                statement.setInt(1, id);
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        BatchStok batch = new BatchStok(
                                result.getString("kode"),
                                LocalDate.parse(result.getString("tanggal_masuk")),
                                LocalDate.parse(result.getString("tanggal_kedaluwarsa")),
                                result.getInt("sisa_jumlah"));
                        barang.tambahBatch(batch);
                    }
                }
            }
            return barang;
        } catch (SQLException exception) {
            throw new PenyimpananException("Gagal memuat barang dari database", exception);
        }
    }

    /**
     * Menyimpan barang baru dan mengembalikan objek dengan id database.
     */
    public Barang simpanBarang(Barang barang) {
        if (barang == null) {
            throw new IllegalArgumentException("Barang wajib diisi");
        }
        if (barang.getId() != 0) {
            throw new IllegalArgumentException("Barang yang sudah tersimpan tidak dapat ditambahkan lagi");
        }

        try {
            try (PreparedStatement statement = koneksi.prepareStatement(
                    "SELECT id FROM barang WHERE lower(nama) = lower(?)")) {
                statement.setString(1, barang.getNama());
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        throw new PenyimpananException(
                                "Nama barang sudah terdaftar: " + barang.getNama());
                    }
                }
            }

            boolean autoCommit = koneksi.getAutoCommit();
            koneksi.setAutoCommit(false);
            try (PreparedStatement statement = koneksi.prepareStatement(
                    "INSERT INTO barang(nama, satuan, stok_minimum) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, barang.getNama());
                statement.setString(2, barang.getSatuan());
                statement.setInt(3, barang.getStokMinimum());
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("ID barang baru tidak diperoleh");
                    }
                    int id = keys.getInt(1);
                    koneksi.commit();
                    return new Barang(
                            id, barang.getNama(), barang.getSatuan(),
                            barang.getStokMinimum());
                }
            } catch (SQLException | RuntimeException exception) {
                rollback();
                throw new PenyimpananException("Gagal menyimpan barang", exception);
            } finally {
                try {
                    koneksi.setAutoCommit(autoCommit);
                } catch (SQLException ignored) {
                    // Kegagalan utama sudah dilaporkan.
                }
            }
        } catch (SQLException exception) {
            throw new PenyimpananException("Gagal memulai transaksi database", exception);
        }
    }

    public void simpanStokMinimum(Barang barang, int stokMinimum) {
        if (barang == null || barang.getId() <= 0) {
            throw new IllegalArgumentException("Barang aktif tidak valid");
        }
        if (stokMinimum < 0) {
            throw new IllegalArgumentException("Stok minimum tidak boleh negatif");
        }
        try (PreparedStatement statement = koneksi.prepareStatement(
                "UPDATE barang SET stok_minimum = ? WHERE id = ?")) {
            statement.setInt(1, stokMinimum);
            statement.setInt(2, barang.getId());
            if (statement.executeUpdate() != 1) {
                throw new PenyimpananException(
                        "Barang tidak ditemukan saat menyimpan stok minimum");
            }
        } catch (SQLException exception) {
            throw new PenyimpananException(
                    "Gagal menyimpan stok minimum", exception);
        }
    }

    public List<Transaksi> muatRiwayat(Barang barang) {
        if (barang == null || barang.getId() <= 0) {
            throw new IllegalArgumentException("Barang aktif tidak valid");
        }
        try {
            Map<String, BatchStok> batchBerdasarkanKode = new HashMap<>();
            for (BatchStok batch : barang.getDaftarBatch()) {
                batchBerdasarkanKode.put(batch.getKode(), batch);
            }

            Map<Integer, BatchStok> batchBerdasarkanId = new HashMap<>();
            try (PreparedStatement statement = koneksi.prepareStatement(
                    "SELECT id, kode FROM batch WHERE barang_id = ?")) {
                statement.setInt(1, barang.getId());
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        BatchStok batch = batchBerdasarkanKode.get(result.getString("kode"));
                        if (batch == null) {
                            throw new PenyimpananException("Batch rujukan tidak ditemukan: "
                                    + result.getString("kode"));
                        }
                        batchBerdasarkanId.put(result.getInt("id"), batch);
                    }
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
                    + "WHERE t.barang_id = ? "
                    + "ORDER BY t.id, r.id";
            try (PreparedStatement statement = koneksi.prepareStatement(sql)) {
                statement.setInt(1, barang.getId());
                try (ResultSet result = statement.executeQuery()) {
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

    /**
     * Menyimpan transaksi untuk barang aktif. Kepemilikan batch diperiksa
     * kembali di lapisan penyimpanan sebelum transaksi SQL dimulai.
     */
    public void simpanTransaksi(Barang barang, Transaksi transaksi) {
        if (barang == null || barang.getId() <= 0) {
            throw new IllegalArgumentException("Barang aktif tidak valid");
        }
        if (transaksi == null) {
            throw new IllegalArgumentException("Transaksi wajib diisi");
        }
        if (transaksi.getRincian().isEmpty()) {
            throw new IllegalArgumentException("Transaksi tanpa rincian tidak dapat disimpan");
        }
        for (RincianTransaksi rincian : transaksi.getRincian()) {
            if (!barang.memilikiBatch(rincian.getBatch())) {
                throw new PenyimpananException(
                        "Batch " + rincian.getBatch().getKode()
                        + " bukan milik barang " + barang.getNama());
            }
            if (transaksi.getTanggal().isBefore(
                    rincian.getBatch().getTanggalMasuk())) {
                throw new PenyimpananException(
                        "Tanggal transaksi tidak boleh sebelum tanggal masuk batch "
                        + rincian.getBatch().getKode());
            }
        }

        try {
            boolean autoCommit = koneksi.getAutoCommit();
            koneksi.setAutoCommit(false);
            try {
                Map<String, Integer> idBatch = new HashMap<>();
                for (RincianTransaksi rincian : transaksi.getRincian()) {
                    idBatch.put(
                            rincian.getBatch().getKode(),
                            sinkronBatch(barang, rincian.getBatch()));
                }

                int idTransaksi = insertTransaksi(barang, transaksi);
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

    private int sinkronBatch(Barang barang, BatchStok batch) throws SQLException {
        Integer id = cariIdBatch(barang, batch.getKode());
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
            statement.setInt(1, barang.getId());
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

    private Integer cariIdBatch(Barang barang, String kode) throws SQLException {
        try (PreparedStatement statement = koneksi.prepareStatement(
                "SELECT id FROM batch WHERE barang_id = ? AND kode = ?")) {
            statement.setInt(1, barang.getId());
            statement.setString(2, kode);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getInt("id");
                }
                return null;
            }
        }
    }

    private int insertTransaksi(Barang barang, Transaksi transaksi) throws SQLException {
        try (PreparedStatement statement = koneksi.prepareStatement(
                "INSERT INTO transaksi(barang_id, jenis, tanggal, alasan) "
                + "VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, barang.getId());
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
