package kantinflow.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;

import kantinflow.BatchStok;
import kantinflow.PembuanganStok;
import kantinflow.StokTidakCukupException;

/**
 * Halaman pembuangan batch kedaluwarsa.
 */
public class PanelPembuangan extends PanelKantin {
    private final JComboBox<BatchStok> comboBatch = new JComboBox<>();
    private final JTextField fieldJumlah = Ui.field(8);
    private final JTextField fieldTanggal = Ui.field(12);
    private final JTextField fieldAlasan = Ui.field(24);
    private final JButton tombolBuang = Ui.utama("Buang Stok");

    public PanelPembuangan(KonteksKantin konteks) {
        super(konteks, "Pembuangan",
                "Buang batch yang sudah kedaluwarsa pada tanggal pembuangan.");
        comboBatch.setName("batchBuang");
        fieldJumlah.setName("jumlahBuang");
        fieldTanggal.setName("tanggalBuang");
        fieldAlasan.setName("alasanBuang");
        tombolBuang.setName("tombolBuang");
        fieldTanggal.setText(LocalDate.now().toString());
        comboBatch.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                if (value instanceof BatchStok batch) {
                    setText(batch.getKode() + " — sisa "
                            + batch.getSisaJumlah() + " "
                            + konteks.getBarangAktif().getSatuan()
                            + " — kedaluwarsa "
                            + batch.getTanggalKedaluwarsa());
                }
                return this;
            }
        });
        tombolBuang.addActionListener(event -> buang());
        bangunIsi();
    }

    private void bangunIsi() {
        JPanel form = Ui.kartu("Form Pembuangan");
        JPanel isian = new JPanel(new GridLayout(2, 2, 16, 10));
        isian.setOpaque(false);
        isian.add(Ui.field("Batch", comboBatch));
        isian.add(Ui.field("Jumlah", fieldJumlah));
        isian.add(Ui.field("Tanggal pembuangan (yyyy-MM-dd)", fieldTanggal));
        isian.add(Ui.field("Alasan", fieldAlasan));
        form.add(isian, BorderLayout.CENTER);
        form.add(tombolBuang, BorderLayout.SOUTH);

        JPanel catatan = Ui.kartu("Ketentuan");
        catatan.add(Ui.keterangan(
                "Hanya batch berstatus Kedaluwarsa pada tanggal pembuangan yang "
                + "dapat dibuang. Batch yang kedaluwarsa tepat pada tanggal "
                + "tersebut belum memenuhi syarat."), BorderLayout.CENTER);

        JPanel isi = new JPanel(new BorderLayout(0, 16));
        isi.setOpaque(false);
        isi.add(form, BorderLayout.NORTH);
        isi.add(catatan, BorderLayout.CENTER);
        add(isi, BorderLayout.CENTER);
    }

    private void buang() {
        BatchStok batch = (BatchStok) comboBatch.getSelectedItem();
        if (batch == null) {
            konteks.tampilkanPesan("Pilih batch terlebih dahulu.", false);
            return;
        }
        int jumlah;
        try {
            jumlah = Integer.parseInt(fieldJumlah.getText().trim());
        } catch (NumberFormatException exception) {
            konteks.tampilkanPesan("Jumlah harus berupa angka bulat.", false);
            return;
        }
        LocalDate tanggal;
        try {
            tanggal = LocalDate.parse(fieldTanggal.getText().trim());
        } catch (DateTimeParseException exception) {
            konteks.tampilkanPesan("Format tanggal tidak valid. Gunakan yyyy-MM-dd.",
                    false);
            return;
        }

        PembuanganStok pembuangan = new PembuanganStok(tanggal);
        try {
            pembuangan.buang(konteks.getBarangAktif(), batch, jumlah,
                    fieldAlasan.getText());
        } catch (StokTidakCukupException | IllegalArgumentException exception) {
            konteks.muatUlang();
            konteks.tampilkanPesan("Pembuangan gagal: "
                    + exception.getMessage(), false);
            return;
        }

        if (konteks.simpanTransaksi(pembuangan, "Pembuangan berhasil: "
                + batch.getKode() + ", " + jumlah + " "
                + konteks.getBarangAktif().getSatuan() + " pada " + tanggal
                + ". Alasan: " + pembuangan.getAlasan() + ".")) {
            fieldJumlah.setText("");
            fieldAlasan.setText("");
            fieldTanggal.setText(LocalDate.now().toString());
        }
    }

    @Override
    public void perbarui() {
        String kodeTerpilih = comboBatch.getItemCount() == 0
                ? null
                : ((BatchStok) comboBatch.getSelectedItem()).getKode();
        comboBatch.removeAllItems();
        for (BatchStok batch : konteks.getBarangAktif().getDaftarBatch()) {
            if (batch.getSisaJumlah() > 0) {
                comboBatch.addItem(batch);
            }
        }
        if (kodeTerpilih != null) {
            for (int i = 0; i < comboBatch.getItemCount(); i++) {
                if (kodeTerpilih.equals(comboBatch.getItemAt(i).getKode())) {
                    comboBatch.setSelectedIndex(i);
                    break;
                }
            }
        }
        tombolBuang.setEnabled(comboBatch.getItemCount() > 0);
    }
}
