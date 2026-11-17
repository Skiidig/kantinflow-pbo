package kantinflow.fx;

import java.time.LocalDate;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import kantinflow.Barang;

public class RingkasanController implements HalamanFx {
    @FXML
    private VBox halaman;
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

    private KonteksFx konteks;

    @FXML
    private void initialize() {
        tanggalPicker.setValue(LocalDate.now());
        periksaButton.setOnAction(event -> periksa());
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
            konteks.tampilkanPesan("Tanggal pengecekan wajib diisi.", false);
            return;
        }
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

        fisikLabel.setText(fisik + " " + satuan);
        layakLabel.setText(layak + " " + satuan);
        minimumLabel.setText(minimum + " " + satuan);

        boolean perluRestok = layak <= minimum;
        statusRestokLabel.setText(perluRestok ? "Perlu restok" : "Cukup");
        statusRestokLabel.getStyleClass().removeAll("status-restock", "status-cukup");
        statusRestokLabel.getStyleClass().add(
                perluRestok ? "status-restock" : "status-cukup");
    }

    @Override
    public Node getNode() {
        return halaman;
    }
}
