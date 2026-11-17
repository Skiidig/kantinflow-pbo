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
import javax.swing.JTextField;

import kantinflow.BatchStok;
import kantinflow.PengeluaranStok;
import kantinflow.RincianTransaksi;
import kantinflow.StokTidakCukupException;

/**
 * Halaman pengeluaran manual dan FEFO untuk barang aktif.
 */
public class PanelStokKeluar extends PanelKantin {
    private final JComboBox<BatchStok> comboBatch = new JComboBox<>();
    private final JLabel labelSisa = new JLabel("-");
    private final JTextField fieldJumlahManual = Ui.field(8);
    private final JButton tombolManual = Ui.utama("Keluarkan Manual");
    private final JTextField fieldJumlahFefo = Ui.field(8);
    private final JTextField fieldTanggalFefo = Ui.field(12);
    private final JButton tombolFefo = Ui.utama("Keluarkan FEFO");

    public PanelStokKeluar(KonteksKantin konteks) {
        super(konteks, "Stok Keluar",
                "Pilih batch secara manual atau biarkan FEFO memilih batch paling cepat kedaluwarsa.");
        comboBatch.setName("batchManual");
        fieldJumlahManual.setName("jumlahKeluar");
        tombolManual.setName("tombolKeluarkan");
        fieldJumlahFefo.setName("jumlahFefo");
        fieldTanggalFefo.setName("tanggalFefo");
        tombolFefo.setName("tombolFefo");
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
        fieldTanggalFefo.setText(LocalDate.now().toString());
        bangunIsi();
    }

    private void bangunIsi() {
        JPanel manual = Ui.kartu("Pengeluaran Manual");
        JPanel manualIsi = new JPanel(new BorderLayout(0, 10));
        manualIsi.setOpaque(false);
        manualIsi.add(Ui.field("Batch", comboBatch), BorderLayout.NORTH);
        labelSisa.setName("sisaBatch");
        labelSisa.setFont(Ui.font(Font.BOLD, 12f));
        labelSisa.setForeground(Ui.AKSEN);
        manualIsi.add(labelSisa, BorderLayout.CENTER);
        JPanel manualAksi = new JPanel(new BorderLayout(8, 6));
        manualAksi.setOpaque(false);
        manualAksi.add(Ui.field("Jumlah", fieldJumlahManual), BorderLayout.NORTH);
        manualAksi.add(tombolManual, BorderLayout.SOUTH);
        manualIsi.add(manualAksi, BorderLayout.SOUTH);
        manual.add(manualIsi, BorderLayout.CENTER);

        JPanel fefo = Ui.kartu("Pengeluaran FEFO");
        JPanel fefoIsi = new JPanel(new BorderLayout(0, 10));
        fefoIsi.setOpaque(false);
        JPanel fefoField = new JPanel(new GridLayout(2, 1, 0, 8));
        fefoField.setOpaque(false);
        fefoField.add(Ui.field("Jumlah", fieldJumlahFefo));
        fefoField.add(Ui.field("Tanggal pengeluaran (yyyy-MM-dd)",
                fieldTanggalFefo));
        fefoIsi.add(fefoField, BorderLayout.CENTER);
        fefoIsi.add(tombolFefo, BorderLayout.SOUTH);
        fefo.add(fefoIsi, BorderLayout.CENTER);

        JPanel kartu = new JPanel(new GridLayout(1, 2, 16, 0));
        kartu.setOpaque(false);
        kartu.add(manual);
        kartu.add(fefo);
        JLabel catatan = Ui.keterangan(
                "Tanggal pengeluaran tidak boleh sebelum tanggal masuk batch.");
        kartu.setBorder(new javax.swing.border.EmptyBorder(0, 0, 0, 0));

        JPanel isi = new JPanel(new BorderLayout(0, 12));
        isi.setOpaque(false);
        isi.add(kartu, BorderLayout.NORTH);
        isi.add(catatan, BorderLayout.CENTER);
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
        BatchStok batch = (BatchStok) comboBatch.getSelectedItem();
        if (batch == null) {
            konteks.tampilkanPesan("Pilih batch terlebih dahulu.", false);
            return;
        }
        int jumlah;
        try {
            jumlah = Integer.parseInt(fieldJumlahManual.getText().trim());
        } catch (NumberFormatException exception) {
            konteks.tampilkanPesan("Jumlah harus berupa angka bulat.", false);
            return;
        }

        PengeluaranStok pengeluaran = new PengeluaranStok(LocalDate.now());
        try {
            pengeluaran.keluarkan(konteks.getBarangAktif(), batch, jumlah);
        } catch (StokTidakCukupException | IllegalArgumentException exception) {
            konteks.muatUlang();
            konteks.tampilkanPesan("Pengeluaran gagal: "
                    + exception.getMessage(), false);
            return;
        }

        if (konteks.simpanTransaksi(pengeluaran, "Pengeluaran berhasil: "
                + batch.getKode() + ", " + jumlah + " "
                + konteks.getBarangAktif().getSatuan() + ".")) {
            fieldJumlahManual.setText("");
        }
    }

    private void keluarkanFefo() {
        int jumlah;
        try {
            jumlah = Integer.parseInt(fieldJumlahFefo.getText().trim());
        } catch (NumberFormatException exception) {
            konteks.tampilkanPesan("Jumlah harus berupa angka bulat.", false);
            return;
        }

        LocalDate tanggal;
        try {
            tanggal = LocalDate.parse(fieldTanggalFefo.getText().trim());
        } catch (DateTimeParseException exception) {
            konteks.tampilkanPesan("Format tanggal tidak valid. Gunakan yyyy-MM-dd.",
                    false);
            return;
        }

        PengeluaranStok pengeluaran = new PengeluaranStok(tanggal);
        try {
            pengeluaran.keluarkanFefo(konteks.getBarangAktif(), jumlah, tanggal);
        } catch (StokTidakCukupException | IllegalArgumentException exception) {
            konteks.muatUlang();
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
}
