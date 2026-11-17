package kantinflow.fx;

import java.time.LocalDate;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import kantinflow.Barang;
import kantinflow.BatchStok;
import kantinflow.RincianTransaksi;

public class StokKeluarController implements HalamanFx {
    @FXML
    private VBox halaman;
    @FXML
    private ComboBox<BatchStok> batchCombo;
    @FXML
    private Label sisaBatchLabel;
    @FXML
    private TextField jumlahManualField;
    @FXML
    private Button manualButton;
    @FXML
    private TextField jumlahFefoField;
    @FXML
    private DatePicker tanggalFefoPicker;
    @FXML
    private Button fefoButton;
    @FXML
    private Label hasilAlokasiLabel;

    private KonteksFx konteks;
    private int idBarangTerakhir = -1;
    private boolean sibuk;

    @FXML
    private void initialize() {
        tanggalFefoPicker.setValue(LocalDate.now());
        batchCombo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(BatchStok batch, boolean kosong) {
                super.updateItem(batch, kosong);
                if (kosong || batch == null || konteks == null
                        || konteks.getBarangAktif() == null) {
                    setText(null);
                } else {
                    setText(batch.getKode() + " — sisa " + batch.getSisaJumlah()
                            + " " + konteks.getBarangAktif().getSatuan()
                            + " — kedaluwarsa " + batch.getTanggalKedaluwarsa());
                }
            }
        });
        batchCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(BatchStok batch) {
                if (batch == null || konteks == null
                        || konteks.getBarangAktif() == null) {
                    return "";
                }
                return batch.getKode() + " — sisa " + batch.getSisaJumlah()
                        + " " + konteks.getBarangAktif().getSatuan()
                        + " — kedaluwarsa " + batch.getTanggalKedaluwarsa();
            }

            @Override
            public BatchStok fromString(String teks) {
                return null;
            }
        });
        batchCombo.valueProperty().addListener((obs, lama, baru) -> perbaruiSisa());
        manualButton.setOnAction(event -> keluarkanManual());
        fefoButton.setOnAction(event -> keluarkanFefo());
        hasilAlokasiLabel.setText("Belum ada pengeluaran pada halaman ini.");
    }

    @Override
    public void setKonteks(KonteksFx konteks) {
        this.konteks = konteks;
        perbarui();
    }

    private void perbaruiSisa() {
        BatchStok batch = batchCombo.getValue();
        if (batch == null) {
            sisaBatchLabel.setText("Pilih batch untuk melihat sisa.");
            manualButton.setDisable(true);
            return;
        }
        sisaBatchLabel.setText("Sisa batch " + batch.getKode() + ": "
                + batch.getSisaJumlah() + " "
                + konteks.getBarangAktif().getSatuan());
        manualButton.setDisable(sibuk);
    }

    private void keluarkanManual() {
        if (sibuk || konteks == null) {
            return;
        }
        BatchStok batch = batchCombo.getValue();
        if (batch == null) {
            konteks.tampilkanPesan("Pilih batch terlebih dahulu.", false);
            return;
        }
        int jumlah;
        try {
            jumlah = Integer.parseInt(jumlahManualField.getText().trim());
        } catch (NumberFormatException exception) {
            konteks.tampilkanPesan("Jumlah harus berupa angka bulat.", false);
            return;
        }

        sibuk = true;
        manualButton.setDisable(true);
        fefoButton.setDisable(true);
        konteks.keluarkanManualAsync(batch, jumlah, hasil -> selesai(hasil, false));
    }

    private void keluarkanFefo() {
        if (sibuk || konteks == null) {
            return;
        }
        int jumlah;
        try {
            jumlah = Integer.parseInt(jumlahFefoField.getText().trim());
        } catch (NumberFormatException exception) {
            konteks.tampilkanPesan("Jumlah harus berupa angka bulat.", false);
            return;
        }
        LocalDate tanggal = tanggalFefoPicker.getValue();
        if (tanggal == null) {
            konteks.tampilkanPesan("Tanggal pengeluaran wajib diisi.", false);
            return;
        }

        sibuk = true;
        manualButton.setDisable(true);
        fefoButton.setDisable(true);
        konteks.keluarkanFefoAsync(jumlah, tanggal, hasil -> selesai(hasil, true));
    }

    private void selesai(HasilPengeluaran hasil, boolean fefo) {
        sibuk = false;
        if (hasil.berhasil()) {
            hasilAlokasiLabel.setText(formatAlokasi(hasil));
            if (fefo) {
                jumlahFefoField.clear();
                tanggalFefoPicker.setValue(LocalDate.now());
            } else {
                jumlahManualField.clear();
            }
        } else {
            hasilAlokasiLabel.setText("Tidak ada alokasi: " + hasil.pesan());
        }
        perbarui();
    }

    private String formatAlokasi(HasilPengeluaran hasil) {
        StringBuilder teks = new StringBuilder("Alokasi per batch:\n");
        for (RincianTransaksi rincian : hasil.rincian()) {
            teks.append("• ")
                    .append(rincian.getBatch().getKode())
                    .append(": ")
                    .append(rincian.getJumlah())
                    .append(" ")
                    .append(hasil.barang().getSatuan())
                    .append("\n");
        }
        return teks.toString().trim();
    }

    @Override
    public void perbarui() {
        if (konteks == null || konteks.getBarangAktif() == null) {
            return;
        }
        Barang aktif = konteks.getBarangAktif();
        boolean barangBaru = idBarangTerakhir != aktif.getId();
        String kodeTerpilih = batchCombo.getValue() == null
                ? null : batchCombo.getValue().getKode();
        idBarangTerakhir = aktif.getId();

        batchCombo.setItems(FXCollections.observableArrayList(
                aktif.getDaftarBatch().stream()
                        .filter(batch -> batch.getSisaJumlah() > 0)
                        .toList()));
        if (barangBaru) {
            batchCombo.setValue(null);
            jumlahManualField.clear();
            jumlahFefoField.clear();
            tanggalFefoPicker.setValue(LocalDate.now());
            hasilAlokasiLabel.setText("Belum ada pengeluaran pada halaman ini.");
        } else if (kodeTerpilih != null) {
            batchCombo.getItems().stream()
                    .filter(batch -> batch.getKode().equals(kodeTerpilih))
                    .findFirst()
                    .ifPresent(batchCombo::setValue);
        }
        perbaruiSisa();
        tombolAktif();
    }

    private void tombolAktif() {
        manualButton.setDisable(sibuk || batchCombo.getValue() == null);
        fefoButton.setDisable(sibuk);
    }

    @Override
    public Node getNode() {
        return halaman;
    }
}
