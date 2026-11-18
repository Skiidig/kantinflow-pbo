package kantinflow.gui;

import java.awt.BorderLayout;
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
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import kantinflow.BatchStok;
import kantinflow.PengeluaranStok;
import kantinflow.RincianTransaksi;
import kantinflow.StokTidakCukupException;

/**
 * Halaman pengeluaran manual dan FEFO untuk barang aktif.
 */
public class PanelStokKeluar extends PanelKantin {
    private final JComboBox<BatchStok> comboBatch = Ui.combo();
    private final JLabel labelSisa = new JLabel("-");
    private final JTextField fieldJumlahManual = Ui.field(8);
    private final JButton tombolManual = Ui.utama("Keluarkan Manual");
    private final JTextField fieldJumlahFefo = Ui.field(8);
    private final JTextField fieldTanggalFefo = Ui.field(12);
    private final JButton tombolFefo = Ui.utama("Keluarkan FEFO");
    private final JLabel pesanManual = Ui.pesan();
    private final JLabel pesanFefo = Ui.pesan();
    private final JLabel hasilAlokasi = Ui.kosong(
            "Alokasi FEFO akan tampil di sini setelah transaksi berhasil.");
    private final JLabel infoBarang = Ui.keterangan(" ");
    private int idBarangTerakhir = -1;
    private String tanggalFefoAwal;

    public PanelStokKeluar(KonteksKantin konteks) {
        super(konteks, "Stok Keluar",
                "Keluarkan stok manual atau dengan FEFO.");
        comboBatch.setName("batchManual");
        infoBarang.setName("konteksStokKeluar");
        fieldJumlahManual.setName("jumlahKeluar");
        tombolManual.setName("tombolKeluarkan");
        fieldJumlahFefo.setName("jumlahFefo");
        fieldTanggalFefo.setName("tanggalFefo");
        tombolFefo.setName("tombolFefo");
        tombolFefo.setIcon(Ui.ikonAksi("stock-out", 16, java.awt.Color.WHITE));
        tombolFefo.setIconTextGap(7);
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
        comboBatch.addActionListener(event -> perbaruiSisa());
        tombolManual.addActionListener(event -> keluarkanManual());
        tombolFefo.addActionListener(event -> keluarkanFefo());
        tanggalFefoAwal = LocalDate.now().toString();
        fieldTanggalFefo.setText(tanggalFefoAwal);
        bangunIsi();
    }

    private void bangunIsi() {
        JPanel manual = Ui.kartu("Pengeluaran Manual", "stock-out");
        JPanel manualIsi = new JPanel();
        manualIsi.setOpaque(false);
        manualIsi.setLayout(new javax.swing.BoxLayout(
                manualIsi, javax.swing.BoxLayout.Y_AXIS));
        manualIsi.add(Ui.field("Batch *", comboBatch));
        labelSisa.setName("sisaBatch");
        labelSisa.setFont(Ui.font(Font.BOLD, Ui.FONT_CATATAN));
        labelSisa.setForeground(Ui.AKSEN);
        labelSisa.setBorder(new EmptyBorder(8, 0, 8, 0));
        manualIsi.add(labelSisa);
        manualIsi.add(Ui.field("Jumlah *", fieldJumlahManual, pesanManual));
        manualIsi.add(javax.swing.Box.createVerticalStrut(6));
        JPanel aksiManual = new JPanel(new BorderLayout());
        aksiManual.setOpaque(false);
        aksiManual.add(tombolManual, BorderLayout.EAST);
        manualIsi.add(aksiManual);
        manual.add(manualIsi, BorderLayout.CENTER);

        JPanel fefo = Ui.kartu("Pengeluaran FEFO", "stock-out");
        JPanel fefoIsi = new JPanel();
        fefoIsi.setOpaque(false);
        fefoIsi.setLayout(new javax.swing.BoxLayout(
                fefoIsi, javax.swing.BoxLayout.Y_AXIS));
        JLabel bantuan = Ui.keterangan(
                "Batch layak dengan kedaluwarsa terdekat digunakan lebih dahulu.");
        bantuan.setBorder(new EmptyBorder(0, 0, 8, 0));
        fefoIsi.add(bantuan);
        fefoIsi.add(Ui.field("Jumlah *", fieldJumlahFefo));
        fefoIsi.add(javax.swing.Box.createVerticalStrut(8));
        fefoIsi.add(Ui.field("Tanggal pengeluaran * (yyyy-MM-dd)",
                fieldTanggalFefo, pesanFefo));
        fefoIsi.add(javax.swing.Box.createVerticalStrut(6));
        JPanel aksiFefo = new JPanel(new BorderLayout());
        aksiFefo.setOpaque(false);
        aksiFefo.add(tombolFefo, BorderLayout.EAST);
        fefoIsi.add(aksiFefo);
        hasilAlokasi.setBorder(new EmptyBorder(10, 0, 0, 0));
        fefoIsi.add(hasilAlokasi);
        fefo.add(fefoIsi, BorderLayout.CENTER);

        JPanel manualWrap = new JPanel(new java.awt.FlowLayout(
                java.awt.FlowLayout.LEFT, 0, 0));
        manualWrap.setOpaque(true);
        manualWrap.setBackground(Ui.KARTU);
        manual.setMaximumSize(new java.awt.Dimension(Ui.LEBAR_FORM_KELUAR, Integer.MAX_VALUE));
        manual.setPreferredSize(new java.awt.Dimension(
                Ui.skala(Ui.LEBAR_FORM_KELUAR), Ui.skala(Ui.TINGGI_FORM_MANUAL)));
        manualWrap.add(manual);
        JPanel fefoWrap = new JPanel(new java.awt.FlowLayout(
                java.awt.FlowLayout.LEFT, 0, 0));
        fefoWrap.setOpaque(true);
        fefoWrap.setBackground(Ui.KARTU);
        fefo.setMaximumSize(new java.awt.Dimension(Ui.LEBAR_FORM_KELUAR, Integer.MAX_VALUE));
        fefo.setPreferredSize(new java.awt.Dimension(
                Ui.skala(Ui.LEBAR_FORM_KELUAR), Ui.skala(Ui.TINGGI_FORM_FEFO)));
        fefoWrap.add(fefo);

        JTabbedPane mode = new JTabbedPane();
        mode.setFont(Ui.font(Font.BOLD, Ui.FONT_CATATAN));
        mode.setBackground(Ui.KARTU);
        mode.setOpaque(true);
        mode.addTab("Manual", manualWrap);
        mode.addTab("FEFO", fefoWrap);

        JPanel isi = new JPanel(new BorderLayout(0, 8));
        isi.setOpaque(false);
        infoBarang.setBorder(new EmptyBorder(0, 0, 2, 0));
        isi.add(infoBarang, BorderLayout.NORTH);
        isi.add(mode, BorderLayout.CENTER);
        add(isi, BorderLayout.CENTER);
    }

    private void perbaruiSisa() {
        BatchStok batch = (BatchStok) comboBatch.getSelectedItem();
        if (batch == null) {
            labelSisa.setText("Tidak ada batch bersisa.");
            tombolManual.setEnabled(false);
        } else {
            labelSisa.setText("Sisa: " + batch.getSisaJumlah() + " "
                    + konteks.getBarangAktif().getSatuan()
                    + " pada batch " + batch.getKode());
            tombolManual.setEnabled(true);
        }
    }

    private void keluarkanManual() {
        pesanManual.setText(" ");
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
            pengeluaran.keluarkan(konteks.getBarangAktif(), batch, jumlah);
        } catch (StokTidakCukupException | IllegalArgumentException exception) {
            konteks.muatUlang();
            pesanManual.setText("Pengeluaran ditolak: " + exception.getMessage());
            konteks.tampilkanPesan("Pengeluaran gagal: "
                    + exception.getMessage(), false);
            return;
        }

        if (konteks.simpanTransaksi(pengeluaran, "Pengeluaran berhasil: "
                + batch.getKode() + ", " + jumlah + " "
                + konteks.getBarangAktif().getSatuan() + ".")) {
            fieldJumlahManual.setText("");
            pesanManual.setText(" ");
        } else {
            pesanManual.setText("Pengeluaran gagal disimpan. Lihat status.");
        }
    }

    private void keluarkanFefo() {
        pesanFefo.setText(" ");
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
            pengeluaran.keluarkanFefo(konteks.getBarangAktif(), jumlah, tanggal);
        } catch (StokTidakCukupException | IllegalArgumentException exception) {
            konteks.muatUlang();
            pesanFefo.setText("Pengeluaran ditolak: " + exception.getMessage());
            konteks.tampilkanPesan("Pengeluaran FEFO gagal: "
                    + exception.getMessage(), false);
            return;
        }

        String alokasi = formatAlokasi(pengeluaran);
        if (konteks.simpanTransaksi(pengeluaran, "Pengeluaran FEFO berhasil: "
                + alokasi + ". Saldo: "
                + konteks.getBarangAktif().hitungSaldo() + " "
                + konteks.getBarangAktif().getSatuan() + ".")) {
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
                    .append(konteks.getBarangAktif().getSatuan());
        }
        return hasil.toString();
    }

    @Override
    public void perbarui() {
        if (idBarangTerakhir != konteks.getBarangAktif().getId()) {
            idBarangTerakhir = konteks.getBarangAktif().getId();
            fieldJumlahManual.setText("");
            fieldJumlahFefo.setText("");
            tanggalFefoAwal = LocalDate.now().toString();
            fieldTanggalFefo.setText(tanggalFefoAwal);
            pesanManual.setText(" ");
            pesanFefo.setText(" ");
            hasilAlokasi.setText(
                    "Alokasi FEFO akan tampil di sini setelah transaksi berhasil.");
        }
        infoBarang.setText("Pengeluaran untuk "
                + konteks.getBarangAktif().getNama() + " · "
                + konteks.getBarangAktif().getSatuan());
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
        perbaruiSisa();
    }

    @Override
    public boolean adaIsianBelumDisimpan() {
        return !fieldJumlahManual.getText().trim().isEmpty()
                || !fieldJumlahFefo.getText().trim().isEmpty()
                || !fieldTanggalFefo.getText().equals(tanggalFefoAwal);
    }
}
