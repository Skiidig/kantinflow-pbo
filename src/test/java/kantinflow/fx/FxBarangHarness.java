package kantinflow.fx;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import javax.imageio.ImageIO;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;

import kantinflow.Barang;
import kantinflow.penyimpanan.PenyimpananSQLite;

public class FxBarangHarness extends Application {
    private static final Path DB = Paths.get("data", "fx-barang.db");
    private static final Path GAMBAR = Paths.get("docs", "bukti-gui");

    private PenyimpananSQLite penyimpanan;
    private ShellController shell;
    private Stage stage;
    private Stage stage2;
    private ShellController shell2;
    private final AtomicReference<Throwable> gagal = new AtomicReference<>();

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        Platform.setImplicitExit(false);
        try {
            Files.createDirectories(DB.getParent());
            Files.createDirectories(GAMBAR);
            Files.deleteIfExists(DB);
            penyimpanan = new PenyimpananSQLite(DB.toString());
            List<Barang> daftar = penyimpanan.muatSemuaBarang();
            bukaShell(stage, penyimpanan, daftar, daftar.get(0));
            Thread uji = new Thread(this::jalankanUji, "fx-barang-test");
            uji.setDaemon(true);
            uji.start();
        } catch (Exception exception) {
            gagal.set(exception);
            cetakGagal();
            Platform.exit();
        }
    }

    private void bukaShell(Stage target, PenyimpananSQLite repo,
            List<Barang> daftar, Barang aktif) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                FxBarangHarness.class.getResource("/kantinflow/fx/shell.fxml"));
        Parent root = loader.load();
        ShellController controller = loader.getController();
        controller.setData(repo, daftar, aktif, repo.muatRiwayat(aktif));
        Scene scene = new Scene(root, 1180, 720);
        scene.getStylesheets().add(FxBarangHarness.class
                .getResource("/kantinflow/fx/app.css").toExternalForm());
        target.setTitle("KantinFlow - JavaFX");
        target.setScene(scene);
        target.show();
        if (shell == null) {
            shell = controller;
            this.stage = target;
        } else {
            shell2 = controller;
            this.stage2 = target;
        }
    }

    private void jalankanUji() {
        try {
            Thread.sleep(500);
            System.out.println("AWAL=" + fxGet(() -> shell.getBarangAktif().getNama()));
            nav("#navBarang");

            // Validasi lokal dekat form.
            isi("#namaBarangBaru", "   ");
            isi("#satuanBarangBaru", "bungkus");
            isi("#minimumBarangBaru", "0");
            klik("#tombolTambahBarang");
            Thread.sleep(150);
            System.out.println("NAMA_KOSONG=" + teks("#errorTambahBarang"));
            isi("#namaBarangBaru", "Roti");
            isi("#satuanBarangBaru", "   ");
            klik("#tombolTambahBarang");
            Thread.sleep(150);
            System.out.println("SATUAN_KOSONG=" + teks("#errorTambahBarang"));
            isi("#satuanBarangBaru", "bungkus");
            isi("#minimumBarangBaru", "-1");
            klik("#tombolTambahBarang");
            Thread.sleep(150);
            System.out.println("MIN_NEGATIF=" + teks("#errorTambahBarang"));
            isi("#minimumBarangBaru", "abc");
            klik("#tombolTambahBarang");
            Thread.sleep(150);
            System.out.println("MIN_BUKAN_ANGKA=" + teks("#errorTambahBarang"));
            System.out.println("JUMLAH_SETELAH_INVALID=" + jumlahBarang());

            // Tambah Roti bungkus minimum 5.
            isi("#namaBarangBaru", "  Roti  ");
            isi("#satuanBarangBaru", "  bungkus  ");
            isi("#minimumBarangBaru", "5");
            klik("#tombolTambahBarang");
            Thread.sleep(800);
            System.out.println("TAMBAH_ROTI=" + status());
            System.out.println("JUMLAH_BARANG=" + jumlahBarang());
            System.out.println("AKTIF_SETELAH_TAMBAH=" + fxGet(
                    () -> shell.getBarangAktif().getNama()));
            System.out.println("MINIMUM_ROTI=" + fxGet(
                    () -> shell.getBarangAktif().getStokMinimum()));
            System.out.println("SATUAN_ROTI=" + fxGet(
                    () -> shell.getBarangAktif().getSatuan()));
            snapshot("gui-fx-barang.png");

            // Beri stok Roti 5, lalu ubah minimum dan cek Ringkasan.
            nav("#navStokMasuk");
            terima("ROTI-A", "5", "2026-11-18", "2026-12-20");
            Thread.sleep(500);
            nav("#navBarang");
            isi("#minimumBarangAktif", " 6 ");
            klik("#tombolSimpanMinimum");
            Thread.sleep(700);
            System.out.println("MINIMUM_6=" + status());
            nav("#navRingkasan");
            Thread.sleep(300);
            System.out.println("RINGKASAN_MIN_6=" + teks("#ringkasanStatus")
                    + ";MIN=" + teks("#ringkasanMinimum"));
            snapshot("gui-fx-ringkasan.png");

            nav("#navBarang");
            isi("#minimumBarangAktif", " 0 ");
            klik("#tombolSimpanMinimum");
            Thread.sleep(700);
            nav("#navRingkasan");
            Thread.sleep(300);
            System.out.println("RINGKASAN_MIN_0=" + teks("#ringkasanStatus"));

            // Halaman Stok Keluar baru, lalu kembali.
            nav("#navStokKeluar");
            Thread.sleep(300);
            System.out.println("HALAMAN_KELUAR=" + fxGet(
                    () -> stage.getScene().lookup("#batchManual") != null));
            snapshot("gui-fx-stok-keluar.png");
            fx(() -> {
                stage.setWidth(960);
                stage.setHeight(640);
            });
            Thread.sleep(300);
            System.out.println("UKURAN_960=" + fxGet(
                    () -> (int) stage.getScene().getWidth() + "x"
                            + (int) stage.getScene().getHeight()));
            fx(() -> {
                stage.setWidth(1180);
                stage.setHeight(720);
            });
            Thread.sleep(300);
            nav("#navBarang");

            // Pengiriman ganda: klik dua kali, hanya satu barang ditambahkan.
            isi("#namaBarangBaru", "Ganda");
            isi("#satuanBarangBaru", "pcs");
            isi("#minimumBarangBaru", "1");
            fx(() -> {
                ((Button) lookup("#tombolTambahBarang")).fire();
                ((Button) lookup("#tombolTambahBarang")).fire();
            });
            Thread.sleep(800);
            System.out.println("GANDA_JUMLAH=" + jumlahBarang());
            pilihBarang("Roti");
            Thread.sleep(600);

            // Trigger gagal update minimum.
            try (Connection koneksi = DriverManager.getConnection("jdbc:sqlite:" + DB);
                    Statement statement = koneksi.createStatement()) {
                statement.executeUpdate(
                        "CREATE TRIGGER gagal_min BEFORE UPDATE ON barang "
                        + "BEGIN SELECT RAISE(ABORT, 'uji minimum'); END");
            }
            nav("#navBarang");
            isi("#minimumBarangAktif", "7");
            klik("#tombolSimpanMinimum");
            Thread.sleep(800);
            System.out.println("GAGAL_MIN=" + status());
            System.out.println("MIN_SETELAH_GAGAL=" + fxGet(
                    () -> shell.getBarangAktif().getStokMinimum()));
            System.out.println("JUMLAH_SETELAH_MIN_GAGAL=" + jumlahBarang());
            try (Connection koneksi = DriverManager.getConnection("jdbc:sqlite:" + DB);
                    Statement statement = koneksi.createStatement()) {
                statement.executeUpdate("DROP TRIGGER gagal_min");
            }

            // Trigger gagal insert.
            try (Connection koneksi = DriverManager.getConnection("jdbc:sqlite:" + DB);
                    Statement statement = koneksi.createStatement()) {
                statement.executeUpdate(
                        "CREATE TRIGGER gagal_tambah BEFORE INSERT ON barang "
                        + "BEGIN SELECT RAISE(ABORT, 'uji tambah'); END");
            }
            isi("#namaBarangBaru", "Gagal");
            isi("#satuanBarangBaru", "pcs");
            isi("#minimumBarangBaru", "0");
            klik("#tombolTambahBarang");
            Thread.sleep(800);
            System.out.println("GAGAL_TAMBAH=" + status());
            System.out.println("JUMLAH_SETELAH_GAGAL=" + jumlahBarang());
            try (Connection koneksi = DriverManager.getConnection("jdbc:sqlite:" + DB);
                    Statement statement = koneksi.createStatement()) {
                statement.executeUpdate("DROP TRIGGER gagal_tambah");
            }

            // Pindah barang: sidebar dan tabel harus sinkron.
            pilihBarang("Susu Kotak");
            Thread.sleep(700);
            System.out.println("PINDAH_SUSU=" + fxGet(
                    () -> shell.getBarangAktif().getNama())
                    + ";BARIS=" + barisTerpilih());
            pilihBarang("Roti");
            Thread.sleep(700);
            System.out.println("PINDAH_ROTI=" + fxGet(
                    () -> shell.getBarangAktif().getNama())
                    + ";BARIS=" + barisTerpilih());

            // Reopen dari repository baru; stage lama tetap hidup sampai selesai.
            ShellController shellAwal = shell;
            Stage stageAwal = stage;
            fx(() -> shellAwal.close());
            Thread.sleep(250);
            fx(() -> {
                try {
                    PenyimpananSQLite kedua = new PenyimpananSQLite(DB.toString());
                    List<Barang> daftar2 = kedua.muatSemuaBarang();
                    Barang roti2 = cari(daftar2, "Roti");
                    bukaShell(new Stage(), kedua, daftar2, roti2);
                } catch (IOException exception) {
                    throw new IllegalStateException(exception);
                }
            });
            Thread.sleep(700);
            stage = stage2;
            shell = shell2;
            nav("#navBarang");
            System.out.println("REOPEN_AKTIF=" + fxGet(
                    () -> shell.getBarangAktif().getNama()));
            System.out.println("REOPEN_MIN=" + fxGet(
                    () -> shell.getBarangAktif().getStokMinimum()));
            System.out.println("REOPEN_JUMLAH=" + jumlahBarang());
            fx(() -> {
                shell.close();
                stage.close();
                stageAwal.close();
            });
            System.out.println("FX_BARANG_LULUS");
        } catch (Throwable exception) {
            gagal.set(exception);
            cetakGagal();
        } finally {
            Platform.runLater(Platform::exit);
        }
    }

    private void terima(String kode, String jumlah, String masuk, String keluar)
            throws Exception {
        fx(() -> {
            ((TextField) lookup("#kodeBatch")).setText(kode);
            ((TextField) lookup("#jumlah")).setText(jumlah);
            ((DatePicker) lookup("#tanggalMasuk"))
                    .setValue(LocalDate.parse(masuk));
            ((DatePicker) lookup("#tanggalKedaluwarsa"))
                    .setValue(LocalDate.parse(keluar));
            ((Button) lookup("#simpanPenerimaan")).fire();
        });
        Thread.sleep(500);
    }

    private void nav(String id) throws Exception {
        fx(() -> ((ToggleButton) lookup(id)).fire());
        Thread.sleep(300);
    }

    private void pilihBarang(String nama) throws Exception {
        fx(() -> {
            @SuppressWarnings("unchecked")
            ComboBox<Barang> combo = (ComboBox<Barang>) lookup("#barangAktif");
            for (Barang item : combo.getItems()) {
                if (item.getNama().equals(nama)) {
                    combo.setValue(item);
                    return;
                }
            }
            throw new IllegalArgumentException("Barang tidak ada: " + nama);
        });
    }

    private void isi(String id, String nilai) throws Exception {
        fx(() -> ((TextField) lookup(id)).setText(nilai));
    }

    private void klik(String id) throws Exception {
        fx(() -> ((Button) lookup(id)).fire());
    }

    private String status() throws Exception {
        return fxGet(() -> ((Label) lookup("#status")).getText());
    }

    private String teks(String id) throws Exception {
        return fxGet(() -> ((Label) lookup(id)).getText());
    }

    private int jumlahBarang() throws Exception {
        return fxGet(() -> {
            @SuppressWarnings("unchecked")
            TableView<Barang> tabel = (TableView<Barang>) lookup("#tabelBarang");
            return tabel.getItems().size();
        });
    }

    private String barisTerpilih() throws Exception {
        return fxGet(() -> {
            @SuppressWarnings("unchecked")
            TableView<Barang> tabel = (TableView<Barang>) lookup("#tabelBarang");
            Barang item = tabel.getSelectionModel().getSelectedItem();
            return item == null ? "-" : item.getNama();
        });
    }

    private void snapshot(String nama) throws Exception {
        fx(() -> {
            try {
                WritableImage image = stage.getScene().snapshot(null);
                BufferedImage buffered = SwingFXUtils.fromFXImage(image, null);
                ImageIO.write(buffered, "png", GAMBAR.resolve(nama).toFile());
                System.out.println("SCREENSHOT=" + GAMBAR.resolve(nama).toAbsolutePath());
            } catch (IOException exception) {
                throw new IllegalStateException(exception);
            }
        });
    }

    private javafx.scene.Node lookup(String id) {
        return lookup(stage, id);
    }

    private javafx.scene.Node lookup(Stage target, String id) {
        javafx.scene.Node node = target.getScene().lookup(id);
        if (node == null) {
            throw new IllegalArgumentException("Node tidak ditemukan: " + id);
        }
        return node;
    }

    private void fx(Runnable aksi) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                aksi.run();
            } catch (Throwable throwable) {
                error.set(throwable);
            } finally {
                latch.countDown();
            }
        });
        if (!latch.await(15, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Aksi FX timeout");
        }
        if (error.get() != null) {
            throw new IllegalStateException(error.get());
        }
    }

    private <T> T fxGet(Supplier<T> supplier) throws Exception {
        AtomicReference<T> hasil = new AtomicReference<>();
        fx(() -> hasil.set(supplier.get()));
        return hasil.get();
    }

    private void cetakGagal() {
        Throwable error = gagal.get();
        if (error != null) {
            System.out.println("FX_BARANG_GAGAL=" + error);
            error.printStackTrace(System.out);
        }
    }

    private static Barang cari(List<Barang> daftar, String nama) {
        for (Barang barang : daftar) {
            if (barang.getNama().equals(nama)) {
                return barang;
            }
        }
        return null;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
