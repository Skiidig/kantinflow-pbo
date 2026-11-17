package kantinflow.gui;

import java.awt.BorderLayout;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import kantinflow.BatchStok;
import kantinflow.StatusKedaluwarsa;

/**
 * Halaman pemantauan kedaluwarsa batch bersisa.
 */
public class PanelPemantauan extends PanelKantin {
    private final JTextField fieldTanggal = Ui.field(11);
    private final JButton tombolPeriksa = Ui.utama("Periksa Kedaluwarsa");
    private final DefaultTableModel model = Ui.model(
            "Kode Batch", "Sisa", "Tanggal Kedaluwarsa", "Status");
    private final JTable tabel = Ui.tabel(model);
    private final javax.swing.JLabel labelInfo = Ui.keterangan(
            "Belum ada tanggal pemeriksaan.");

    public PanelPemantauan(KonteksKantin konteks) {
        super(konteks, "Pemantauan",
                "Periksa batch bersisa berdasarkan status kedaluwarsa.");
        fieldTanggal.setName("tanggalPemantauan");
        tombolPeriksa.setName("tombolPeriksa");
        tabel.setName("tabelPemantauan");
        tombolPeriksa.addActionListener(event -> periksa());
        bangunIsi();
    }

    private void bangunIsi() {
        JPanel isi = new JPanel(new BorderLayout(0, 16));
        isi.setOpaque(false);

        JPanel kontrol = Ui.kartu();
        JPanel baris = new JPanel(new BorderLayout(0, 6));
        baris.setOpaque(false);
        JPanel input = new JPanel(new java.awt.FlowLayout(
                java.awt.FlowLayout.LEFT, 8, 0));
        input.setOpaque(false);
        input.add(Ui.labelField("Tanggal pengecekan (yyyy-MM-dd):"));
        input.add(fieldTanggal);
        input.add(tombolPeriksa);
        baris.add(input, BorderLayout.NORTH);
        baris.add(labelInfo, BorderLayout.SOUTH);
        kontrol.add(baris, BorderLayout.CENTER);

        JPanel daftar = Ui.kartu("Status Batch");
        daftar.add(Ui.scrollTabel(tabel), BorderLayout.CENTER);

        isi.add(kontrol, BorderLayout.NORTH);
        isi.add(daftar, BorderLayout.CENTER);
        add(isi, BorderLayout.CENTER);
    }

    private void periksa() {
        try {
            LocalDate tanggal = LocalDate.parse(fieldTanggal.getText().trim());
            konteks.setTanggalPemantauan(tanggal);
            konteks.tampilkanPesan("Pemantauan " + tanggal
                    + " selesai: " + model.getRowCount()
                    + " batch bersisa diperiksa.", true);
        } catch (DateTimeParseException exception) {
            konteks.tampilkanPesan("Format tanggal tidak valid. Gunakan yyyy-MM-dd.",
                    false);
        }
    }

    @Override
    public void perbarui() {
        LocalDate tanggal = konteks.getTanggalPemantauan();
        if (tanggal == null) {
            if (fieldTanggal.getText().isBlank()) {
                fieldTanggal.setText(LocalDate.now().toString());
            }
            model.setRowCount(0);
            labelInfo.setText("Belum diperiksa. Isi tanggal lalu klik Periksa.");
            return;
        }

        fieldTanggal.setText(tanggal.toString());
        model.setRowCount(0);
        for (BatchStok batch : konteks.getBarangAktif().getDaftarBatch()) {
            if (batch.getSisaJumlah() <= 0) {
                continue;
            }
            StatusKedaluwarsa status = batch.statusPada(tanggal);
            model.addRow(new Object[]{
                batch.getKode(),
                batch.getSisaJumlah() + " "
                + konteks.getBarangAktif().getSatuan(),
                batch.getTanggalKedaluwarsa().toString(),
                status.getLabel()
            });
        }
        labelInfo.setText("Diperiksa pada " + tanggal + ". Hanya batch bersisa "
                + "yang ditampilkan.");
    }
}
