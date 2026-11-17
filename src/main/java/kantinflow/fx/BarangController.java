package kantinflow.fx;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import kantinflow.Barang;

public class BarangController implements HalamanFx {
    @FXML
    private VBox halaman;
    @FXML
    private TableView<Barang> tabelBarang;
    @FXML
    private TableColumn<Barang, String> kolomNama;
    @FXML
    private TableColumn<Barang, String> kolomSatuan;
    @FXML
    private TableColumn<Barang, String> kolomMinimum;
    @FXML
    private TextField namaBaruField;
    @FXML
    private TextField satuanBaruField;
    @FXML
    private TextField minimumBaruField;
    @FXML
    private Button tambahButton;
    @FXML
    private Label errorTambahLabel;
    @FXML
    private Label barangAktifLabel;
    @FXML
    private TextField minimumAktifField;
    @FXML
    private Button simpanMinimumButton;
    @FXML
    private Label errorMinimumLabel;

    private KonteksFx konteks;
    private boolean memperbarui;

    @FXML
    private void initialize() {
        kolomNama.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getNama()));
        kolomSatuan.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getSatuan()));
        kolomMinimum.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getStokMinimum()
                        + " " + data.getValue().getSatuan()));
        tabelBarang.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabelBarang.setPlaceholder(new Label("Belum ada barang. Tambahkan barang pertama."));
        tabelBarang.getSelectionModel().selectedItemProperty().addListener(
                (obs, lama, baru) -> {
                    if (!memperbarui && baru != null && konteks != null
                            && baru.getId() != konteks.getBarangAktif().getId()) {
                        konteks.pilihBarangAsync(baru);
                    }
                });
        tambahButton.setOnAction(event -> tambah());
        simpanMinimumButton.setOnAction(event -> simpanMinimum());
    }

    @Override
    public void setKonteks(KonteksFx konteks) {
        this.konteks = konteks;
        perbarui();
    }

    private void tambah() {
        errorTambahLabel.setText("");
        String nama = namaBaruField.getText() == null
                ? "" : namaBaruField.getText().trim();
        String satuan = satuanBaruField.getText() == null
                ? "" : satuanBaruField.getText().trim();
        if (nama.isEmpty()) {
            errorTambahLabel.setText("Nama barang wajib diisi.");
            namaBaruField.requestFocus();
            return;
        }
        if (satuan.isEmpty()) {
            errorTambahLabel.setText("Satuan wajib diisi.");
            satuanBaruField.requestFocus();
            return;
        }
        int minimum;
        try {
            minimum = Integer.parseInt(minimumBaruField.getText() == null
                    ? "" : minimumBaruField.getText().trim());
        } catch (NumberFormatException exception) {
            errorTambahLabel.setText("Stok minimum harus berupa angka bulat.");
            minimumBaruField.requestFocus();
            return;
        }
        if (minimum < 0) {
            errorTambahLabel.setText("Stok minimum tidak boleh negatif.");
            minimumBaruField.requestFocus();
            return;
        }

        tambahButton.setDisable(true);
        konteks.tambahBarangAsync(nama, satuan, minimum, sukses -> {
            tambahButton.setDisable(false);
            if (sukses) {
                namaBaruField.clear();
                satuanBaruField.clear();
                minimumBaruField.setText("0");
                errorTambahLabel.setText("");
            } else {
                errorTambahLabel.setText("Tambah barang gagal. Periksa status.");
            }
        });
    }

    private void simpanMinimum() {
        errorMinimumLabel.setText("");
        int minimum;
        try {
            minimum = Integer.parseInt(minimumAktifField.getText() == null
                    ? "" : minimumAktifField.getText().trim());
        } catch (NumberFormatException exception) {
            errorMinimumLabel.setText("Stok minimum harus berupa angka bulat.");
            minimumAktifField.requestFocus();
            return;
        }
        if (minimum < 0) {
            errorMinimumLabel.setText("Stok minimum tidak boleh negatif.");
            minimumAktifField.requestFocus();
            return;
        }

        simpanMinimumButton.setDisable(true);
        konteks.simpanMinimumAsync(minimum, sukses -> {
            simpanMinimumButton.setDisable(false);
            if (!sukses) {
                errorMinimumLabel.setText("Gagal menyimpan minimum. Periksa status.");
            } else {
                errorMinimumLabel.setText("");
            }
        });
    }

    @Override
    public void perbarui() {
        if (konteks == null || konteks.getBarangAktif() == null) {
            return;
        }
        memperbarui = true;
        try {
            tabelBarang.setItems(FXCollections.observableArrayList(
                    konteks.getDaftarBarang()));
            for (Barang item : konteks.getDaftarBarang()) {
                if (item.getId() == konteks.getBarangAktif().getId()) {
                    tabelBarang.getSelectionModel().select(item);
                    break;
                }
            }
            barangAktifLabel.setText("Barang aktif: "
                    + konteks.getBarangAktif().getNama()
                    + " (" + konteks.getBarangAktif().getSatuan() + ")");
            minimumAktifField.setText(String.valueOf(
                    konteks.getBarangAktif().getStokMinimum()));
        } finally {
            memperbarui = false;
        }
    }

    @Override
    public Node getNode() {
        return halaman;
    }
}
