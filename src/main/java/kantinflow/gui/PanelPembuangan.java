package kantinflow.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalDate;
import java.util.Objects;

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
import kantinflow.StatusKedaluwarsa;
import kantinflow.StokTidakCukupException;

/**
 * Halaman pembuangan batch kedaluwarsa.
 */
public class PanelPembuangan extends PanelBarangTerpilih {
    private final JComboBox<BatchStok> comboBatch = Ui.combo();
    private final JTextField fieldJumlah = Ui.field(6);
    private final JLabel labelJumlah = Ui.labelField("Jumlah *");
    private final PemilihTanggal tanggalPembuangan = new PemilihTanggal();
    private final JTextField fieldAlasan = Ui.field(16);
    private final JButton tombolBuang = Ui.bahaya("Buang Stok");
    private final JLabel petunjukBatch = Ui.keterangan(" ");
    private final JLabel pesanTanggal = Ui.pesan();
    private final JLabel pesanValidasi = Ui.pesan();
    private final JLabel ringkasan = Ui.kosong(
            "Pilih batch untuk melihat ringkasan tindakan.");
    private LocalDate tanggalAwal;

    public PanelPembuangan(KonteksKantin konteks) {
        super(konteks, "Pembuangan",
                "Buang batch kedaluwarsa dengan alasan.");
        pilihanBarang.setName("barangPembuangan");
        comboBatch.setName("batchBuang");
        fieldJumlah.setName("jumlahBuang");
        tanggalPembuangan.komponenTeks().setName("tanggalBuang");
        pesanTanggal.setName("pesanTanggalBuang");
        fieldAlasan.setName("alasanBuang");
        tombolBuang.setName("tombolBuang");
        tombolBuang.setIcon(Ui.ikonAksi("trash", 16, java.awt.Color.WHITE));
        tombolBuang.setIconTextGap(7);
        tanggalAwal = LocalDate.now();
        tanggalPembuangan.setTanggal(tanggalAwal);
        tanggalPembuangan.tambahPerubahan(() -> {
            pesanTanggal.setText(" ");
            perbaruiRingkasan();
        });
        comboBatch.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                if (value instanceof BatchStok batch) {
                    setText(batch.getKode() + " — sisa "
                            + batch.getSisaJumlah() + " "
                            + (barang == null ? "-" : barang.getSatuan())
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
        JPanel form = Ui.kartu("Form Pembuangan", "trash");
        JPanel isian = Ui.gridForm(2, Ui.AMBANG_FORM_SEMPIT,
                fieldBarang("Barang *"),
                Ui.field("Batch *", comboBatch, petunjukBatch),
                Ui.field(labelJumlah, fieldJumlah),
                Ui.field("Tanggal pembuangan *", tanggalPembuangan,
                        pesanTanggal),
                Ui.field("Alasan *", fieldAlasan));
        JPanel formIsi = new JPanel();
        formIsi.setOpaque(false);
        formIsi.setLayout(new javax.swing.BoxLayout(
                formIsi, javax.swing.BoxLayout.Y_AXIS));
        isian.setAlignmentX(LEFT_ALIGNMENT);
        formIsi.add(isian);
        formIsi.add(javax.swing.Box.createVerticalStrut(6));
        pesanValidasi.setAlignmentX(LEFT_ALIGNMENT);
        formIsi.add(pesanValidasi);
        JPanel aksi = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        aksi.setOpaque(false);
        aksi.setAlignmentX(LEFT_ALIGNMENT);
        aksi.add(tombolBuang);
        formIsi.add(aksi);
        form.add(formIsi, BorderLayout.CENTER);

        JPanel tindakan = Ui.kartu("Ringkasan tindakan", "alert-triangle");
        JPanel tindakanIsi = new JPanel(new BorderLayout(0, 6));
        tindakanIsi.setOpaque(false);
        ringkasan.setFont(Ui.font(Font.BOLD, Ui.FONT_CATATAN));
        ringkasan.setForeground(Ui.TEKS_LEMBUT);
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

    private void perbaruiLabelJumlah() {
        labelJumlah.setText(Ui.teksJumlah(barang));
    }

    private void perbaruiPetunjukBatch() {
        if (barang == null) {
            petunjukBatch.setText("Pilih barang terlebih dahulu.");
            return;
        }
        if (comboBatch.getItemCount() == 0) {
            petunjukBatch.setText(
                    "Tidak ada batch bersisa untuk barang ini.");
            return;
        }
        BatchStok batch = (BatchStok) comboBatch.getSelectedItem();
        if (batch == null) {
            petunjukBatch.setText("Pilih batch untuk melihat sisa.");
            return;
        }
        petunjukBatch.setText("Sisa batch terpilih: "
                + batch.getSisaJumlah() + " " + barang.getSatuan() + ". "
                + kelayakanBatch(batch));
    }

    private String kelayakanBatch(BatchStok batch) {
        LocalDate dipilih = tanggalPembuangan.getTanggal();
        if (dipilih == null) {
            return "Isi tanggal pembuangan untuk memeriksa kelayakan.";
        }
        if (dipilih.isBefore(batch.getTanggalMasuk())) {
            return "Tanggal pembuangan sebelum tanggal masuk batch.";
        }
        if (batch.statusPada(dipilih) != StatusKedaluwarsa.KEDALUWARSA) {
            return "Batch belum kedaluwarsa pada tanggal pembuangan.";
        }
        return "Batch kedaluwarsa pada tanggal pembuangan.";
    }

    private String ringkasanKelayakan(BatchStok batch) {
        LocalDate dipilih = tanggalPembuangan.getTanggal();
        if (dipilih == null) {
            return "tanggal belum diisi";
        }
        if (dipilih.isBefore(batch.getTanggalMasuk())) {
            return "sebelum tanggal masuk";
        }
        if (batch.statusPada(dipilih) != StatusKedaluwarsa.KEDALUWARSA) {
            return "belum layak";
        }
        return "layak dibuang";
    }

    private void perbaruiRingkasan() {
        perbaruiPetunjukBatch();
        BatchStok batch = (BatchStok) comboBatch.getSelectedItem();
        if (barang == null) {
            ringkasan.setForeground(Ui.TEKS_LEMBUT);
            ringkasan.setText(
                    "Belum ada barang. Tambahkan melalui halaman Barang.");
            return;
        }
        if (batch == null) {
            ringkasan.setForeground(Ui.TEKS_LEMBUT);
            ringkasan.setText("Pilih batch untuk melihat ringkasan tindakan.");
            return;
        }
        ringkasan.setForeground(Ui.TEKS);
        ringkasan.setText("Buang " + batch.getKode() + " · sisa "
                + batch.getSisaJumlah() + " " + barang.getSatuan()
                + " · kedaluwarsa " + batch.getTanggalKedaluwarsa()
                + " · " + ringkasanKelayakan(batch));
    }

    private void buang() {
        pesanValidasi.setText(" ");
        pesanTanggal.setText(" ");
        if (barang == null) {
            pesanValidasi.setText(
                    "Belum ada barang. Tambahkan melalui halaman Barang.");
            return;
        }
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
        LocalDate tanggal = tanggalPembuangan.getTanggal();
        if (tanggal == null) {
            boolean kosong = tanggalPembuangan.komponenTeks()
                    .getText().trim().isEmpty();
            pesanTanggal.setText(kosong
                    ? "Tanggal pembuangan wajib diisi."
                    : "Format tanggal pembuangan tidak valid.");
            tanggalPembuangan.komponenTeks().requestFocusInWindow();
            return;
        }
        if (fieldAlasan.getText().trim().isEmpty()) {
            pesanValidasi.setText("Alasan pembuangan wajib diisi.");
            fieldAlasan.requestFocusInWindow();
            return;
        }

        if (!DialogKonfirmasi.tampilkan(this, "Buang stok kedaluwarsa",
                "Batch " + batch.getKode() + " sebanyak " + jumlah + " "
                + barang.getSatuan() + " akan dibuang. "
                + "Tindakan ini mengurangi sisa batch dan tercatat di riwayat.",
                "Buang Stok")) {
            return;
        }

        PembuanganStok pembuangan = new PembuanganStok(tanggal);
        try {
            pembuangan.buang(barang, batch, jumlah, fieldAlasan.getText());
        } catch (StokTidakCukupException | IllegalArgumentException exception) {
            konteks.muatUlang();
            pesanValidasi.setText("Pembuangan ditolak: " + exception.getMessage());
            konteks.tampilkanPesan("Pembuangan gagal: "
                    + exception.getMessage(), false);
            return;
        }

        if (konteks.simpanTransaksi(barang, pembuangan,
                "Pembuangan berhasil: " + batch.getKode() + ", " + jumlah
                + " " + barang.getSatuan() + " pada " + tanggal
                + ". Alasan: " + pembuangan.getAlasan() + ".")) {
            fieldJumlah.setText("");
            fieldAlasan.setText("");
            tanggalAwal = LocalDate.now();
            tanggalPembuangan.setTanggal(tanggalAwal);
            pesanTanggal.setText(" ");
            pesanValidasi.setText(" ");
        } else {
            pesanValidasi.setText("Pembuangan gagal disimpan. Lihat status.");
        }
    }

    @Override
    protected void saatBarangBerubah() {
        fieldJumlah.setText("");
        fieldAlasan.setText("");
        pesanValidasi.setText(" ");
        pesanTanggal.setText(" ");
        perbaruiLabelJumlah();
        comboBatch.removeAllItems();
        tanggalAwal = LocalDate.now();
        tanggalPembuangan.setTanggal(tanggalAwal);
    }

    @Override
    public void perbarui() {
        boolean adaBarang = perbaruiBarangTerpilih();
        perbaruiLabelJumlah();
        if (!adaBarang) {
            comboBatch.removeAllItems();
            perbaruiPetunjukBatch();
            tombolBuang.setEnabled(false);
            ringkasan.setForeground(Ui.TEKS_LEMBUT);
            ringkasan.setText(
                    "Belum ada barang. Tambahkan melalui halaman Barang.");
            return;
        }
        String kodeTerpilih = comboBatch.getItemCount() == 0
                ? null
                : ((BatchStok) comboBatch.getSelectedItem()).getKode();
        comboBatch.removeAllItems();
        for (BatchStok batch : barang.getDaftarBatch()) {
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

    @Override
    public boolean adaIsianBelumDisimpan() {
        return !fieldJumlah.getText().trim().isEmpty()
                || !fieldAlasan.getText().trim().isEmpty()
                || !Objects.equals(
                        tanggalPembuangan.getTanggal(), tanggalAwal);
    }
}
