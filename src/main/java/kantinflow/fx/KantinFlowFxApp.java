package kantinflow.fx;

import java.util.List;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import kantinflow.Barang;
import kantinflow.Transaksi;
import kantinflow.penyimpanan.PenyimpananSQLite;

public class KantinFlowFxApp extends Application {
    private ShellController shell;

    @Override
    public void start(Stage stage) {
        stage.setTitle("KantinFlow - JavaFX");
        StackPane loading = new StackPane(new Label("Memuat KantinFlow..."));
        loading.setAlignment(Pos.CENTER);
        loading.getStyleClass().add("loading-root");
        stage.setScene(new Scene(loading, 1180, 720));
        stage.show();

        Task<DataAwal> task = new Task<>() {
            @Override
            protected DataAwal call() {
                PenyimpananSQLite penyimpanan = new PenyimpananSQLite(lokasiDatabase());
                List<Barang> daftar = penyimpanan.muatSemuaBarang();
                if (daftar.isEmpty()) {
                    penyimpanan.close();
                    throw new IllegalStateException("Database belum memiliki barang");
                }
                Barang aktif = daftar.get(0);
                List<Transaksi> riwayat = penyimpanan.muatRiwayat(aktif);
                return new DataAwal(penyimpanan, daftar, aktif, riwayat);
            }
        };
        task.setOnSucceeded(event -> {
            try {
                bukaShell(stage, task.getValue());
            } catch (RuntimeException exception) {
                task.getValue().penyimpanan.close();
                tampilkanError("Gagal membuka shell JavaFX: " + exception.getMessage());
            }
        });
        task.setOnFailed(event -> {
            Throwable error = task.getException();
            tampilkanError("Gagal membuka database: "
                    + (error == null ? "kesalahan tidak diketahui" : error.getMessage()));
        });
        Thread thread = new Thread(task, "kantinflow-fx-start");
        thread.setDaemon(true);
        thread.start();
    }

    private void bukaShell(Stage stage, DataAwal data) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    KantinFlowFxApp.class.getResource("/kantinflow/fx/shell.fxml"));
            Parent root = loader.load();
            shell = loader.getController();
            shell.setData(data.penyimpanan, data.daftarBarang,
                    data.barangAktif, data.riwayat);

            Scene scene = new Scene(root, 1180, 720);
            scene.getStylesheets().add(
                    KantinFlowFxApp.class.getResource("/kantinflow/fx/app.css")
                            .toExternalForm());
            stage.setScene(scene);
            stage.setMinWidth(960);
            stage.setMinHeight(600);
            stage.centerOnScreen();
        } catch (java.io.IOException exception) {
            data.penyimpanan.close();
            tampilkanError("Gagal memuat FXML: " + exception.getMessage());
        }
    }

    private void tampilkanError(String pesan) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("KantinFlow");
        alert.setHeaderText(null);
        alert.setContentText(pesan);
        alert.showAndWait();
    }

    private String lokasiDatabase() {
        String lokasi = System.getenv("KANTINFLOW_DB");
        if (lokasi == null || lokasi.isBlank()) {
            return "data/kantinflow.db";
        }
        return lokasi;
    }

    @Override
    public void stop() {
        if (shell != null) {
            shell.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    private static final class DataAwal {
        private final PenyimpananSQLite penyimpanan;
        private final List<Barang> daftarBarang;
        private final Barang barangAktif;
        private final List<Transaksi> riwayat;

        private DataAwal(PenyimpananSQLite penyimpanan, List<Barang> daftarBarang,
                Barang barangAktif, List<Transaksi> riwayat) {
            this.penyimpanan = penyimpanan;
            this.daftarBarang = daftarBarang;
            this.barangAktif = barangAktif;
            this.riwayat = riwayat;
        }
    }
}
