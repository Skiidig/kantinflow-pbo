package kantinflow.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import kantinflow.BatchStok;
import kantinflow.PembuanganStok;
import kantinflow.StokTidakCukupException;

/**
 * Halaman pembuangan batch kedaluwarsa.
 */
public class PanelPembuangan extends PanelKantin {
    private final JComboBox<BatchStok> comboBatch = Ui.combo();
    private final JTextField fieldJumlah = Ui.field(6);
    private final JTextField fieldTanggal = Ui.field(10);
    private final JTextField fieldAlasan = Ui.field(16);
    private final JButton tombolBuang = Ui.bahaya("Buang Stok");
    private final JLabel pesanValidasi = Ui.pesan();
    private final JLabel ringkasan = Ui.kosong(
            "Pilih batch untuk melihat ringkasan tindakan.");

    public PanelPembuangan(KonteksKantin konteks) {
        super(konteks, "Pembuangan",
                "Buang batch kedaluwarsa dengan alasan.");
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
        comboBatch.addActionListener(event -> perbaruiRingkasan());
        tombolBuang.addActionListener(event -> buang());
        bangunIsi();
    }

    private void bangunIsi() {
        JPanel form = Ui.kartu("Form Pembuangan");
        JPanel isian = Ui.gridResponsif(2, Ui.AMBANG_FORM_SEMPIT,
                Ui.field("Batch *", comboBatch),
                Ui.field("Jumlah *", fieldJumlah),
                Ui.field("Tanggal pembuangan * (yyyy-MM-dd)", fieldTanggal),
                Ui.field("Alasan *", fieldAlasan));
        JPanel formIsi = new JPanel();
        formIsi.setOpaque(false);
        formIsi.setLayout(new javax.swing.BoxLayout(
                formIsi, javax.swing.BoxLayout.Y_AXIS));
        formIsi.add(isian);
        formIsi.add(javax.swing.Box.createVerticalStrut(6));
        formIsi.add(pesanValidasi);
        JPanel aksi = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        aksi.setOpaque(false);
        aksi.add(tombolBuang);
        formIsi.add(aksi);
        form.add(formIsi, BorderLayout.CENTER);

        JPanel tindakan = Ui.kartu("Ringkasan tindakan");
        JPanel tindakanIsi = new JPanel(new BorderLayout(0, 6));
        tindakanIsi.setOpaque(false);
        ringkasan.setFont(Ui.font(Font.BOLD, Ui.FONT_CATATAN));
        ringkasan.setForeground(Ui.MERAH);
        tindakanIsi.add(ringkasan, BorderLayout.NORTH);
        JLabel catatan = Ui.keterangan(
                "Hanya batch berstatus Kedaluwarsa pada tanggal pembuangan yang "
                + "dapat dibuang. Tindakan ini mengurangi sisa batch dan tercatat "
                + "di riwayat.");
        catatan.setBorder(new EmptyBorder(4, 0, 0, 0));
        tindakanIsi.add(catatan, BorderLayout.CENTER);
        tindakan.add(tindakanIsi, BorderLayout.CENTER);

        JPanel isi = new JPanel(new BorderLayout(0, 16));
        isi.setOpaque(false);
        isi.add(form, BorderLayout.NORTH);
        isi.add(tindakan, BorderLayout.CENTER);
        add(isi, BorderLayout.CENTER);
    }

    private void perbaruiRingkasan() {
        BatchStok batch = (BatchStok) comboBatch.getSelectedItem();
        if (batch == null) {
            ringkasan.setText("Pilih batch untuk melihat ringkasan tindakan.");
            return;
        }
        ringkasan.setText("Buang " + batch.getKode() + " · sisa "
                + batch.getSisaJumlah() + " "
                + konteks.getBarangAktif().getSatuan()
                + " · kedaluwarsa " + batch.getTanggalKedaluwarsa());
    }

    private void buang() {
        pesanValidasi.setText(" ");
        BatchStok batch = (BatchStok) comboBatch.getSelectedItem();
        if (batch == null) {
            pesanValidasi.setText("Pilih batch terlebih dahulu.");
            comboBatch.requestFocusInWindow();
            return;
        }
        int jumlah;
        try {
            jumlah = Integer.parseInt(fieldJumlah.getText().trim());
        } catch (NumberFormatException exception) {
            pesanValidasi.setText("Jumlah harus berupa angka bulat.");
            fieldJumlah.requestFocusInWindow();
            return;
        }
        LocalDate tanggal;
        try {
            tanggal = LocalDate.parse(fieldTanggal.getText().trim());
        } catch (DateTimeParseException exception) {
            pesanValidasi.setText("Format tanggal harus yyyy-MM-dd.");
            fieldTanggal.requestFocusInWindow();
            return;
        }
        if (fieldAlasan.getText().trim().isEmpty()) {
            pesanValidasi.setText("Alasan pembuangan wajib diisi.");
            fieldAlasan.requestFocusInWindow();
            return;
        }

        PembuanganStok pembuangan = new PembuanganStok(tanggal);
        try {
            pembuangan.buang(konteks.getBarangAktif(), batch, jumlah,
                    fieldAlasan.getText());
        } catch (StokTidakCukupException | IllegalArgumentException exception) {
            konteks.muatUlang();
            pesanValidasi.setText("Pembuangan ditolak: " + exception.getMessage());
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
            pesanValidasi.setText(" ");
        } else {
            pesanValidasi.setText("Pembuangan gagal disimpan. Lihat status.");
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
        perbaruiRingkasan();
    }
}
