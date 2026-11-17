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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;

import kantinflow.Barang;
import kantinflow.Transaksi;
import kantinflow.penyimpanan.PenyimpananSQLite;

public class FxFlowHarness extends Application {
    private static final Path DB = Paths.get("data", "fx-flow.db");
    private static final Path GAMBAR = Paths.get("docs", "bukti-gui");

    private PenyimpananSQLite penyimpanan;
    private ShellController shell;
    private Stage stage;
    private ShellController shell2;
    private Stage stage2;
    private final AtomicReference<Throwable> kegagalan = new AtomicReference<>();

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
            Barang susu = daftar.get(0);
            Barang roti = cari(daftar, "Roti");
            if (roti == null) {
                roti = penyimpanan.simpanBarang(new Barang("Roti", "bungkus"));
                daftar.add(roti);
            }
            penyimpanan.simpanStokMinimum(roti, 5);
            roti.setStokMinimum(5);

            FXMLLoader loader = new FXMLLoader(
                    FxFlowHarness.class.getResource("/kantinflow/fx/shell.fxml"));
            Parent root = loader.load();
            shell = loader.getController();
            shell.setData(penyimpanan, daftar, susu,
                    penyimpanan.muatRiwayat(susu));

            Scene scene = new Scene(root, 1180, 720);
            scene.getStylesheets().add(FxFlowHarness.class
                    .getResource("/kantinflow/fx/app.css").toExternalForm());
            stage.setTitle("KantinFlow - JavaFX");
            stage.setScene(scene);
            stage.show();

            Thread uji = new Thread(this::jalankanUji, "fx-flow-test");
            uji.setDaemon(true);
            uji.start();
        } catch (Exception exception) {
            kegagalan.set(exception);
            cetakKegagalan();
            Platform.exit();
        }
    }

    private void jalankanUji() {
        try {
            Thread.sleep(500);
            System.out.println("AWAL_AKTIF=" + fxGet(() -> shell.getBarangAktif().getNama()));

            fx(() -> {
                @SuppressWarnings("unchecked")
                ComboBox<Barang> combo = (ComboBox<Barang>) lookup("#barangAktif");
                for (Barang item : combo.getItems()) {
                    if (item.getNama().equals("Roti")) {
                        combo.setValue(item);
                        break;
                    }
                }
            });
            Thread.sleep(800);
            System.out.println("PINDAH_ROTI=" + fxGet(() -> shell.getBarangAktif().getNama()));

            fx(() -> ((ToggleButton) lookup("#navStokMasuk")).fire());
            Thread.sleep(300);
            fx(() -> {
                ((TextField) lookup("#kodeBatch")).setText("FX-A");
                ((TextField) lookup("#jumlah")).setText("5");
                ((DatePicker) lookup("#tanggalMasuk"))
                        .setValue(LocalDate.of(2026, 11, 18));
                ((DatePicker) lookup("#tanggalKedaluwarsa"))
                        .setValue(LocalDate.of(2026, 12, 20));
                ((Button) lookup("#simpanPenerimaan")).fire();
            });
            Thread.sleep(900);
            System.out.println("TERIMA_5=" + fxGet(() -> ((Label) lookup("#status")).getText()));
            System.out.println("TABEL_SETELAH_5=" + fxGet(this::barisBatch));

            fx(() -> ((ToggleButton) lookup("#navRingkasan")).fire());
            Thread.sleep(300);
            System.out.println("RINGKASAN_FISIK="
                    + fxGet(() -> ((Label) lookup("#ringkasanFisik")).getText()));
            System.out.println("RINGKASAN_LAYAK="
                    + fxGet(() -> ((Label) lookup("#ringkasanLayak")).getText()));
            System.out.println("RINGKASAN_STATUS="
                    + fxGet(() -> ((Label) lookup("#ringkasanStatus")).getText()));
            snapshot("gui-fx-ringkasan.png");

            fx(() -> ((ToggleButton) lookup("#navStokMasuk")).fire());
            Thread.sleep(300);
            snapshot("gui-fx-stok-masuk.png");

            try (Connection koneksi = DriverManager.getConnection("jdbc:sqlite:" + DB);
                    Statement statement = koneksi.createStatement()) {
                statement.executeUpdate(
                        "CREATE TRIGGER gagal_fx BEFORE INSERT ON transaksi "
                        + "BEGIN SELECT RAISE(ABORT, 'uji fx'); END");
            }
            fx(() -> {
                ((TextField) lookup("#kodeBatch")).setText("FX-FAIL");
                ((TextField) lookup("#jumlah")).setText("1");
                ((DatePicker) lookup("#tanggalMasuk"))
                        .setValue(LocalDate.of(2026, 11, 18));
                ((DatePicker) lookup("#tanggalKedaluwarsa"))
                        .setValue(LocalDate.of(2026, 12, 20));
                ((Button) lookup("#simpanPenerimaan")).fire();
            });
            Thread.sleep(900);
            System.out.println("PENYIMPANAN_GAGAL="
                    + fxGet(() -> ((Label) lookup("#status")).getText()));
            System.out.println("TABEL_SETELAH_GAGAL=" + fxGet(this::barisBatch));
            try (Connection koneksi = DriverManager.getConnection("jdbc:sqlite:" + DB);
                    Statement statement = koneksi.createStatement()) {
                statement.executeUpdate("DROP TRIGGER gagal_fx");
            }

            fx(() -> {
                ((TextField) lookup("#jumlah")).setText("0");
                ((Button) lookup("#simpanPenerimaan")).fire();
            });
            Thread.sleep(700);
            System.out.println("JUMLAH_0=" + fxGet(() -> ((Label) lookup("#status")).getText()));
            System.out.println("TABEL_SETELAH_0=" + fxGet(this::barisBatch));

            fx(() -> {
                ((TextField) lookup("#jumlah")).setText("2");
                ((TextField) lookup("#kodeBatch")).setText("FX-A");
                ((DatePicker) lookup("#tanggalMasuk"))
                        .setValue(LocalDate.of(2026, 11, 18));
                ((DatePicker) lookup("#tanggalKedaluwarsa"))
                        .setValue(LocalDate.of(2026, 12, 20));
                ((Button) lookup("#simpanPenerimaan")).fire();
            });
            Thread.sleep(700);
            System.out.println("DUPLIKAT=" + fxGet(() -> ((Label) lookup("#status")).getText()));
            System.out.println("TABEL_SETELAH_DUPLIKAT=" + fxGet(this::barisBatch));

            fx(() -> {
                @SuppressWarnings("unchecked")
                ComboBox<Barang> combo = (ComboBox<Barang>) lookup("#barangAktif");
                for (Barang item : combo.getItems()) {
                    if (item.getNama().equals("Susu Kotak")) {
                        combo.setValue(item);
                        break;
                    }
                }
            });
            Thread.sleep(700);
            System.out.println("SUSU_AKTIF=" + fxGet(() -> shell.getBarangAktif().getNama())
                    + ";BATCH=" + fxGet(this::barisBatch));
            fx(() -> {
                @SuppressWarnings("unchecked")
                ComboBox<Barang> combo = (ComboBox<Barang>) lookup("#barangAktif");
                for (Barang item : combo.getItems()) {
                    if (item.getNama().equals("Roti")) {
                        combo.setValue(item);
                        break;
                    }
                }
            });
            Thread.sleep(700);
            System.out.println("ROTI_AKTIF=" + fxGet(() -> shell.getBarangAktif().getNama())
                    + ";BATCH=" + fxGet(this::barisBatch));

            fx(() -> {
                shell.close();
                stage.close();
            });
            Thread.sleep(300);

            fx(() -> {
                try {
                    PenyimpananSQLite kedua = new PenyimpananSQLite(DB.toString());
                    List<Barang> daftar2 = kedua.muatSemuaBarang();
                    Barang roti2 = cari(daftar2, "Roti");
                    FXMLLoader loader = new FXMLLoader(
                            FxFlowHarness.class.getResource("/kantinflow/fx/shell.fxml"));
                    Parent root = loader.load();
                    shell2 = loader.getController();
                    shell2.setData(kedua, daftar2, roti2, kedua.muatRiwayat(roti2));
                    Scene scene2 = new Scene(root, 1180, 720);
                    scene2.getStylesheets().add(FxFlowHarness.class
                            .getResource("/kantinflow/fx/app.css").toExternalForm());
                    stage2 = new Stage();
                    stage2.setTitle("KantinFlow - JavaFX");
                    stage2.setScene(scene2);
                    stage2.show();
                } catch (IOException exception) {
                    throw new IllegalStateException(exception);
                }
            });
            Thread.sleep(600);
            fx(() -> ((ToggleButton) lookup(stage2, "#navStokMasuk")).fire());
            Thread.sleep(300);
            System.out.println("REOPEN_ROTI_BATCH="
                    + fxGet(() -> ((TableView<?>) lookup(stage2, "#tabelBatch"))
                            .getItems().size()));
            System.out.println("REOPEN_STATUS="
                    + fxGet(() -> ((Label) lookup(stage2, "#status")).getText()));
            fx(() -> {
                shell2.close();
                stage2.close();
            });
            System.out.println("FX_FLOW_LULUS");
        } catch (Throwable exception) {
            kegagalan.set(exception);
            cetakKegagalan();
        } finally {
            Platform.runLater(Platform::exit);
        }
    }

    private int barisBatch() {
        return ((TableView<?>) lookup("#tabelBatch")).getItems().size();
    }

    private void snapshot(String nama) throws Exception {
        fx(() -> {
            try {
                WritableImage image = stage.getScene().snapshot(null);
                BufferedImage buffered = SwingFXUtils.fromFXImage(image, null);
                ImageIO.write(buffered, "png",
                        GAMBAR.resolve(nama).toFile());
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
        if (!latch.await(10, TimeUnit.SECONDS)) {
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

    private void cetakKegagalan() {
        Throwable error = kegagalan.get();
        if (error != null) {
            System.out.println("FX_FLOW_GAGAL=" + error);
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
