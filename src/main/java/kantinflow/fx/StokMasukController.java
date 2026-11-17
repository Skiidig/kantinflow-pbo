package kantinflow.fx;

import java.time.LocalDate;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import kantinflow.BatchStok;

public class StokMasukController implements HalamanFx {
    @FXML
    private VBox halaman;
    @FXML
    private TextField kodeBatchField;
    @FXML
    private TextField jumlahField;
    @FXML
    private DatePicker tanggalMasukPicker;
    @FXML
    private DatePicker tanggalKedaluwarsaPicker;
    @FXML
    private Button simpanButton;
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

    private KonteksFx konteks;

    @FXML
    private void initialize() {
        LocalDate hariIni = LocalDate.now();
        tanggalMasukPicker.setValue(hariIni);
        tanggalKedaluwarsaPicker.setValue(hariIni.plusMonths(1));
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
        simpanButton.setOnAction(event -> simpan());
        tabelBatch.setPlaceholder(new javafx.scene.control.Label(
                "Belum ada batch untuk barang ini."));
        tabelBatch.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    @Override
    public void setKonteks(KonteksFx konteks) {
        this.konteks = konteks;
        perbarui();
    }

    private void simpan() {
        if (konteks == null) {
            return;
        }
        String kode = kodeBatchField.getText() == null
                ? "" : kodeBatchField.getText().trim();
        int jumlah;
        try {
            jumlah = Integer.parseInt(jumlahField.getText() == null
                    ? "" : jumlahField.getText().trim());
        } catch (NumberFormatException exception) {
            konteks.tampilkanPesan("Jumlah harus berupa angka bulat.", false);
            return;
        }
        LocalDate masuk = tanggalMasukPicker.getValue();
        LocalDate kedaluwarsa = tanggalKedaluwarsaPicker.getValue();
        if (masuk == null || kedaluwarsa == null) {
            konteks.tampilkanPesan("Tanggal masuk dan kedaluwarsa wajib diisi.", false);
            return;
        }

        simpanButton.setDisable(true);
        konteks.simpanPenerimaanAsync(kode, jumlah, masuk, kedaluwarsa, sukses -> {
            simpanButton.setDisable(false);
            if (sukses) {
                kodeBatchField.clear();
                jumlahField.clear();
                LocalDate hariIni = LocalDate.now();
                tanggalMasukPicker.setValue(hariIni);
                tanggalKedaluwarsaPicker.setValue(hariIni.plusMonths(1));
            }
        });
    }

    @Override
    public void perbarui() {
        if (konteks == null || konteks.getBarangAktif() == null) {
            return;
        }
        tabelBatch.setItems(FXCollections.observableArrayList(
                konteks.getBarangAktif().getDaftarBatch()));
    }

    @Override
    public Node getNode() {
        return halaman;
    }
}
