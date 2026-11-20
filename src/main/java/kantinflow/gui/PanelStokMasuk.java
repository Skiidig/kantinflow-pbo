package kantinflow.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.time.LocalDate;
import java.util.Objects;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import kantinflow.BatchStok;
import kantinflow.PenerimaanStok;

/**
 * Halaman penerimaan stok dan daftar batch barang terpilih.
 */
public class PanelStokMasuk extends PanelBarangTerpilih {
    private final JTextField fieldJumlah = Ui.field(8);
    private final JLabel labelJumlah = Ui.labelField("Jumlah");
    private final PemilihTanggal tanggalMasuk = new PemilihTanggal();
    private final PemilihTanggal tanggalKedaluwarsa = new PemilihTanggal();
    private final JButton tombolSimpan = Ui.utama("Simpan Penerimaan");
    private final JLabel pesanValidasi = Ui.pesan();
    private final JLabel pesanTanggalMasuk = Ui.pesan();
    private final JLabel pesanKedaluwarsa = Ui.pesan();
    private final EmptyState kosong = new EmptyState(
            "package", "Belum ada batch",
            "Batch baru akan muncul di sini setelah penerimaan pertama.");
    private LocalDate tanggalMasukAwal;
    private final DefaultTableModel model = Ui.model(
            "Kode Batch", "Sisa", "Tanggal Masuk", "Tanggal Kedaluwarsa",
            "Status");
    private final JTable tabel = Ui.tabel(model);
    private JScrollPane gulirTabel;

    public PanelStokMasuk(KonteksKantin konteks) {
        super(konteks, "Stok Masuk",
                "Terima batch baru untuk barang terpilih.");
        pilihanBarang.setName("barangStokMasuk");
        fieldJumlah.setName("jumlah");
        fieldJumlah.putClientProperty("JTextField.placeholderText",
                "Contoh: 20");
        tanggalMasuk.komponenTeks().setName("tanggalMasuk");
        tanggalKedaluwarsa.komponenTeks().setName("tanggalKedaluwarsa");
        pesanTanggalMasuk.setName("pesanTanggalMasuk");
        pesanKedaluwarsa.setName("pesanTanggalKedaluwarsa");
        tombolSimpan.setName("tombolSimpan");
        tombolSimpan.setIcon(Ui.ikonAksi("stock-in", 16, java.awt.Color.WHITE));
        tombolSimpan.setIconTextGap(7);
        tabel.setName("tabelBatch");
        tanggalMasuk.tambahPerubahan(() -> {
            kosongkanPesan(pesanTanggalMasuk);
            LocalDate masuk = tanggalMasuk.getTanggal();
            tanggalKedaluwarsa.setTanggalMinimum(masuk);
            LocalDate kedaluwarsa = tanggalKedaluwarsa.getTanggal();
            if (masuk != null && kedaluwarsa != null
                    && kedaluwarsa.isBefore(masuk)) {
                aturPesan(pesanKedaluwarsa,
                        "Tanggal kedaluwarsa tidak boleh sebelum tanggal masuk.");
            } else {
                kosongkanPesan(pesanKedaluwarsa);
            }
        });
        tanggalKedaluwarsa.tambahPerubahan(
                () -> kosongkanPesan(pesanKedaluwarsa));
        tombolSimpan.addActionListener(event -> simpan());
        kosong.setName("kosongBatch");
        Ui.rataKanan(tabel, 1);
        Ui.pasangBadge(tabel, 4);
        Ui.aturKolom(tabel, 175, 85, 125, 145, 175);
        tabel.getColumnModel().getColumn(4).setMinWidth(Ui.skala(165));
        Ui.gulirHorizontalSaatSempit(tabel);
        aturTanggalAwal();
        bangunIsi();
    }

    private void perbaruiLabelJumlah() {
        labelJumlah.setText(Ui.teksJumlah(barang));
    }

    private void aturTanggalAwal() {
        tanggalMasukAwal = LocalDate.now();
        tanggalMasuk.setTanggal(tanggalMasukAwal);
        tanggalKedaluwarsa.setTanggal(null);
        tanggalKedaluwarsa.setTanggalMinimum(tanggalMasukAwal);
        kosongkanPesan(pesanValidasi);
        kosongkanPesan(pesanTanggalMasuk);
        kosongkanPesan(pesanKedaluwarsa);
    }

    private void aturPesan(JLabel label, String teks) {
        boolean tampil = teks != null && !teks.isBlank();
        label.setText(tampil ? teks : " ");
        label.setVisible(tampil);
        label.revalidate();
    }

    private void kosongkanPesan(JLabel label) {
        aturPesan(label, null);
    }

    private void bangunIsi() {
        JPanel isi = new JPanel(new BorderLayout(0, Ui.skala(Ui.GAP_KARTU)));
        isi.setOpaque(false);

        JPanel form = Ui.kartu("Form Penerimaan", "stock-in");
        JPanel kolomBarang = fieldBarang("Barang");
        JPanel kolomJumlah = Ui.field(labelJumlah, fieldJumlah);
        JPanel kolomMasuk = Ui.field("Tanggal masuk",
                tanggalMasuk, pesanTanggalMasuk);
        JPanel kolomKedaluwarsa = Ui.field("Tanggal kedaluwarsa",
                tanggalKedaluwarsa, pesanKedaluwarsa);
        JPanel[] kolom = {kolomBarang, kolomJumlah,
                kolomMasuk, kolomKedaluwarsa};
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
        formIsi.setLayout(new BoxLayout(formIsi, BoxLayout.Y_AXIS));
        isian.setAlignmentX(LEFT_ALIGNMENT);
        formIsi.add(isian);
        pesanValidasi.setAlignmentX(LEFT_ALIGNMENT);
        pesanValidasi.setBorder(new javax.swing.border.EmptyBorder(
                Ui.skala(6), 0, 0, 0));
        formIsi.add(pesanValidasi);
        JPanel aksi = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        aksi.setOpaque(false);
        aksi.setAlignmentX(LEFT_ALIGNMENT);
        aksi.setBorder(new javax.swing.border.EmptyBorder(
                Ui.skala(16), 0, 0, 0));
        aksi.add(tombolSimpan);
        formIsi.add(aksi);
        form.add(formIsi, BorderLayout.CENTER);

        JPanel daftar = Ui.kartu("Batch Barang", "package");
        JPanel daftarIsi = new JPanel(new BorderLayout(0, 4));
        daftarIsi.setOpaque(false);
        gulirTabel = Ui.scrollTabel(tabel, Ui.TINGGI_TABEL_STOK_MASUK);
        daftarIsi.add(gulirTabel, BorderLayout.CENTER);
        daftarIsi.add(kosong, BorderLayout.SOUTH);
        daftar.add(daftarIsi, BorderLayout.CENTER);

        isi.add(form, BorderLayout.NORTH);
        isi.add(daftar, BorderLayout.CENTER);
        add(isi, BorderLayout.CENTER);
    }

    private void aturGridIsian(JPanel panel, JPanel[] kolom,
            boolean sempit) {
        panel.removeAll();
        panel.setLayout(new GridBagLayout());
        int jarak = Ui.skala(Ui.GAP_FORM);
        for (int i = 0; i < kolom.length; i++) {
            GridBagConstraints c = new GridBagConstraints();
            if (sempit) {
                c.gridx = 0;
                c.gridy = i;
                c.insets = new Insets(i == 0 ? 0 : jarak, 0, 0, 0);
            } else {
                c.gridx = i % 2;
                c.gridy = i / 2;
                c.insets = new Insets(c.gridy == 0 ? 0 : jarak,
                        c.gridx == 0 ? 0 : jarak / 2,
                        0, c.gridx == 1 ? 0 : jarak / 2);
            }
            c.weightx = 1;
            c.weighty = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.NORTH;
            panel.add(kolom[i], c);
        }
    }

    private void simpan() {
        kosongkanPesan(pesanValidasi);
        kosongkanPesan(pesanTanggalMasuk);
        kosongkanPesan(pesanKedaluwarsa);
        if (barang == null) {
            aturPesan(pesanValidasi,
                    "Belum ada barang. Tambahkan melalui halaman Barang.");
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

        LocalDate masuk = tanggalMasuk.getTanggal();
        if (masuk == null) {
            aturPesan(pesanTanggalMasuk, "Tanggal masuk wajib diisi.");
            tanggalMasuk.komponenTeks().requestFocusInWindow();
            return;
        }

        LocalDate kedaluwarsa = tanggalKedaluwarsa.getTanggal();
        if (kedaluwarsa == null) {
            aturPesan(pesanKedaluwarsa, "Tanggal kedaluwarsa wajib diisi.");
            tanggalKedaluwarsa.komponenTeks().requestFocusInWindow();
            return;
        }
        if (kedaluwarsa.isBefore(masuk)) {
            aturPesan(pesanKedaluwarsa,
                    "Tanggal kedaluwarsa tidak boleh sebelum tanggal masuk.");
            tanggalKedaluwarsa.komponenTeks().requestFocusInWindow();
            return;
        }

        String kode = konteks.kodeBatchOtomatis(barang, masuk);
        if (kode == null || kode.isBlank()) {
            aturPesan(pesanValidasi,
                    "Kode batch otomatis tidak dapat dibentuk.");
            return;
        }

        PenerimaanStok penerimaan = new PenerimaanStok(masuk);
        try {
            penerimaan.terima(barang, kode, jumlah, masuk, kedaluwarsa);
        } catch (IllegalArgumentException exception) {
            konteks.muatUlang();
            aturPesan(pesanValidasi,
                    "Penerimaan ditolak: " + exception.getMessage());
            konteks.tampilkanPesan("Penerimaan gagal: "
                    + exception.getMessage(), false);
            return;
        }

        if (konteks.simpanTransaksi(barang, penerimaan, "Penerimaan berhasil: "
                + kode + ", " + jumlah + " " + barang.getSatuan() + ".")) {
            fieldJumlah.setText("");
            aturTanggalAwal();
        } else {
            aturPesan(pesanValidasi,
                    "Penerimaan gagal disimpan. Lihat status.");
        }
    }

    @Override
    protected void saatBarangBerubah() {
        fieldJumlah.setText("");
        kosongkanPesan(pesanValidasi);
        perbaruiLabelJumlah();
        aturTanggalAwal();
    }

    @Override
    public void perbarui() {
        boolean adaBarang = perbaruiBarangTerpilih();
        perbaruiLabelJumlah();
        if (!adaBarang) {
            model.setRowCount(0);
            kosong.setTitle("Belum ada barang");
            kosong.setDescription(
                    "Tambahkan barang melalui halaman Barang untuk mencatat "
                    + "penerimaan.");
            kosong.setVisible(true);
            if (gulirTabel != null) {
                gulirTabel.setVisible(false);
            }
            tombolSimpan.setEnabled(false);
            return;
        }
        tombolSimpan.setEnabled(true);
        model.setRowCount(0);
        LocalDate acuan = konteks.getTanggalAcuan();
        for (BatchStok batch : barang.getDaftarBatch()) {
            String status = batch.getSisaJumlah() <= 0
                    ? "Habis" : batch.statusPada(acuan).getLabel();
            model.addRow(new Object[]{
                batch.getKode(),
                batch.getSisaJumlah() + " " + barang.getSatuan(),
                batch.getTanggalMasuk(),
                batch.getTanggalKedaluwarsa(),
                status
            });
        }
        boolean kosongTabel = model.getRowCount() == 0;
        kosong.setTitle("Belum ada batch");
        kosong.setDescription(
                "Batch baru akan muncul di sini setelah penerimaan pertama.");
        kosong.setVisible(kosongTabel);
        if (gulirTabel != null) {
            gulirTabel.setVisible(!kosongTabel);
        }
        Ui.sesuaikanTinggiTabel(gulirTabel, tabel, 8);
    }

    @Override
    public boolean adaIsianBelumDisimpan() {
        return !fieldJumlah.getText().trim().isEmpty()
                || !Objects.equals(tanggalMasuk.getTanggal(), tanggalMasukAwal)
                || tanggalKedaluwarsa.getTanggal() != null
                || !tanggalKedaluwarsa.komponenTeks().getText().trim().isEmpty();
    }
}
