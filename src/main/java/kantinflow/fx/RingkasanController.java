package kantinflow.fx;

import java.time.LocalDate;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import kantinflow.Barang;
import kantinflow.BatchStok;
import kantinflow.StatusKedaluwarsa;

public class RingkasanController implements HalamanFx {
    @FXML
    private VBox halaman;
    @FXML
    private Label barangAktifLabel;
    @FXML
    private DatePicker tanggalPicker;
    @FXML
    private Button periksaButton;
    @FXML
    private Label fisikLabel;
    @FXML
    private Label layakLabel;
    @FXML
    private Label minimumLabel;
    @FXML
    private Label statusRestokLabel;
    @FXML
    private Label errorLabel;
    @FXML
    private TableView<BatchStok> tabelBatch;
    @FXML
    private TableColumn<BatchStok, String> kolomKode;
    @FXML
    private TableColumn<BatchStok, String> kolomSisa;
    @FXML
    private TableColumn<BatchStok, String> kolomMasuk;
    @FXML
    private TableColumn<BatchStok, String> kolomKedaluwarsa;
    @FXML
    private TableColumn<BatchStok, String> kolomStatus;

    private KonteksFx konteks;

    @FXML
    private void initialize() {
        tanggalPicker.setValue(LocalDate.now());
        periksaButton.setOnAction(event -> periksa());

        kolomKode.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getKode()));
        kolomSisa.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                data.getValue().getSisaJumlah()
                        + (konteks == null || konteks.getBarangAktif() == null
                                ? "" : " " + konteks.getBarangAktif().getSatuan())));
        kolomMasuk.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getTanggalMasuk().toString()));
        kolomKedaluwarsa.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getTanggalKedaluwarsa().toString()));
        kolomStatus.setCellValueFactory(data -> {
            LocalDate tanggal = tanggalPicker.getValue();
            return new ReadOnlyStringWrapper(tanggal == null ? "-"
                    : data.getValue().statusPada(tanggal).getLabel());
        });
        kolomStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean kosong) {
                super.updateItem(status, kosong);
                getStyleClass().removeAll(
                        "status-cell-safe", "status-cell-warning", "status-cell-danger");
                if (kosong || status == null) {
                    setText(null);
                    return;
                }
                setText(status);
                if (StatusKedaluwarsa.AMAN.getLabel().equals(status)) {
                    getStyleClass().add("status-cell-safe");
                } else if (StatusKedaluwarsa.SEGERA_KEDALUWARSA.getLabel().equals(status)) {
                    getStyleClass().add("status-cell-warning");
                } else {
                    getStyleClass().add("status-cell-danger");
                }
            }
        });

        tabelBatch.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabelBatch.setPlaceholder(new Label("Belum ada batch untuk barang ini."));
    }

    @Override
    public void setKonteks(KonteksFx konteks) {
        this.konteks = konteks;
        if (konteks.getTanggalPemantauan() != null) {
            tanggalPicker.setValue(konteks.getTanggalPemantauan());
        }
        perbarui();
    }

    private void periksa() {
        LocalDate tanggal = tanggalPicker.getValue();
        if (tanggal == null) {
            errorLabel.setText("Tanggal pengecekan wajib diisi.");
            konteks.tampilkanPesan("Tanggal pengecekan wajib diisi.", false);
            tanggalPicker.requestFocus();
            return;
        }
        errorLabel.setText("");
        konteks.setTanggalPemantauan(tanggal);
        perbarui();
        konteks.tampilkanPesan("Ringkasan diperiksa untuk tanggal " + tanggal + ".", true);
    }

    @Override
    public void perbarui() {
        if (konteks == null || konteks.getBarangAktif() == null) {
            return;
        }
        LocalDate tanggal = konteks.getTanggalPemantauan();
        if (tanggal == null) {
            tanggal = tanggalPicker.getValue();
        }
        if (tanggal == null) {
            tanggal = LocalDate.now();
        }
        tanggalPicker.setValue(tanggal);

        Barang barang = konteks.getBarangAktif();
        int fisik = barang.hitungSaldo();
        int layak = barang.hitungStokLayak(tanggal);
        int minimum = barang.getStokMinimum();
        String satuan = barang.getSatuan();

        barangAktifLabel.setText(barang.getNama() + " (" + satuan + ")");
        fisikLabel.setText(fisik + " " + satuan);
        layakLabel.setText(layak + " " + satuan);
        minimumLabel.setText(minimum + " " + satuan);

        boolean perluRestok = layak <= minimum;
        statusRestokLabel.setText(perluRestok ? "Perlu restok" : "Cukup");
        statusRestokLabel.getStyleClass().removeAll("status-restock", "status-cukup");
        statusRestokLabel.getStyleClass().add(
                perluRestok ? "status-restock" : "status-cukup");

        tabelBatch.setItems(FXCollections.observableArrayList(barang.getDaftarBatch()));
        tabelBatch.refresh();
    }

    @Override
    public Node getNode() {
        return halaman;
    }
}
