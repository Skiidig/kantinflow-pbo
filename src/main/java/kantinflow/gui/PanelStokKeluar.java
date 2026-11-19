package kantinflow.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import kantinflow.BatchStok;
import kantinflow.PengeluaranStok;
import kantinflow.RincianTransaksi;
import kantinflow.StokTidakCukupException;

/**
 * Halaman pengeluaran manual dan FEFO untuk barang terpilih.
 */
public class PanelStokKeluar extends PanelBarangTerpilih {
    private final JComboBox<BatchStok> comboBatch = Ui.combo();
    private final JLabel labelSisa = new JLabel("-");
    private final JTextField fieldJumlahManual = Ui.field(8);
    private final JLabel labelJumlahManual = Ui.labelField("Jumlah *");
    private final JButton tombolManual = Ui.utama("Keluarkan Manual");
    private final JTextField fieldJumlahFefo = Ui.field(8);
    private final JLabel labelJumlahFefo = Ui.labelField("Jumlah *");
    private final JTextField fieldTanggalFefo = Ui.field(12);
    private final JButton tombolFefo = Ui.utama("Keluarkan FEFO");
    private final JLabel pesanManual = Ui.pesan();
    private final JLabel pesanFefo = Ui.pesan();
    private final JLabel hasilAlokasi = Ui.keterangan(
            "Alokasi FEFO akan tampil di sini setelah transaksi berhasil.");
    private String tanggalFefoAwal;

    public PanelStokKeluar(KonteksKantin konteks) {
        super(konteks, "Stok Keluar",
                "Keluarkan stok manual atau dengan FEFO.");
        pilihanBarang.setName("barangStokKeluar");
        comboBatch.setName("batchManual");
        fieldJumlahManual.setName("jumlahKeluar");
        tombolManual.setName("tombolKeluarkan");
        fieldJumlahFefo.setName("jumlahFefo");
        fieldTanggalFefo.setName("tanggalFefo");
        fieldTanggalFefo.putClientProperty("JTextField.placeholderText",
                "yyyy-MM-dd");
        tombolFefo.setName("tombolFefo");
        tombolFefo.setIcon(Ui.ikonAksi("stock-out", 16, java.awt.Color.WHITE));
        tombolFefo.setIconTextGap(7);
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
        comboBatch.addActionListener(event -> perbaruiSisa());
        tombolManual.addActionListener(event -> keluarkanManual());
        tombolFefo.addActionListener(event -> keluarkanFefo());
        tanggalFefoAwal = LocalDate.now().toString();
        fieldTanggalFefo.setText(tanggalFefoAwal);
        bangunIsi();
    }

    private void bangunIsi() {
        JPanel manualIsi = isiMode();
        manualIsi.add(bantuanMode(
                "Pilih batch sumber, lalu masukkan jumlah yang dikeluarkan."));
        JPanel formManual = Ui.gridForm(2, Ui.AMBANG_FORM_SEMPIT,
                Ui.field("Batch *", comboBatch),
                Ui.field(labelJumlahManual, fieldJumlahManual));
        formManual.setAlignmentX(LEFT_ALIGNMENT);
        manualIsi.add(formManual);

        labelSisa.setName("sisaBatch");
        siapkanInfoMode(labelSisa);
        manualIsi.add(labelSisa);

        pesanManual.setAlignmentX(LEFT_ALIGNMENT);
        manualIsi.add(pesanManual);
        manualIsi.add(javax.swing.Box.createVerticalStrut(6));
        manualIsi.add(aksiKiri(tombolManual));

        JPanel fefoIsi = isiMode();
        fefoIsi.add(bantuanMode(
                "Stok layak dengan tanggal kedaluwarsa terdekat digunakan lebih dahulu."));

        JPanel formFefo = Ui.gridForm(2, Ui.AMBANG_FORM_SEMPIT,
                Ui.field(labelJumlahFefo, fieldJumlahFefo),
                Ui.field("Tanggal pengeluaran * (yyyy-MM-dd)",
                        fieldTanggalFefo));
        formFefo.setAlignmentX(LEFT_ALIGNMENT);
        fefoIsi.add(formFefo);

        siapkanInfoMode(hasilAlokasi);
        fefoIsi.add(hasilAlokasi);

        pesanFefo.setAlignmentX(LEFT_ALIGNMENT);
        fefoIsi.add(pesanFefo);
        fefoIsi.add(javax.swing.Box.createVerticalStrut(6));
        fefoIsi.add(aksiKiri(tombolFefo));

        JTabbedPane mode = new JTabbedPane();
        mode.setFont(Ui.font(Font.BOLD, Ui.FONT_CATATAN));
        mode.setBackground(Ui.KARTU);
        mode.setOpaque(true);
        mode.addTab("Manual", manualIsi);
        mode.addTab("Otomatis (FEFO)", fefoIsi);

        JPanel isi = new JPanel();
        isi.setOpaque(false);
        isi.setLayout(new javax.swing.BoxLayout(
                isi, javax.swing.BoxLayout.Y_AXIS));
        JPanel pilihan = fieldBarang("Barang *");
        pilihan.setAlignmentX(LEFT_ALIGNMENT);
        pilihan.setBorder(new EmptyBorder(0, 0, 10, 0));
        isi.add(pilihan);
        mode.setAlignmentX(LEFT_ALIGNMENT);
        isi.add(mode);

        JPanel kartu = Ui.kartu("Pengeluaran Stok", "stock-out");
        kartu.add(isi, BorderLayout.CENTER);
        add(kartu, BorderLayout.NORTH);
    }

    private JLabel bantuanMode(String teks) {
        JLabel label = Ui.keterangan(teks);
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.setBorder(new EmptyBorder(0, 0, 10, 0));
        return label;
    }

    private void siapkanInfoMode(JLabel label) {
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.setFont(Ui.font(Font.PLAIN, Ui.FONT_KECIL));
        label.setForeground(Ui.TEKS_LEMBUT);
        label.setBorder(new EmptyBorder(6, 0, 0, 0));
    }

    private JPanel isiMode() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new javax.swing.BoxLayout(
                panel, javax.swing.BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(16, 0, 0, 0));
        return panel;
    }

    private JPanel aksiKiri(JButton tombol) {
        JPanel panel = new JPanel(new java.awt.FlowLayout(
                java.awt.FlowLayout.LEFT, 0, 0));
        panel.setOpaque(false);
        panel.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(tombol);
        return panel;
    }

    private void perbaruiSisa() {
        BatchStok batch = (BatchStok) comboBatch.getSelectedItem();
        if (barang == null) {
            labelSisa.setText(
                    "Belum ada barang. Tambahkan melalui halaman Barang.");
            labelSisa.setForeground(Ui.TEKS_LEMBUT);
            tombolManual.setEnabled(false);
            return;
        }
        if (batch == null) {
            labelSisa.setText("Tidak ada batch bersisa.");
            labelSisa.setForeground(Ui.TEKS_LEMBUT);
            tombolManual.setEnabled(false);
        } else {
            labelSisa.setText("Sisa: " + batch.getSisaJumlah() + " "
                    + barang.getSatuan() + " pada batch " + batch.getKode());
            labelSisa.setForeground(Ui.AKSEN);
            tombolManual.setEnabled(true);
        }
    }

    private void keluarkanManual() {
        pesanManual.setText(" ");
        if (barang == null) {
            pesanManual.setText(
                    "Belum ada barang. Tambahkan melalui halaman Barang.");
            return;
        }
        BatchStok batch = (BatchStok) comboBatch.getSelectedItem();
        if (batch == null) {
            pesanManual.setText("Pilih batch terlebih dahulu.");
            comboBatch.requestFocusInWindow();
            return;
        }
        int jumlah;
        try {
            jumlah = Integer.parseInt(fieldJumlahManual.getText().trim());
        } catch (NumberFormatException exception) {
            pesanManual.setText("Jumlah harus berupa angka bulat.");
            fieldJumlahManual.requestFocusInWindow();
            return;
        }

        PengeluaranStok pengeluaran = new PengeluaranStok(LocalDate.now());
        try {
            pengeluaran.keluarkan(barang, batch, jumlah);
        } catch (StokTidakCukupException | IllegalArgumentException exception) {
            konteks.muatUlang();
            pesanManual.setText("Pengeluaran ditolak: " + exception.getMessage());
            konteks.tampilkanPesan("Pengeluaran gagal: "
                    + exception.getMessage(), false);
            return;
        }

        if (konteks.simpanTransaksi(barang, pengeluaran,
                "Pengeluaran berhasil: " + batch.getKode() + ", " + jumlah
                + " " + barang.getSatuan() + ".")) {
            fieldJumlahManual.setText("");
            pesanManual.setText(" ");
        } else {
            pesanManual.setText("Pengeluaran gagal disimpan. Lihat status.");
        }
    }

    private void keluarkanFefo() {
        pesanFefo.setText(" ");
        if (barang == null) {
            pesanFefo.setText(
                    "Belum ada barang. Tambahkan melalui halaman Barang.");
            return;
        }
        int jumlah;
        try {
            jumlah = Integer.parseInt(fieldJumlahFefo.getText().trim());
        } catch (NumberFormatException exception) {
            pesanFefo.setText("Jumlah harus berupa angka bulat.");
            fieldJumlahFefo.requestFocusInWindow();
            return;
        }

        LocalDate tanggal;
        try {
            tanggal = LocalDate.parse(fieldTanggalFefo.getText().trim());
        } catch (DateTimeParseException exception) {
            pesanFefo.setText("Format tanggal harus yyyy-MM-dd.");
            fieldTanggalFefo.requestFocusInWindow();
            return;
        }

        PengeluaranStok pengeluaran = new PengeluaranStok(tanggal);
        try {
            pengeluaran.keluarkanFefo(barang, jumlah, tanggal);
        } catch (StokTidakCukupException | IllegalArgumentException exception) {
            konteks.muatUlang();
            pesanFefo.setText("Pengeluaran ditolak: " + exception.getMessage());
            konteks.tampilkanPesan("Pengeluaran FEFO gagal: "
                    + exception.getMessage(), false);
            return;
        }

        String alokasi = formatAlokasi(pengeluaran);
        if (konteks.simpanTransaksi(barang, pengeluaran,
                "Pengeluaran FEFO berhasil: " + alokasi + ". Saldo: "
                + barang.hitungSaldo() + " " + barang.getSatuan() + ".")) {
            fieldJumlahFefo.setText("");
            fieldTanggalFefo.setText(LocalDate.now().toString());
            pesanFefo.setText(" ");
            hasilAlokasi.setText("Alokasi: " + alokasi);
        } else {
            pesanFefo.setText("Pengeluaran gagal disimpan. Lihat status.");
        }
    }

    private String formatAlokasi(PengeluaranStok pengeluaran) {
        StringBuilder hasil = new StringBuilder();
        for (RincianTransaksi rincian : pengeluaran.getRincian()) {
            if (hasil.length() > 0) {
                hasil.append(", ");
            }
            hasil.append(rincian.getBatch().getKode())
                    .append(" ")
                    .append(rincian.getJumlah())
                    .append(" ")
                    .append(barang == null ? "" : barang.getSatuan());
        }
        return hasil.toString();
    }

    private void perbaruiLabelJumlah() {
        String teks = Ui.teksJumlah(barang);
        labelJumlahManual.setText(teks);
        labelJumlahFefo.setText(teks);
    }

    @Override
    protected void saatBarangBerubah() {
        fieldJumlahManual.setText("");
        fieldJumlahFefo.setText("");
        perbaruiLabelJumlah();
        tanggalFefoAwal = LocalDate.now().toString();
        fieldTanggalFefo.setText(tanggalFefoAwal);
        pesanManual.setText(" ");
        pesanFefo.setText(" ");
        hasilAlokasi.setText(
                "Alokasi FEFO akan tampil di sini setelah transaksi berhasil.");
        comboBatch.removeAllItems();
    }

    @Override
    public void perbarui() {
        boolean adaBarang = perbaruiBarangTerpilih();
        perbaruiLabelJumlah();
        if (!adaBarang) {
            comboBatch.removeAllItems();
            labelSisa.setText(
                    "Belum ada barang. Tambahkan melalui halaman Barang.");
            labelSisa.setForeground(Ui.TEKS_LEMBUT);
            tombolManual.setEnabled(false);
            tombolFefo.setEnabled(false);
            return;
        }
        tombolFefo.setEnabled(true);
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
        perbaruiSisa();
    }

    @Override
    public boolean adaIsianBelumDisimpan() {
        return !fieldJumlahManual.getText().trim().isEmpty()
                || !fieldJumlahFefo.getText().trim().isEmpty()
                || !fieldTanggalFefo.getText().equals(tanggalFefoAwal);
    }
}
