package kantinflow.fx;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.util.StringConverter;

import kantinflow.Barang;
import kantinflow.PenerimaanStok;
import kantinflow.Transaksi;
import kantinflow.penyimpanan.PenyimpananException;
import kantinflow.penyimpanan.PenyimpananSQLite;

/**
 * Shell JavaFX tahap pertama: sidebar, pemilih barang, dua halaman, status.
 * Semua akses SQLite dijalankan di executor latar.
 */
public class ShellController implements KonteksFx, AutoCloseable {
    @FXML
    private BorderPane root;
    @FXML
    private ComboBox<Barang> barangCombo;
    @FXML
    private StackPane halamanContainer;
    @FXML
    private Label statusLabel;
    @FXML
    private ToggleButton navRingkasan;
    @FXML
    private ToggleButton navStokMasuk;

    private final ExecutorService io = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "kantinflow-fx-io");
        thread.setDaemon(true);
        return thread;
    });
    private final ToggleGroup grupNav = new ToggleGroup();

    private PenyimpananSQLite penyimpanan;
    private List<Barang> daftarBarang;
    private Barang barang;
    private List<Transaksi> riwayat;
    private java.time.LocalDate tanggalPemantauan;
    private HalamanFx halamanRingkasan;
    private HalamanFx halamanStokMasuk;
    private HalamanFx halamanAktif;
    private boolean menggantiBarang;
    private boolean sibuk;

    @FXML
    private void initialize() {
        navRingkasan.setToggleGroup(grupNav);
        navStokMasuk.setToggleGroup(grupNav);
        grupNav.selectedToggleProperty().addListener((obs, lama, baru) -> {
            if (baru == null) {
                if (lama != null) {
                    lama.setSelected(true);
                }
            }
        });

        barangCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Barang item) {
                return item == null ? "" : item.getNama() + " (" + item.getSatuan() + ")";
            }

            @Override
            public Barang fromString(String teks) {
                return null;
            }
        });
        barangCombo.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Barang item, boolean kosong) {
                super.updateItem(item, kosong);
                setText(kosong || item == null ? null
                        : item.getNama() + " (" + item.getSatuan() + ")");
            }
        });
        barangCombo.valueProperty().addListener((obs, lama, baru) -> {
            if (!menggantiBarang && baru != null && barang != null
                    && baru.getId() != barang.getId()) {
                gantiBarangAsync(baru);
            }
        });

        halamanRingkasan = muatHalaman("/kantinflow/fx/ringkasan.fxml");
        halamanStokMasuk = muatHalaman("/kantinflow/fx/stok-masuk.fxml");
        halamanRingkasan.setKonteks(this);
        halamanStokMasuk.setKonteks(this);
    }

    public void setData(PenyimpananSQLite penyimpanan, List<Barang> daftarBarang,
            Barang barang, List<Transaksi> riwayat) {
        this.penyimpanan = penyimpanan;
        this.daftarBarang = daftarBarang;
        this.barang = barang;
        this.riwayat = riwayat;
        this.tanggalPemantauan = null;
        perbaruiCombo();
        perbaruiHalaman();
        tampilkanHalaman(halamanRingkasan, "ringkasan");
        tampilkanPesan("Data dan riwayat dimuat dari database.", true);
    }

    private HalamanFx muatHalaman(String resource) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    ShellController.class.getResource(resource));
            Node node = loader.load();
            HalamanFx controller = loader.getController();
            if (controller == null) {
                throw new IllegalStateException("Controller halaman tidak ditemukan: " + resource);
            }
            // Simpan node melalui controller agar penggantian halaman memakai root FXML.
            return new HalamanFx() {
                @Override
                public void setKonteks(KonteksFx konteks) {
                    controller.setKonteks(konteks);
                }

                @Override
                public void perbarui() {
                    controller.perbarui();
                }

                @Override
                public Node getNode() {
                    return node;
                }
            };
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Gagal memuat halaman " + resource, exception);
        }
    }

    private void tampilkanHalaman(HalamanFx halaman, String nama) {
        halamanAktif = halaman;
        ScrollPane scroll = new ScrollPane(halaman.getNode());
        scroll.setFitToWidth(true);
        scroll.setPannable(true);
        scroll.getStyleClass().add("page-scroll");
        halamanContainer.getChildren().setAll(scroll);
        if ("ringkasan".equals(nama)) {
            navRingkasan.setSelected(true);
        } else {
            navStokMasuk.setSelected(true);
        }
        halaman.perbarui();
    }

    @FXML
    private void tampilkanRingkasan() {
        tampilkanHalaman(halamanRingkasan, "ringkasan");
    }

    @FXML
    private void tampilkanStokMasuk() {
        tampilkanHalaman(halamanStokMasuk, "stok-masuk");
    }

    private void perbaruiHalaman() {
        if (halamanRingkasan != null) {
            halamanRingkasan.perbarui();
        }
        if (halamanStokMasuk != null) {
            halamanStokMasuk.perbarui();
        }
    }

    private void perbaruiCombo() {
        menggantiBarang = true;
        try {
            barangCombo.getItems().setAll(daftarBarang);
            for (Barang item : daftarBarang) {
                if (item.getId() == barang.getId()) {
                    barangCombo.setValue(item);
                    break;
                }
            }
        } finally {
            menggantiBarang = false;
        }
    }

    @Override
    public Barang getBarangAktif() {
        return barang;
    }

    @Override
    public List<Barang> getDaftarBarang() {
        return java.util.Collections.unmodifiableList(daftarBarang);
    }

    @Override
    public List<Transaksi> getRiwayat() {
        return java.util.Collections.unmodifiableList(riwayat);
    }

    @Override
    public java.time.LocalDate getTanggalPemantauan() {
        return tanggalPemantauan;
    }

    @Override
    public void setTanggalPemantauan(java.time.LocalDate tanggal) {
        this.tanggalPemantauan = tanggal;
        if (halamanRingkasan != null) {
            halamanRingkasan.perbarui();
        }
    }

    @Override
    public void simpanPenerimaanAsync(String kodeBatch, int jumlah,
            java.time.LocalDate tanggalMasuk, java.time.LocalDate tanggalKedaluwarsa,
            Consumer<Boolean> selesai) {
        if (sibuk) {
            tampilkanPesan("Tunggu proses sebelumnya selesai.", false);
            return;
        }
        sibuk = true;
        barangCombo.setDisable(true);
        halamanContainer.setDisable(true);
        tampilkanPesan("Menyimpan penerimaan...", true);

        Barang aktif = this.barang;
        Task<HasilSimpan> task = new Task<>() {
            @Override
            protected HasilSimpan call() {
                String pesan;
                boolean berhasil = false;
                try {
                    PenerimaanStok penerimaan = new PenerimaanStok(tanggalMasuk);
                    penerimaan.terima(aktif, kodeBatch, jumlah, tanggalMasuk,
                            tanggalKedaluwarsa);
                    penyimpanan.simpanTransaksi(aktif, penerimaan);
                    berhasil = true;
                    pesan = "Penerimaan berhasil: " + kodeBatch + ", " + jumlah
                            + " " + aktif.getSatuan() + ".";
                } catch (RuntimeException exception) {
                    pesan = "Penerimaan gagal: " + exception.getMessage();
                }
                return muatHasil(aktif.getId(), berhasil, pesan);
            }
        };
        task.setOnSucceeded(event -> {
            HasilSimpan hasil = task.getValue();
            terapkanHasil(hasil);
            sibuk = false;
            barangCombo.setDisable(false);
            halamanContainer.setDisable(false);
            tampilkanPesan(hasil.pesan, hasil.berhasil);
            if (selesai != null) {
                selesai.accept(hasil.berhasil);
            }
        });
        task.setOnFailed(event -> {
            sibuk = false;
            barangCombo.setDisable(false);
            halamanContainer.setDisable(false);
            Throwable error = task.getException();
            tampilkanPesan("Penerimaan gagal: "
                    + (error == null ? "kesalahan tidak diketahui" : error.getMessage()),
                    false);
            if (selesai != null) {
                selesai.accept(false);
            }
        });
        io.submit(task);
    }

    private HasilSimpan muatHasil(int idBarang, boolean berhasil, String pesan) {
        try {
            Barang terbaru = penyimpanan.muatBarang(idBarang);
            List<Transaksi> riwayatTerbaru = penyimpanan.muatRiwayat(terbaru);
            return new HasilSimpan(berhasil, pesan, terbaru, riwayatTerbaru);
        } catch (PenyimpananException exception) {
            return new HasilSimpan(false, pesan + " Gagal memuat ulang: "
                    + exception.getMessage(), barang, riwayat);
        }
    }

    private void terapkanHasil(HasilSimpan hasil) {
        for (int i = 0; i < daftarBarang.size(); i++) {
            if (daftarBarang.get(i).getId() == hasil.barang.getId()) {
                daftarBarang.set(i, hasil.barang);
                break;
            }
        }
        this.barang = hasil.barang;
        this.riwayat = hasil.riwayat;
        perbaruiCombo();
        perbaruiHalaman();
    }

    private void gantiBarangAsync(Barang target) {
        if (sibuk) {
            perbaruiCombo();
            return;
        }
        sibuk = true;
        barangCombo.setDisable(true);
        halamanContainer.setDisable(true);
        tampilkanPesan("Memuat " + target.getNama() + "...", true);

        Task<HasilBarang> task = new Task<>() {
            @Override
            protected HasilBarang call() {
                try {
                    Barang terbaru = penyimpanan.muatBarang(target.getId());
                    List<Transaksi> riwayatBaru = penyimpanan.muatRiwayat(terbaru);
                    return new HasilBarang(terbaru, riwayatBaru, null);
                } catch (PenyimpananException exception) {
                    return new HasilBarang(null, null, exception.getMessage());
                }
            }
        };
        task.setOnSucceeded(event -> {
            HasilBarang hasil = task.getValue();
            sibuk = false;
            barangCombo.setDisable(false);
            halamanContainer.setDisable(false);
            if (hasil.barang == null) {
                tampilkanPesan("Gagal mengganti barang: " + hasil.error, false);
                perbaruiCombo();
                return;
            }
            for (int i = 0; i < daftarBarang.size(); i++) {
                if (daftarBarang.get(i).getId() == hasil.barang.getId()) {
                    daftarBarang.set(i, hasil.barang);
                    break;
                }
            }
            barang = hasil.barang;
            riwayat = hasil.riwayat;
            tanggalPemantauan = null;
            perbaruiCombo();
            perbaruiHalaman();
            tampilkanPesan("Barang aktif: " + barang.getNama()
                    + " (" + barang.getSatuan() + ").", true);
        });
        task.setOnFailed(event -> {
            sibuk = false;
            barangCombo.setDisable(false);
            halamanContainer.setDisable(false);
            Throwable error = task.getException();
            tampilkanPesan("Gagal mengganti barang: "
                    + (error == null ? "kesalahan tidak diketahui" : error.getMessage()),
                    false);
            perbaruiCombo();
        });
        io.submit(task);
    }

    @Override
    public void tampilkanPesan(String pesan, boolean sukses) {
        statusLabel.setText(pesan);
        statusLabel.getStyleClass().removeAll("status-success", "status-error");
        statusLabel.getStyleClass().add(sukses ? "status-success" : "status-error");
    }

    @Override
    public void close() {
        io.shutdownNow();
        if (penyimpanan != null) {
            try {
                penyimpanan.close();
            } catch (PenyimpananException exception) {
                System.err.println("Gagal menutup database: " + exception.getMessage());
            }
        }
    }

    private static final class HasilSimpan {
        private final boolean berhasil;
        private final String pesan;
        private final Barang barang;
        private final List<Transaksi> riwayat;

        private HasilSimpan(boolean berhasil, String pesan, Barang barang,
                List<Transaksi> riwayat) {
            this.berhasil = berhasil;
            this.pesan = pesan;
            this.barang = barang;
            this.riwayat = riwayat;
        }
    }

    private static final class HasilBarang {
        private final Barang barang;
        private final List<Transaksi> riwayat;
        private final String error;

        private HasilBarang(Barang barang, List<Transaksi> riwayat, String error) {
            this.barang = barang;
            this.riwayat = riwayat;
            this.error = error;
        }
    }
}
