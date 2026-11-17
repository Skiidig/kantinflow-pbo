package kantinflow.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import kantinflow.PenerimaanStok;

/**
 * Halaman penerimaan stok dan daftar batch barang aktif.
 */
public class PanelStokMasuk extends PanelKantin {
    private final JTextField fieldKode = Ui.field(16);
    private final JTextField fieldJumlah = Ui.field(8);
    private final JTextField fieldTanggalMasuk = Ui.field(12);
    private final JTextField fieldTanggalKedaluwarsa = Ui.field(12);
    private final JButton tombolSimpan = Ui.utama("Simpan Penerimaan");
    private final DefaultTableModel model = Ui.model(
            "Kode Batch", "Sisa", "Tanggal Masuk", "Tanggal Kedaluwarsa");
    private final JTable tabel = Ui.tabel(model);

    public PanelStokMasuk(KonteksKantin konteks) {
        super(konteks, "Stok Masuk",
                "Terima batch baru dan lihat batch yang dimiliki barang aktif.");
        fieldKode.setName("kodeBatch");
        fieldJumlah.setName("jumlah");
        fieldTanggalMasuk.setName("tanggalMasuk");
        fieldTanggalKedaluwarsa.setName("tanggalKedaluwarsa");
        tombolSimpan.setName("tombolSimpan");
        tabel.setName("tabelBatch");
        tombolSimpan.addActionListener(event -> simpan());
        aturTanggalAwal();
        bangunIsi();
    }

    private void aturTanggalAwal() {
        LocalDate hariIni = LocalDate.now();
        fieldTanggalMasuk.setText(hariIni.toString());
        fieldTanggalKedaluwarsa.setText(hariIni.plusMonths(1).toString());
    }

    private void bangunIsi() {
        JPanel isi = new JPanel(new BorderLayout(0, 16));
        isi.setOpaque(false);

        JPanel form = Ui.kartu("Form Penerimaan");
        JPanel isian = new JPanel(new GridLayout(2, 2, 16, 10));
        isian.setOpaque(false);
        isian.add(Ui.field("Kode batch", fieldKode));
        isian.add(Ui.field("Jumlah", fieldJumlah));
        isian.add(Ui.field("Tanggal masuk (yyyy-MM-dd)", fieldTanggalMasuk));
        isian.add(Ui.field("Tanggal kedaluwarsa (yyyy-MM-dd)",
                fieldTanggalKedaluwarsa));
        form.add(isian, BorderLayout.CENTER);
        form.add(tombolSimpan, BorderLayout.SOUTH);

        JPanel daftar = Ui.kartu("Batch Barang Aktif");
        daftar.add(Ui.scrollTabel(tabel), BorderLayout.CENTER);

        isi.add(form, BorderLayout.NORTH);
        isi.add(daftar, BorderLayout.CENTER);
        add(isi, BorderLayout.CENTER);
    }

    private void simpan() {
        String kode = fieldKode.getText().trim();
        int jumlah;
        try {
            jumlah = Integer.parseInt(fieldJumlah.getText().trim());
        } catch (NumberFormatException exception) {
            konteks.tampilkanPesan("Jumlah harus berupa angka bulat.", false);
            return;
        }

        LocalDate masuk;
        LocalDate kedaluwarsa;
        try {
            masuk = LocalDate.parse(fieldTanggalMasuk.getText().trim());
            kedaluwarsa = LocalDate.parse(
                    fieldTanggalKedaluwarsa.getText().trim());
        } catch (DateTimeParseException exception) {
            konteks.tampilkanPesan("Format tanggal tidak valid. Gunakan yyyy-MM-dd.",
                    false);
            return;
        }

        PenerimaanStok penerimaan = new PenerimaanStok(masuk);
        try {
            penerimaan.terima(konteks.getBarangAktif(), kode, jumlah, masuk,
                    kedaluwarsa);
        } catch (IllegalArgumentException exception) {
            konteks.muatUlang();
            konteks.tampilkanPesan("Penerimaan gagal: "
                    + exception.getMessage(), false);
            return;
        }

        if (konteks.simpanTransaksi(penerimaan, "Penerimaan berhasil: "
                + kode + ", " + jumlah + " "
                + konteks.getBarangAktif().getSatuan() + ".")) {
            fieldKode.setText("");
            fieldJumlah.setText("");
            aturTanggalAwal();
        }
    }

    @Override
    public void perbarui() {
        model.setRowCount(0);
        for (kantinflow.BatchStok batch : konteks.getBarangAktif().getDaftarBatch()) {
            model.addRow(new Object[]{
                batch.getKode(),
                batch.getSisaJumlah() + " "
                + konteks.getBarangAktif().getSatuan(),
                batch.getTanggalMasuk().toString(),
                batch.getTanggalKedaluwarsa().toString()
            });
        }
    }
}
