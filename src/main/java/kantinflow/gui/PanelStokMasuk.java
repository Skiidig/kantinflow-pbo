package kantinflow.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.util.Objects;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import kantinflow.BatchStok;
import kantinflow.PenerimaanStok;

/**
 * Halaman penerimaan stok dan daftar batch barang terpilih.
 */
public class PanelStokMasuk extends PanelBarangTerpilih {
    private final JTextField fieldKode = Ui.field(16);
    private final JTextField fieldJumlah = Ui.field(8);
    private final JLabel labelJumlah = Ui.labelField("Jumlah *");
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

    public PanelStokMasuk(KonteksKantin konteks) {
        super(konteks, "Stok Masuk",
                "Terima batch baru untuk barang terpilih.");
        pilihanBarang.setName("barangStokMasuk");
        fieldKode.setName("kodeBatch");
        fieldKode.putClientProperty("JTextField.placeholderText",
                "Contoh: B01 atau SUSU-001");
        fieldKode.setToolTipText(
                "Kode untuk mengenali satu kelompok stok yang diterima.");
        fieldJumlah.setName("jumlah");
        tanggalMasuk.komponenTeks().setName("tanggalMasuk");
        tanggalKedaluwarsa.komponenTeks().setName("tanggalKedaluwarsa");
        pesanTanggalMasuk.setName("pesanTanggalMasuk");
        pesanKedaluwarsa.setName("pesanTanggalKedaluwarsa");
        tombolSimpan.setName("tombolSimpan");
        tombolSimpan.setIcon(Ui.ikonAksi("stock-in", 16, java.awt.Color.WHITE));
        tombolSimpan.setIconTextGap(7);
        tabel.setName("tabelBatch");
        tanggalMasuk.tambahPerubahan(() -> {
            pesanTanggalMasuk.setText(" ");
            LocalDate masuk = tanggalMasuk.getTanggal();
            tanggalKedaluwarsa.setTanggalMinimum(masuk);
            LocalDate kedaluwarsa = tanggalKedaluwarsa.getTanggal();
            if (masuk != null && kedaluwarsa != null
                    && kedaluwarsa.isBefore(masuk)) {
                pesanKedaluwarsa.setText(
                        "Tanggal kedaluwarsa tidak boleh sebelum tanggal masuk.");
            } else {
                pesanKedaluwarsa.setText(" ");
            }
        });
        tanggalKedaluwarsa.tambahPerubahan(
                () -> pesanKedaluwarsa.setText(" "));
        tombolSimpan.addActionListener(event -> simpan());
        kosong.setName("kosongBatch");
        Ui.rataKanan(tabel, 1);
        Ui.pasangTeksStatus(tabel, 4);
        Ui.aturKolom(tabel, 120, 75, 120, 135, 140);
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
        pesanTanggalMasuk.setText(" ");
        pesanKedaluwarsa.setText(" ");
    }

    private void bangunIsi() {
        JPanel isi = new JPanel(new BorderLayout(0, 16));
        isi.setOpaque(false);

        JPanel kolomKedaluwarsa = new JPanel(new BorderLayout(0, 2));
        kolomKedaluwarsa.setOpaque(false);
        kolomKedaluwarsa.add(Ui.field("Tanggal kedaluwarsa *",
                tanggalKedaluwarsa, pesanKedaluwarsa),
                BorderLayout.CENTER);
        JLabel petunjukKedaluwarsa = Ui.keterangan(
                "Isi sesuai tanggal kedaluwarsa barang.");
        petunjukKedaluwarsa.setName("petunjukTanggalKedaluwarsa");
        kolomKedaluwarsa.add(petunjukKedaluwarsa, BorderLayout.SOUTH);

        JPanel form = Ui.kartu("Form Penerimaan", "stock-in");
        JPanel isian = Ui.gridForm(2, Ui.AMBANG_FORM_SEMPIT,
                fieldBarang("Barang *"),
                Ui.field("Kode batch *", fieldKode),
                Ui.field(labelJumlah, fieldJumlah),
                Ui.field("Tanggal masuk *", tanggalMasuk, pesanTanggalMasuk),
                kolomKedaluwarsa);
        JPanel formIsi = new JPanel();
        formIsi.setOpaque(false);
        formIsi.setLayout(new BoxLayout(formIsi, BoxLayout.Y_AXIS));
        isian.setAlignmentX(LEFT_ALIGNMENT);
        formIsi.add(isian);
        formIsi.add(javax.swing.Box.createVerticalStrut(6));
        pesanValidasi.setAlignmentX(LEFT_ALIGNMENT);
        formIsi.add(pesanValidasi);
        JPanel aksi = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        aksi.setOpaque(false);
        aksi.setAlignmentX(LEFT_ALIGNMENT);
        aksi.add(tombolSimpan);
        formIsi.add(aksi);
        form.add(formIsi, BorderLayout.CENTER);

        JPanel daftar = Ui.kartu("Batch Barang", "package");
        JPanel daftarIsi = new JPanel(new BorderLayout(0, 4));
        daftarIsi.setOpaque(false);
        daftarIsi.add(Ui.scrollTabel(tabel, Ui.TINGGI_TABEL_STOK_MASUK),
                BorderLayout.CENTER);
        daftarIsi.add(kosong, BorderLayout.SOUTH);
        daftar.add(daftarIsi, BorderLayout.CENTER);

        isi.add(form, BorderLayout.NORTH);
        isi.add(daftar, BorderLayout.CENTER);
        add(isi, BorderLayout.CENTER);
    }

    private void simpan() {
        pesanValidasi.setText(" ");
        pesanTanggalMasuk.setText(" ");
        pesanKedaluwarsa.setText(" ");
        if (barang == null) {
            pesanValidasi.setText(
                    "Belum ada barang. Tambahkan melalui halaman Barang.");
            return;
        }
        String kode = fieldKode.getText().trim();
        int jumlah;
        try {
            jumlah = Integer.parseInt(fieldJumlah.getText().trim());
        } catch (NumberFormatException exception) {
            pesanValidasi.setText("Jumlah harus berupa angka bulat.");
            fieldJumlah.requestFocusInWindow();
            return;
        }

        LocalDate masuk = tanggalMasuk.getTanggal();
        if (masuk == null) {
            pesanTanggalMasuk.setText("Tanggal masuk wajib diisi.");
            tanggalMasuk.komponenTeks().requestFocusInWindow();
            return;
        }

        LocalDate kedaluwarsa = tanggalKedaluwarsa.getTanggal();
        if (kedaluwarsa == null) {
            pesanKedaluwarsa.setText("Tanggal kedaluwarsa wajib diisi.");
            tanggalKedaluwarsa.komponenTeks().requestFocusInWindow();
            return;
        }
        if (kedaluwarsa.isBefore(masuk)) {
            pesanKedaluwarsa.setText(
                    "Tanggal kedaluwarsa tidak boleh sebelum tanggal masuk.");
            tanggalKedaluwarsa.komponenTeks().requestFocusInWindow();
            return;
        }

        PenerimaanStok penerimaan = new PenerimaanStok(masuk);
        try {
            penerimaan.terima(barang, kode, jumlah, masuk, kedaluwarsa);
        } catch (IllegalArgumentException exception) {
            konteks.muatUlang();
            pesanValidasi.setText("Penerimaan ditolak: " + exception.getMessage());
            konteks.tampilkanPesan("Penerimaan gagal: "
                    + exception.getMessage(), false);
            return;
        }

        if (konteks.simpanTransaksi(barang, penerimaan, "Penerimaan berhasil: "
                + kode + ", " + jumlah + " " + barang.getSatuan() + ".")) {
            fieldKode.setText("");
            fieldJumlah.setText("");
            aturTanggalAwal();
            pesanValidasi.setText(" ");
        } else {
            pesanValidasi.setText("Penerimaan gagal disimpan. Lihat status.");
        }
    }

    @Override
    protected void saatBarangBerubah() {
        fieldKode.setText("");
        fieldJumlah.setText("");
        pesanValidasi.setText(" ");
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
                batch.getTanggalMasuk().toString(),
                batch.getTanggalKedaluwarsa().toString(),
                status
            });
        }
        kosong.setTitle("Belum ada batch");
        kosong.setDescription(
                "Batch baru akan muncul di sini setelah penerimaan pertama.");
        kosong.setVisible(model.getRowCount() == 0);
    }

    @Override
    public boolean adaIsianBelumDisimpan() {
        return !fieldKode.getText().trim().isEmpty()
                || !fieldJumlah.getText().trim().isEmpty()
                || !Objects.equals(tanggalMasuk.getTanggal(), tanggalMasukAwal)
                || tanggalKedaluwarsa.getTanggal() != null
                || !tanggalKedaluwarsa.komponenTeks().getText().trim().isEmpty();
    }
}
