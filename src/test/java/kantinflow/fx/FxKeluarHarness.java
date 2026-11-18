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
import javafx.scene.control.TextField;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;

import kantinflow.Barang;
import kantinflow.BatchStok;
import kantinflow.Transaksi;
import kantinflow.penyimpanan.PenyimpananSQLite;

public class FxKeluarHarness extends Application {
    private static final Path DB = Paths.get("data", "fx-keluar.db");
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
            Barang susu = daftar.get(0);
            Barang roti = cari(daftar, "Roti");
            if (roti == null) {
                roti = penyimpanan.simpanBarang(new Barang("Roti", "bungkus"));
                daftar.add(roti);
            }
            bukaShell(stage, penyimpanan, daftar, susu);
            Thread uji = new Thread(this::jalankanUji, "fx-keluar-test");
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
                FxKeluarHarness.class.getResource("/kantinflow/fx/shell.fxml"));
        Parent root = loader.load();
        ShellController controller = loader.getController();
        controller.setData(repo, daftar, aktif, repo.muatRiwayat(aktif));
        Scene scene = new Scene(root, 1180, 720);
        scene.getStylesheets().add(FxKeluarHarness.class
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

            // Susu: terima 5, manual 2, lalu manual 4 ditolak.
            nav("#navStokMasuk");
            terima("SUSU-A", "5", "2026-11-18", "2026-12-20");
            Thread.sleep(800);
            nav("#navStokKeluar");
            Thread.sleep(300);
            pilihBatch("SUSU-A");
            isi("#jumlahKeluar", "2");
            klik("#tombolKeluarkan");
            Thread.sleep(800);
            System.out.println("SUSU_MANUAL_2=" + status());
            System.out.println("SUSU_SISA_2=" + sisaAktif("SUSU-A"));
            snapshot("gui-fx-stok-keluar-manual.png");
            isi("#jumlahKeluar", "4");
            klik("#tombolKeluarkan");
            Thread.sleep(700);
            System.out.println("SUSU_MANUAL_4=" + status());
            System.out.println("SUSU_SISA_4=" + sisaAktif("SUSU-A"));

            // Roti: dua batch A=5, B=7.
            pilihBarang("Roti");
            Thread.sleep(700);
            nav("#navStokMasuk");
            terima("ROTI-A", "5", "2026-11-18", "2026-12-20");
            terima("ROTI-B", "7", "2026-11-18", "2026-12-25");
            Thread.sleep(500);

            nav("#navStokKeluar");
            Thread.sleep(300);
            fx(() -> ((TabPane) lookup("#modeTabs"))
                    .getSelectionModel().select(1));
            Thread.sleep(200);
            // Tanggal sebelum batch masuk harus ditolak.
            isi("#jumlahFefo", "1");
            isiTanggal("#tanggalFefo", "2026-11-17");
            klik("#tombolFefo");
            Thread.sleep(700);
            System.out.println("SEBELUM_MASUK=" + status());

            isi("#jumlahFefo", "8");
            isiTanggal("#tanggalFefo", "2026-11-18");
            klik("#tombolFefo");
            Thread.sleep(900);
            System.out.println("FEFO_8=" + status());
            System.out.println("FEFO_8_A=" + sisaAktif("ROTI-A"));
            System.out.println("FEFO_8_B=" + sisaAktif("ROTI-B"));
            System.out.println("FEFO_8_RINCIAN=" + rincianTransaksiTerakhir());
            System.out.println("ALOKASI_8=" + alokasi());
            snapshot("gui-fx-stok-keluar-berhasil.png");

            isi("#jumlahFefo", "5");
            isiTanggal("#tanggalFefo", "2026-11-18");
            klik("#tombolFefo");
            Thread.sleep(700);
            System.out.println("FEFO_5=" + status());
            System.out.println("FEFO_5_B=" + sisaAktif("ROTI-B"));
            System.out.println("FEFO_5_RIWAYAT=" + riwayatAktif());
            snapshot("gui-fx-stok-keluar-ditolak.png");

            // Batch kedaluwarsa harus dilewati FEFO.
            nav("#navStokMasuk");
            terima("ROTI-EXP", "2", "2026-10-01", "2026-11-10");
            Thread.sleep(400);
            nav("#navStokKeluar");
            Thread.sleep(300);
            isi("#jumlahFefo", "4");
            isiTanggal("#tanggalFefo", "2026-11-18");
            klik("#tombolFefo");
            Thread.sleep(800);
            System.out.println("FEFO_EXPIRED=" + status());
            System.out.println("EXPIRED_B=" + sisaAktif("ROTI-B"));
            System.out.println("EXPIRED_EXP=" + sisaAktif("ROTI-EXP"));
            System.out.println("ALOKASI_EXPIRED=" + alokasi());

            // Pindah barang harus membersihkan pilihan/result sebelumnya.
            pilihBatch("ROTI-EXP");
            pilihBarang("Susu Kotak");
            Thread.sleep(700);
            System.out.println("SWITCH_SUSU_BATCH=" + batchTerpilih()
                    + ";ALOKASI=" + alokasi());
            pilihBarang("Roti");
            Thread.sleep(700);
            System.out.println("SWITCH_ROTI_BATCH=" + batchTerpilih()
                    + ";ALOKASI=" + alokasi());

            // Trigger gagal simpan, lalu pastikan rollback dan reload.
            try (Connection koneksi = DriverManager.getConnection("jdbc:sqlite:" + DB);
                    Statement statement = koneksi.createStatement()) {
                statement.executeUpdate(
                        "CREATE TRIGGER gagal_keluar BEFORE INSERT ON transaksi "
                        + "BEGIN SELECT RAISE(ABORT, 'uji keluar'); END");
            }
            pilihBatch("ROTI-EXP");
            isi("#jumlahKeluar", "1");
            klik("#tombolKeluarkan");
            Thread.sleep(900);
            System.out.println("GAGAL_SIMPAN=" + status());
            System.out.println("GAGAL_EXP=" + sisaAktif("ROTI-EXP"));
            try (Connection koneksi = DriverManager.getConnection("jdbc:sqlite:" + DB);
                    Statement statement = koneksi.createStatement()) {
                statement.executeUpdate("DROP TRIGGER gagal_keluar");
            }

            ShellController shellAwal = shell;
            Stage stageAwal = stage;
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
            nav("#navStokMasuk");
            Thread.sleep(300);
            System.out.println("REOPEN_AKTIF=" + fxGet(
                    () -> shell2.getBarangAktif().getNama()));
            System.out.println("REOPEN_EXP=" + sisaAktif("ROTI-EXP"));
            System.out.println("REOPEN_RIWAYAT=" + fxGet(() -> shell2.getRiwayat().size()));
            fx(() -> {
                shell.close();
                stage.close();
                shellAwal.close();
                stageAwal.close();
            });
            System.out.println("FX_KELUAR_LULUS");
        } catch (Throwable exception) {
            gagal.set(exception);
            cetakGagal();
        } finally {
            Platform.runLater(Platform::exit);
        }
    }

    private void nav(String id) throws Exception {
        fx(() -> ((ToggleButton) lookup(id)).fire());
        Thread.sleep(300);
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

    private void pilihBatch(String kode) throws Exception {
        fx(() -> {
            @SuppressWarnings("unchecked")
            ComboBox<BatchStok> combo = (ComboBox<BatchStok>) lookup("#batchManual");
            for (BatchStok batch : combo.getItems()) {
                if (batch.getKode().equals(kode)) {
                    combo.setValue(batch);
                    return;
                }
            }
            throw new IllegalArgumentException("Batch tidak ada: " + kode);
        });
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

    private void isiTanggal(String id, String nilai) throws Exception {
        fx(() -> ((DatePicker) lookup(id)).setValue(LocalDate.parse(nilai)));
    }

    private void klik(String id) throws Exception {
        fx(() -> ((Button) lookup(id)).fire());
    }

    private String status() throws Exception {
        return fxGet(() -> ((Label) lookup("#status")).getText());
    }

    private String alokasi() throws Exception {
        return fxGet(() -> ((Label) lookup("#hasilAlokasi")).getText());
    }

    private String batchTerpilih() throws Exception {
        return fxGet(() -> {
            BatchStok batch = ((ComboBox<?>) lookup("#batchManual")).getValue() == null
                    ? null : (BatchStok) ((ComboBox<?>) lookup("#batchManual")).getValue();
            return batch == null ? "-" : batch.getKode();
        });
    }

    private int sisaAktif(String kode) throws Exception {
        return fxGet(() -> {
            for (BatchStok batch : shell.getBarangAktif().getDaftarBatch()) {
                if (batch.getKode().equals(kode)) {
                    return batch.getSisaJumlah();
                }
            }
            return -1;
        });
    }

    private int rincianTransaksiTerakhir() throws Exception {
        return fxGet(() -> {
            List<Transaksi> riwayat = shell.getRiwayat();
            return riwayat.isEmpty() ? -1
                    : riwayat.get(riwayat.size() - 1).getRincian().size();
        });
    }

    private int riwayatAktif() throws Exception {
        return fxGet(() -> shell.getRiwayat().size());
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
            System.out.println("FX_KELUAR_GAGAL=" + error);
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
