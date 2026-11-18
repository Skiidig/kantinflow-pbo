package kantinflow.fx;

import java.util.ArrayList;
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
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.util.StringConverter;

import kantinflow.Barang;
import kantinflow.BatchStok;
import kantinflow.PenerimaanStok;
import kantinflow.PengeluaranStok;
import kantinflow.RincianTransaksi;
import kantinflow.Transaksi;
import kantinflow.penyimpanan.PenyimpananException;
import kantinflow.penyimpanan.PenyimpananSQLite;

/**
 * Shell JavaFX: sidebar, pemilih barang, tiga halaman, status.
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
    private ProgressIndicator progress;
    @FXML
    private ToggleButton navRingkasan;
    @FXML
    private ToggleButton navBarang;
    @FXML
    private ToggleButton navStokMasuk;
    @FXML
    private ToggleButton navStokKeluar;

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
    private HalamanFx halamanBarang;
    private HalamanFx halamanStokMasuk;
    private HalamanFx halamanStokKeluar;
    private HalamanFx halamanAktif;
    private boolean menggantiBarang;
    private boolean sibuk;
    private long versiBarang;

    @FXML
    private void initialize() {
        navRingkasan.setToggleGroup(grupNav);
        navBarang.setToggleGroup(grupNav);
        navStokMasuk.setToggleGroup(grupNav);
        navStokKeluar.setToggleGroup(grupNav);
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
        halamanBarang = muatHalaman("/kantinflow/fx/barang.fxml");
        halamanStokMasuk = muatHalaman("/kantinflow/fx/stok-masuk.fxml");
        halamanStokKeluar = muatHalaman("/kantinflow/fx/stok-keluar.fxml");
        halamanRingkasan.setKonteks(this);
        halamanBarang.setKonteks(this);
        halamanStokMasuk.setKonteks(this);
        halamanStokKeluar.setKonteks(this);
    }

    public void setData(PenyimpananSQLite penyimpanan, List<Barang> daftarBarang,
            Barang barang, List<Transaksi> riwayat) {
        this.penyimpanan = penyimpanan;
        this.daftarBarang = daftarBarang;
        this.barang = barang;
        this.riwayat = riwayat;
        this.tanggalPemantauan = null;
        versiBarang++;
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
        } else if ("barang".equals(nama)) {
            navBarang.setSelected(true);
        } else if ("stok-masuk".equals(nama)) {
            navStokMasuk.setSelected(true);
        } else {
            navStokKeluar.setSelected(true);
        }
        halaman.perbarui();
    }

    @FXML
    private void tampilkanRingkasan() {
        tampilkanHalaman(halamanRingkasan, "ringkasan");
    }

    @FXML
    private void tampilkanBarang() {
        tampilkanHalaman(halamanBarang, "barang");
    }

    @FXML
    private void tampilkanStokMasuk() {
        tampilkanHalaman(halamanStokMasuk, "stok-masuk");
    }

    @FXML
    private void tampilkanStokKeluar() {
        tampilkanHalaman(halamanStokKeluar, "stok-keluar");
    }

    private void perbaruiHalaman() {
        if (halamanRingkasan != null) {
            halamanRingkasan.perbarui();
        }
        if (halamanBarang != null) {
            halamanBarang.perbarui();
        }
        if (halamanStokMasuk != null) {
            halamanStokMasuk.perbarui();
        }
        if (halamanStokKeluar != null) {
            halamanStokKeluar.perbarui();
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
        aturSibukUI(true);
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
            aturSibukUI(false);
            tampilkanPesan(hasil.pesan, hasil.berhasil);
            if (selesai != null) {
                selesai.accept(hasil.berhasil);
            }
        });
        task.setOnFailed(event -> {
            sibuk = false;
            aturSibukUI(false);
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

    @Override
    public void keluarkanManualAsync(BatchStok batch, int jumlah,
            Consumer<HasilPengeluaran> selesai) {
        jalankanPengeluaran(batch, jumlah, java.time.LocalDate.now(), false, selesai);
    }

    @Override
    public void keluarkanFefoAsync(int jumlah, java.time.LocalDate tanggal,
            Consumer<HasilPengeluaran> selesai) {
        jalankanPengeluaran(null, jumlah, tanggal, true, selesai);
    }

    private void jalankanPengeluaran(BatchStok batch, int jumlah,
            java.time.LocalDate tanggal, boolean fefo,
            Consumer<HasilPengeluaran> selesai) {
        if (sibuk) {
            tampilkanPesan("Tunggu proses sebelumnya selesai.", false);
            if (selesai != null) {
                selesai.accept(new HasilPengeluaran(
                        false, "Tunggu proses sebelumnya selesai.",
                        List.of(), barang, riwayat));
            }
            return;
        }
        sibuk = true;
        aturSibukUI(true);
        tampilkanPesan(fefo ? "Memproses pengeluaran FEFO..."
                : "Memproses pengeluaran manual...", true);

        Barang aktif = this.barang;
        int idAktif = aktif.getId();
        long versiAktif = versiBarang;
        Task<HasilPengeluaran> task = new Task<>() {
            @Override
            protected HasilPengeluaran call() {
                PengeluaranStok pengeluaran = new PengeluaranStok(tanggal);
                boolean berhasil = false;
                List<RincianTransaksi> rincian = List.of();
                String pesan;
                try {
                    if (fefo) {
                        pengeluaran.keluarkanFefo(aktif, jumlah, tanggal);
                    } else {
                        pengeluaran.keluarkan(aktif, batch, jumlah);
                    }
                    penyimpanan.simpanTransaksi(aktif, pengeluaran);
                    berhasil = true;
                    rincian = List.copyOf(pengeluaran.getRincian());
                    pesan = fefo ? "Pengeluaran FEFO berhasil."
                            : "Pengeluaran berhasil.";
                } catch (RuntimeException exception) {
                    pesan = (fefo ? "Pengeluaran FEFO gagal: "
                            : "Pengeluaran gagal: ") + exception.getMessage();
                }

                Barang terbaru = null;
                List<Transaksi> riwayatBaru = null;
                try {
                    terbaru = penyimpanan.muatBarang(idAktif);
                    riwayatBaru = penyimpanan.muatRiwayat(terbaru);
                } catch (PenyimpananException exception) {
                    pesan = pesan + " Gagal memuat ulang: " + exception.getMessage();
                    berhasil = false;
                    rincian = List.of();
                }
                return new HasilPengeluaran(berhasil, pesan, rincian,
                        terbaru, riwayatBaru);
            }
        };
        task.setOnSucceeded(event -> {
            HasilPengeluaran hasil = task.getValue();
            sibuk = false;
            aturSibukUI(false);

            if (hasil.barang() == null) {
                tampilkanPesan(hasil.pesan(), false);
                if (selesai != null) {
                    selesai.accept(hasil);
                }
                return;
            }
            if (barang.getId() != idAktif || versiBarang != versiAktif) {
                HasilPengeluaran basi = new HasilPengeluaran(
                        false, "Hasil pengeluaran barang lama diabaikan.",
                        List.of(), barang, riwayat);
                tampilkanPesan(basi.pesan(), false);
                if (selesai != null) {
                    selesai.accept(basi);
                }
                return;
            }

            for (int i = 0; i < daftarBarang.size(); i++) {
                if (daftarBarang.get(i).getId() == hasil.barang().getId()) {
                    daftarBarang.set(i, hasil.barang());
                    break;
                }
            }
            barang = hasil.barang();
            riwayat = hasil.riwayat();
            perbaruiCombo();
            perbaruiHalaman();
            tampilkanPesan(hasil.pesan(), hasil.berhasil());
            if (selesai != null) {
                selesai.accept(hasil);
            }
        });
        task.setOnFailed(event -> {
            sibuk = false;
            aturSibukUI(false);
            Throwable error = task.getException();
            String pesan = "Pengeluaran gagal: "
                    + (error == null ? "kesalahan tidak diketahui" : error.getMessage());
            tampilkanPesan(pesan, false);
            if (selesai != null) {
                selesai.accept(new HasilPengeluaran(
                        false, pesan, List.of(), barang, riwayat));
            }
        });
        io.submit(task);
    }

    @Override
    public void pilihBarangAsync(Barang target) {
        gantiBarangAsync(target);
    }

    @Override
    public void tambahBarangAsync(String nama, String satuan, int stokMinimum,
            Consumer<Boolean> selesai) {
        if (sibuk) {
            tampilkanPesan("Tunggu proses sebelumnya selesai.", false);
            if (selesai != null) {
                selesai.accept(false);
            }
            return;
        }
        sibuk = true;
        aturSibukUI(true);
        tampilkanPesan("Menambah barang...", true);
        int idAktif = barang.getId();
        long versiAktif = versiBarang;

        Task<HasilTambah> task = new Task<>() {
            @Override
            protected HasilTambah call() {
                try {
                    Barang baru = penyimpanan.simpanBarang(
                            new Barang(0, nama, satuan, stokMinimum));
                    List<Barang> daftarBaru = new ArrayList<>(daftarBarang);
                    daftarBaru.add(baru);
                    return new HasilTambah(true, "Barang ditambahkan: "
                            + baru.getNama() + " (" + baru.getSatuan() + ").",
                            baru, daftarBaru, List.of());
                } catch (RuntimeException exception) {
                    Barang aktifTerbaru = null;
                    List<Barang> daftarBaru = null;
                    List<Transaksi> riwayatBaru = null;
                    try {
                        aktifTerbaru = penyimpanan.muatBarang(idAktif);
                        daftarBaru = penyimpanan.muatSemuaBarang();
                        riwayatBaru = penyimpanan.muatRiwayat(aktifTerbaru);
                    } catch (RuntimeException ignored) {
                        // Pesan kegagalan utama tetap dilaporkan.
                    }
                    return new HasilTambah(false, "Tambah barang gagal: "
                            + exception.getMessage(), aktifTerbaru, daftarBaru,
                            riwayatBaru);
                }
            }
        };
        task.setOnSucceeded(event -> {
            HasilTambah hasil = task.getValue();
            sibuk = false;
            aturSibukUI(false);
            if (barang.getId() != idAktif || versiBarang != versiAktif) {
                tampilkanPesan("Hasil tambah barang lama diabaikan.", false);
                if (selesai != null) {
                    selesai.accept(false);
                }
                return;
            }
            if (!hasil.berhasil) {
                if (hasil.barang != null) {
                    terapkanDataBarang(hasil.barang, hasil.daftar, hasil.riwayat);
                }
                tampilkanPesan(hasil.pesan, false);
                if (selesai != null) {
                    selesai.accept(false);
                }
                return;
            }
            terapkanDataBarang(hasil.barang, hasil.daftar, List.of());
            tampilkanPesan(hasil.pesan, true);
            if (selesai != null) {
                selesai.accept(true);
            }
        });
        task.setOnFailed(event -> {
            sibuk = false;
            aturSibukUI(false);
            Throwable error = task.getException();
            tampilkanPesan("Tambah barang gagal: "
                    + (error == null ? "kesalahan tidak diketahui" : error.getMessage()),
                    false);
            if (selesai != null) {
                selesai.accept(false);
            }
        });
        io.submit(task);
    }

    @Override
    public void simpanMinimumAsync(int stokMinimum, Consumer<Boolean> selesai) {
        if (sibuk) {
            tampilkanPesan("Tunggu proses sebelumnya selesai.", false);
            if (selesai != null) {
                selesai.accept(false);
            }
            return;
        }
        sibuk = true;
        aturSibukUI(true);
        tampilkanPesan("Menyimpan stok minimum...", true);
        Barang target = barang;
        int idAktif = target.getId();
        long versiAktif = versiBarang;

        Task<HasilMinimum> task = new Task<>() {
            @Override
            protected HasilMinimum call() {
                try {
                    penyimpanan.simpanStokMinimum(target, stokMinimum);
                    Barang baru = penyimpanan.muatBarang(idAktif);
                    List<Barang> daftarBaru = penyimpanan.muatSemuaBarang();
                    return new HasilMinimum(true, "Stok minimum "
                            + target.getNama() + " disimpan: " + stokMinimum
                            + " " + target.getSatuan() + ".", baru, daftarBaru,
                            null);
                } catch (RuntimeException exception) {
                    Barang aktifTerbaru = null;
                    List<Barang> daftarBaru = null;
                    List<Transaksi> riwayatBaru = null;
                    try {
                        aktifTerbaru = penyimpanan.muatBarang(idAktif);
                        daftarBaru = penyimpanan.muatSemuaBarang();
                        riwayatBaru = penyimpanan.muatRiwayat(aktifTerbaru);
                    } catch (RuntimeException ignored) {
                        // Pesan kegagalan utama tetap dilaporkan.
                    }
                    return new HasilMinimum(false,
                            "Gagal menyimpan stok minimum: "
                            + exception.getMessage(), aktifTerbaru, daftarBaru,
                            riwayatBaru);
                }
            }
        };
        task.setOnSucceeded(event -> {
            HasilMinimum hasil = task.getValue();
            sibuk = false;
            aturSibukUI(false);
            if (barang.getId() != idAktif || versiBarang != versiAktif) {
                tampilkanPesan("Hasil minimum barang lama diabaikan.", false);
                if (selesai != null) {
                    selesai.accept(false);
                }
                return;
            }
            if (!hasil.berhasil) {
                if (hasil.barang != null) {
                    terapkanDataBarang(hasil.barang, hasil.daftar, hasil.riwayat);
                }
                tampilkanPesan(hasil.pesan, false);
                if (selesai != null) {
                    selesai.accept(false);
                }
                return;
            }
            terapkanDataBarang(hasil.barang, hasil.daftar, hasil.riwayat);
            tampilkanPesan(hasil.pesan, true);
            if (selesai != null) {
                selesai.accept(true);
            }
        });
        task.setOnFailed(event -> {
            sibuk = false;
            aturSibukUI(false);
            Throwable error = task.getException();
            tampilkanPesan("Gagal menyimpan stok minimum: "
                    + (error == null ? "kesalahan tidak diketahui" : error.getMessage()),
                    false);
            if (selesai != null) {
                selesai.accept(false);
            }
        });
        io.submit(task);
    }

    private void terapkanDataBarang(Barang baru, List<Barang> daftarBaru,
            List<Transaksi> riwayatBaru) {
        if (daftarBaru != null) {
            daftarBarang.clear();
            daftarBarang.addAll(daftarBaru);
        }
        if (baru == null) {
            return;
        }
        barang = baru;
        if (riwayatBaru != null) {
            riwayat = riwayatBaru;
        }
        tanggalPemantauan = null;
        versiBarang++;
        perbaruiCombo();
        perbaruiHalaman();
    }

    private void aturSibukUI(boolean aktif) {
        progress.setVisible(aktif);
        progress.setManaged(aktif);
        barangCombo.setDisable(aktif);
        halamanContainer.setDisable(aktif);
    }

    private void gantiBarangAsync(Barang target) {
        if (sibuk) {
            perbaruiCombo();
            return;
        }
        sibuk = true;
        aturSibukUI(true);
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
            aturSibukUI(false);
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
            versiBarang++;
            perbaruiCombo();
            perbaruiHalaman();
            tampilkanPesan("Barang aktif: " + barang.getNama()
                    + " (" + barang.getSatuan() + ").", true);
        });
        task.setOnFailed(event -> {
            sibuk = false;
            aturSibukUI(false);
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

    private static final class HasilTambah {
        private final boolean berhasil;
        private final String pesan;
        private final Barang barang;
        private final List<Barang> daftar;
        private final List<Transaksi> riwayat;

        private HasilTambah(boolean berhasil, String pesan, Barang barang,
                List<Barang> daftar, List<Transaksi> riwayat) {
            this.berhasil = berhasil;
            this.pesan = pesan;
            this.barang = barang;
            this.daftar = daftar;
            this.riwayat = riwayat;
        }
    }

    private static final class HasilMinimum {
        private final boolean berhasil;
        private final String pesan;
        private final Barang barang;
        private final List<Barang> daftar;
        private final List<Transaksi> riwayat;

        private HasilMinimum(boolean berhasil, String pesan, Barang barang,
                List<Barang> daftar, List<Transaksi> riwayat) {
            this.berhasil = berhasil;
            this.pesan = pesan;
            this.barang = barang;
            this.daftar = daftar;
            this.riwayat = riwayat;
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
