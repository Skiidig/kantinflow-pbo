package kantinflow.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import kantinflow.Barang;
import kantinflow.BatchStok;
import kantinflow.PembuanganStok;
import kantinflow.StatusKedaluwarsa;
import kantinflow.StokTidakCukupException;

/**
 * Halaman pembuangan batch kedaluwarsa atau rusak/tidak layak pakai.
 */
public class PanelPembuangan extends PanelBarangTerpilih {
    private final JComboBox<BatchStok> comboBatch = Ui.combo();
    private final JTextField fieldJumlah = Ui.field(6);
    private final JLabel labelJumlah = Ui.labelField("Jumlah");
    private final PemilihTanggal tanggalPembuangan = new PemilihTanggal();
    private final JLabel labelAlasan = Ui.labelField("Alasan");
    private final JTextField fieldAlasan = Ui.field(16);
    private final JButton tombolBuang = Ui.bahaya("Buang Stok");
    private final JLabel pesanTanggal = Ui.pesan();
    private final JLabel pesanValidasi = Ui.pesan();
    private final JLabel ringkasan = Ui.kosong(
            "Pilih batch untuk melihat ringkasan tindakan.");
    private LocalDate tanggalAwal;
    private boolean memperbaruiBatch;

    public PanelPembuangan(KonteksKantin konteks) {
        super(konteks, "Pembuangan",
                "Buang batch kedaluwarsa atau stok rusak dengan alasan.");
        pilihanBarang.setName("barangPembuangan");
        comboBatch.setName("batchBuang");
        fieldJumlah.setName("jumlahBuang");
        fieldJumlah.putClientProperty("JTextField.placeholderText",
                "Contoh: 5");
        tanggalPembuangan.komponenTeks().setName("tanggalBuang");
        pesanTanggal.setName("pesanTanggalBuang");
        fieldAlasan.setName("alasanBuang");
        tombolBuang.setName("tombolBuang");
        tombolBuang.setIcon(Ui.ikonAksi("trash", 16, java.awt.Color.WHITE));
        tombolBuang.setIconTextGap(7);
        tanggalAwal = LocalDate.now();
        tanggalPembuangan.setTanggal(tanggalAwal);
        kosongkanPesan(pesanTanggal);
        kosongkanPesan(pesanValidasi);
        tanggalPembuangan.tambahPerubahan(() -> {
            kosongkanPesan(pesanTanggal);
            if (barang != null && !memperbaruiBatch) {
                BatchStok terpilih = (BatchStok) comboBatch.getSelectedItem();
                isiComboBatch(terpilih == null ? null : terpilih.getKode());
            }
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
                    LocalDate acuan = tanggalAcuanPembuangan();
                    boolean kedaluwarsa = sudahKedaluwarsa(batch, acuan);
                    String awalan = kedaluwarsa ? "[KEDALUWARSA] " : "";
                    setText(awalan + batch.getKode() + " — sisa "
                            + batch.getSisaJumlah() + " "
                            + (barang == null ? "-" : barang.getSatuan())
                            + " — kedaluwarsa "
                            + FormatTanggal.format(batch.getTanggalKedaluwarsa()));
                    if (!isSelected) {
                        setForeground(kedaluwarsa ? Ui.MERAH_BADGE : Ui.TEKS);
                    }
                }
                return this;
            }
        });
        comboBatch.addActionListener(event -> {
            if (!memperbaruiBatch) {
                perbaruiRingkasan();
            }
        });
        tombolBuang.addActionListener(event -> buang());
        bangunIsi();
    }

    private void bangunIsi() {
        JPanel form = Ui.kartu("Form Pembuangan");
        JPanel[] kolom = {
            fieldBarang("Barang"),
            Ui.field("Batch", comboBatch),
            Ui.field(labelJumlah, fieldJumlah),
            Ui.field("Tanggal pembuangan", tanggalPembuangan, pesanTanggal),
            Ui.field(labelAlasan, fieldAlasan)
        };
        JPanel isian = new JPanel();
        isian.setOpaque(false);
        aturGridIsian(isian, kolom, false);
        isian.addComponentListener(new ComponentAdapter() {
            private boolean sempit;

            @Override
            public void componentResized(ComponentEvent event) {
                boolean sekarang = isian.getWidth() > 0
                        && isian.getWidth() < Ui.skala(Ui.AMBANG_FORM_SEMPIT);
                if (sekarang == sempit) {
                    return;
                }
                sempit = sekarang;
                aturGridIsian(isian, kolom, sempit);
                isian.revalidate();
                isian.repaint();
            }
        });
        JPanel formIsi = new JPanel();
        formIsi.setOpaque(false);
        formIsi.setLayout(new javax.swing.BoxLayout(
                formIsi, javax.swing.BoxLayout.Y_AXIS));
        isian.setAlignmentX(LEFT_ALIGNMENT);
        formIsi.add(isian);
        pesanValidasi.setAlignmentX(LEFT_ALIGNMENT);
        pesanValidasi.setBorder(new EmptyBorder(Ui.skala(6), 0, 0, 0));
        formIsi.add(pesanValidasi);
        JPanel aksi = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        aksi.setOpaque(false);
        aksi.setAlignmentX(LEFT_ALIGNMENT);
        aksi.setBorder(new EmptyBorder(Ui.skala(16), 0, 0, 0));
        aksi.add(tombolBuang);
        formIsi.add(aksi);
        form.add(formIsi, BorderLayout.CENTER);

        JPanel tindakan = Ui.kartu("Ringkasan Tindakan");
        JPanel tindakanIsi = new JPanel(new BorderLayout(0, 6));
        tindakanIsi.setOpaque(false);
        ringkasan.setFont(Ui.fontSemiBold(Ui.FONT_CATATAN));
        ringkasan.setForeground(Ui.TEKS_LEMBUT);
        tindakanIsi.add(ringkasan, BorderLayout.NORTH);
        JTextArea catatan = new JTextArea(
                "Batch yang sudah kedaluwarsa dapat langsung dibuang tanpa "
                + "mengisi alasan (otomatis tercatat sebagai \"Kedaluwarsa\" "
                + "jika dikosongkan). Batch yang belum kedaluwarsa tetap dapat "
                + "dibuang — misalnya karena jatuh atau rusak — dengan wajib "
                + "mengisi kolom Alasan. Tindakan ini mengurangi sisa batch "
                + "dan tercatat di riwayat.");
        catatan.setEditable(false);
        catatan.setFocusable(false);
        catatan.setOpaque(false);
        catatan.setLineWrap(true);
        catatan.setWrapStyleWord(true);
        catatan.setFont(Ui.font(Font.PLAIN, Ui.FONT_CATATAN));
        catatan.setForeground(Ui.TEKS_LEMBUT);
        catatan.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        tindakanIsi.add(catatan, BorderLayout.CENTER);
        tindakan.add(tindakanIsi, BorderLayout.CENTER);

        JPanel isi = new JPanel(new BorderLayout(0, Ui.skala(Ui.GAP_KARTU)));
        isi.setOpaque(false);
        isi.add(form, BorderLayout.NORTH);
        isi.add(tindakan, BorderLayout.CENTER);
        add(isi, BorderLayout.NORTH);
    }

    private void aturGridIsian(JPanel panel, JPanel[] kolom,
            boolean sempit) {
        panel.removeAll();
        panel.setLayout(new GridBagLayout());
        int jarak = Ui.skala(Ui.GAP_FORM);
        if (sempit) {
            for (int i = 0; i < kolom.length; i++) {
                GridBagConstraints c = new GridBagConstraints();
                c.gridx = 0;
                c.gridy = i;
                c.weightx = 1;
                c.fill = GridBagConstraints.HORIZONTAL;
                c.anchor = GridBagConstraints.NORTH;
                c.insets = new Insets(i == 0 ? 0 : jarak, 0, 0, 0);
                panel.add(kolom[i], c);
            }
            return;
        }
        GridBagConstraints awal = new GridBagConstraints();
        awal.gridx = 0;
        awal.gridy = 0;
        awal.gridwidth = 2;
        awal.weightx = 1;
        awal.fill = GridBagConstraints.HORIZONTAL;
        awal.anchor = GridBagConstraints.NORTH;
        panel.add(kolom[0], awal);
        for (int i = 1; i < kolom.length; i += 2) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 1 + (i - 1) / 2;
            c.gridwidth = 2;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.NORTH;
            c.insets = new Insets(jarak, 0, 0, 0);
            panel.add(i + 1 < kolom.length
                    ? pasangan(kolom[i], kolom[i + 1], jarak) : kolom[i], c);
        }
    }

    private JPanel pasangan(JPanel kiri, JPanel kanan, int jarak) {
        JPanel panel = new JPanel(new GridLayout(1, 2, jarak, 0));
        panel.setOpaque(false);
        panel.add(kiri);
        panel.add(kanan);
        return panel;
    }

    private void perbaruiLabelJumlah() {
        labelJumlah.setText(Ui.teksJumlah(barang));
    }

    private void aturPesan(JLabel label, String teks) {
        boolean tampil = teks != null && !teks.isBlank();
        label.setText(tampil ? teks : " ");
        label.setVisible(tampil);
        if (tampil) {
            label.setPreferredSize(null);
            label.setMinimumSize(null);
            label.setMaximumSize(null);
        } else {
            Dimension kosong = new Dimension(0, 0);
            label.setPreferredSize(kosong);
            label.setMinimumSize(kosong);
            label.setMaximumSize(kosong);
        }
        label.revalidate();
    }

    private void kosongkanPesan(JLabel label) {
        aturPesan(label, null);
    }

    private LocalDate tanggalAcuanPembuangan() {
        LocalDate dipilih = tanggalPembuangan.getTanggal();
        return dipilih != null ? dipilih : konteks.getTanggalAcuan();
    }

    private boolean sudahKedaluwarsa(BatchStok batch, LocalDate tanggal) {
        return batch != null && tanggal != null
                && batch.statusPada(tanggal) == StatusKedaluwarsa.KEDALUWARSA;
    }

    private void isiComboBatch(String kodeTarget) {
        memperbaruiBatch = true;
        try {
            comboBatch.removeAllItems();
            if (barang == null) {
                return;
            }
            LocalDate acuan = tanggalAcuanPembuangan();
            List<BatchStok> bersisa = new ArrayList<>();
            for (BatchStok batch : barang.getDaftarBatch()) {
                if (batch.getSisaJumlah() > 0) {
                    bersisa.add(batch);
                }
            }
            bersisa.sort(Comparator
                    .comparingInt((BatchStok batch) ->
                            sudahKedaluwarsa(batch, acuan) ? 0 : 1)
                    .thenComparing(BatchStok::getTanggalKedaluwarsa)
                    .thenComparing(BatchStok::getTanggalMasuk)
                    .thenComparing(BatchStok::getKode,
                            String.CASE_INSENSITIVE_ORDER));
            for (BatchStok batch : bersisa) {
                comboBatch.addItem(batch);
            }
            if (kodeTarget != null) {
                for (int i = 0; i < comboBatch.getItemCount(); i++) {
                    if (kodeTarget.equals(comboBatch.getItemAt(i).getKode())) {
                        comboBatch.setSelectedIndex(i);
                        break;
                    }
                }
            }
        } finally {
            memperbaruiBatch = false;
        }
    }

    private String ringkasanKelayakan(BatchStok batch) {
        LocalDate dipilih = tanggalPembuangan.getTanggal();
        if (dipilih == null) {
            return "tanggal belum diisi";
        }
        if (dipilih.isBefore(batch.getTanggalMasuk())) {
            return "sebelum tanggal masuk (tidak dapat dibuang)";
        }
        if (sudahKedaluwarsa(batch, dipilih)) {
            return "Kedaluwarsa — alasan opsional (otomatis \"Kedaluwarsa\" jika kosong)";
        }
        return "Belum kedaluwarsa — wajib mengisi alasan";
    }

    private void perbaruiLabelAlasan(BatchStok batch) {
        LocalDate dipilih = tanggalPembuangan.getTanggal();
        if (batch == null || dipilih == null) {
            labelAlasan.setText("Alasan");
            fieldAlasan.putClientProperty("JTextField.placeholderText", "");
            return;
        }
        if (sudahKedaluwarsa(batch, dipilih)) {
            labelAlasan.setText("Alasan (opsional, default: Kedaluwarsa)");
            fieldAlasan.putClientProperty("JTextField.placeholderText",
                    "Opsional — otomatis \"Kedaluwarsa\" jika kosong");
        } else {
            labelAlasan.setText("Alasan (wajib diisi)");
            fieldAlasan.putClientProperty("JTextField.placeholderText",
                    "Contoh: Jatuh, kemasan rusak, tumpah");
        }
    }

    private void perbaruiRingkasan() {
        BatchStok batch = (BatchStok) comboBatch.getSelectedItem();
        perbaruiLabelAlasan(batch);
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
                + " · kedaluwarsa "
                + FormatTanggal.format(batch.getTanggalKedaluwarsa())
                + " · " + ringkasanKelayakan(batch));
    }

    private void buang() {
        kosongkanPesan(pesanValidasi);
        kosongkanPesan(pesanTanggal);
        if (barang == null) {
            aturPesan(pesanValidasi,
                    "Belum ada barang. Tambahkan melalui halaman Barang.");
            return;
        }
        BatchStok batch = (BatchStok) comboBatch.getSelectedItem();
        if (batch == null) {
            aturPesan(pesanValidasi, "Pilih batch terlebih dahulu.");
            comboBatch.requestFocusInWindow();
            return;
        }
        int jumlah;
        try {
            jumlah = Integer.parseInt(fieldJumlah.getText().trim());
        } catch (NumberFormatException exception) {
            aturPesan(pesanValidasi, "Jumlah harus berupa angka bulat.");
            fieldJumlah.requestFocusInWindow();
            return;
        }
        LocalDate tanggal = tanggalPembuangan.getTanggal();
        if (tanggal == null) {
            boolean kosong = tanggalPembuangan.komponenTeks()
                    .getText().trim().isEmpty();
            aturPesan(pesanTanggal, kosong
                    ? "Tanggal pembuangan wajib diisi."
                    : "Format tanggal pembuangan tidak valid.");
            tanggalPembuangan.komponenTeks().requestFocusInWindow();
            return;
        }
        boolean kedaluwarsa = sudahKedaluwarsa(batch, tanggal);
        String alasanInput = fieldAlasan.getText() == null
                ? "" : fieldAlasan.getText().trim();
        if (!kedaluwarsa && alasanInput.isEmpty()) {
            aturPesan(pesanValidasi,
                    "Alasan pembuangan wajib diisi untuk batch yang belum kedaluwarsa.");
            fieldAlasan.requestFocusInWindow();
            return;
        }
        String alasanEfektif = alasanInput.isEmpty()
                ? "Kedaluwarsa" : alasanInput;

        String judulDialog = kedaluwarsa
                ? "Buang stok kedaluwarsa" : "Buang stok belum kedaluwarsa";
        String pesanDialog = "Batch " + batch.getKode() + " sebanyak " + jumlah
                + " " + barang.getSatuan() + " akan dibuang dengan alasan \""
                + alasanEfektif + "\"."
                + (kedaluwarsa ? "" : " Perhatian: batch ini belum kedaluwarsa.")
                + " Tindakan ini mengurangi sisa batch dan tercatat di riwayat.";
        if (!DialogKonfirmasi.tampilkan(this, judulDialog, pesanDialog,
                "Buang Stok")) {
            return;
        }

        PembuanganStok pembuangan = new PembuanganStok(tanggal);
        try {
            pembuangan.buang(barang, batch, jumlah, alasanInput);
        } catch (StokTidakCukupException | IllegalArgumentException exception) {
            aturPesan(pesanValidasi,
                    "Pembuangan ditolak: " + exception.getMessage());
            konteks.tampilkanPesan("Pembuangan gagal: "
                    + exception.getMessage(), false);
            return;
        }

        if (konteks.simpanTransaksi(barang, pembuangan,
                "Pembuangan berhasil: " + batch.getKode() + ", " + jumlah
                + " " + barang.getSatuan() + " pada "
                + FormatTanggal.format(tanggal)
                + ". Alasan: " + pembuangan.getAlasan() + ".")) {
            fieldJumlah.setText("");
            fieldAlasan.setText("");
            tanggalAwal = LocalDate.now();
            tanggalPembuangan.setTanggal(tanggalAwal);
            kosongkanPesan(pesanTanggal);
            kosongkanPesan(pesanValidasi);
        } else {
            aturPesan(pesanValidasi,
                    "Pembuangan gagal disimpan. Lihat status.");
        }
    }

    boolean siapkanDariPemantauan(
            int idBarangTarget, String kodeBatch, LocalDate tanggal) {
        if (idBarangTarget < 0 || kodeBatch == null
                || kodeBatch.isBlank() || tanggal == null) {
            return false;
        }

        Barang target = null;
        for (Barang item : konteks.getDaftarBarang()) {
            if (item.getId() == idBarangTarget) {
                target = item;
                break;
            }
        }
        if (target == null) {
            return false;
        }

        BatchStok batchTarget = null;
        for (BatchStok item : target.getDaftarBatch()) {
            if (item.getKode().equals(kodeBatch)) {
                batchTarget = item;
                break;
            }
        }
        if (batchTarget == null || batchTarget.getSisaJumlah() <= 0
                || batchTarget.statusPada(tanggal)
                        != StatusKedaluwarsa.KEDALUWARSA) {
            return false;
        }

        fieldJumlah.setText("");
        fieldAlasan.setText("");
        kosongkanPesan(pesanValidasi);
        kosongkanPesan(pesanTanggal);
        tanggalAwal = tanggal;
        tanggalPembuangan.setTanggal(tanggal);

        pilihanBarang.setSelectedItem(target);
        if (barang == null || barang.getId() != idBarangTarget) {
            return false;
        }

        tanggalAwal = tanggal;
        tanggalPembuangan.setTanggal(tanggal);
        isiComboBatch(kodeBatch);
        BatchStok terpilih = (BatchStok) comboBatch.getSelectedItem();
        if (terpilih != null && terpilih.getKode().equals(kodeBatch)) {
            perbaruiLabelJumlah();
            perbaruiRingkasan();
            return true;
        }
        return false;
    }

    @Override
    protected void saatBarangBerubah() {
        fieldJumlah.setText("");
        fieldAlasan.setText("");
        kosongkanPesan(pesanValidasi);
        kosongkanPesan(pesanTanggal);
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
            tombolBuang.setEnabled(false);
            perbaruiLabelAlasan(null);
            ringkasan.setForeground(Ui.TEKS_LEMBUT);
            ringkasan.setText(
                    "Belum ada barang. Tambahkan melalui halaman Barang.");
            return;
        }
        BatchStok sebelum = (BatchStok) comboBatch.getSelectedItem();
        String kodeTerpilih = sebelum == null ? null : sebelum.getKode();
        isiComboBatch(kodeTerpilih);
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
