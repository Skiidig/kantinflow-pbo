package kantinflow.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import kantinflow.PenerimaanStok;

/**
 * Halaman penerimaan stok dan daftar batch barang aktif.
 */
public class PanelStokMasuk extends PanelKantin {
    private final JTextField fieldKode = Ui.field(16);
    private final JTextField fieldJumlah = Ui.field(8);
    private final JTextField fieldTanggalMasuk = Ui.field(12);
    private final JTextField fieldTanggalKedaluwarsa = Ui.field(12);
    private final JButton tombolSimpan = Ui.utama("Simpan Penerimaan");
    private final JLabel pesanValidasi = Ui.pesan();
    private final EmptyState kosong = new EmptyState(
            "package", "Belum ada batch",
            "Batch baru akan muncul di sini setelah penerimaan pertama.");
    private final JLabel infoBarang = Ui.keterangan(" ");
    private int idBarangTerakhir = -1;
    private String tanggalMasukAwal;
    private String tanggalKedaluwarsaAwal;
    private final DefaultTableModel model = Ui.model(
            "Kode Batch", "Sisa", "Tanggal Masuk", "Tanggal Kedaluwarsa",
            "Status");
    private final JTable tabel = Ui.tabel(model);

    public PanelStokMasuk(KonteksKantin konteks) {
        super(konteks, "Stok Masuk",
                "Terima batch baru untuk barang aktif.");
        fieldKode.setName("kodeBatch");
        infoBarang.setName("konteksStokMasuk");
        fieldJumlah.setName("jumlah");
        fieldTanggalMasuk.setName("tanggalMasuk");
        fieldTanggalKedaluwarsa.setName("tanggalKedaluwarsa");
        tombolSimpan.setName("tombolSimpan");
        tombolSimpan.setIcon(Ui.ikonAksi("stock-in", 16, java.awt.Color.WHITE));
        tombolSimpan.setIconTextGap(7);
        tabel.setName("tabelBatch");
        tombolSimpan.addActionListener(event -> simpan());
        kosong.setName("kosongBatch");
        Ui.rataKanan(tabel, 1);
        Ui.pasangBadge(tabel, 4);
        Ui.aturKolom(tabel, 120, 75, 120, 135, 140);
        aturTanggalAwal();
        bangunIsi();
    }

    private void aturTanggalAwal() {
        LocalDate hariIni = LocalDate.now();
        tanggalMasukAwal = hariIni.toString();
        tanggalKedaluwarsaAwal = hariIni.plusMonths(1).toString();
        fieldTanggalMasuk.setText(tanggalMasukAwal);
        fieldTanggalKedaluwarsa.setText(tanggalKedaluwarsaAwal);
    }

    private void bangunIsi() {
        JPanel isi = new JPanel(new BorderLayout(0, 16));
        isi.setOpaque(false);

        JPanel form = Ui.kartu("Form Penerimaan", "stock-in");
        JPanel isian = Ui.gridForm(2, Ui.AMBANG_FORM_SEMPIT,
                Ui.field("Kode batch *", fieldKode),
                Ui.field("Jumlah *", fieldJumlah),
                Ui.field("Tanggal masuk * (yyyy-MM-dd)", fieldTanggalMasuk),
                Ui.field("Tanggal kedaluwarsa * (yyyy-MM-dd)",
                        fieldTanggalKedaluwarsa));
        JPanel formIsi = new JPanel();
        formIsi.setOpaque(false);
        formIsi.setLayout(new BoxLayout(formIsi, BoxLayout.Y_AXIS));
        infoBarang.setAlignmentX(LEFT_ALIGNMENT);
        infoBarang.setBorder(new EmptyBorder(0, 0, 6, 0));
        formIsi.add(infoBarang);
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

        JPanel daftar = Ui.kartu("Batch Barang Aktif", "package");
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
        String kode = fieldKode.getText().trim();
        int jumlah;
        try {
            jumlah = Integer.parseInt(fieldJumlah.getText().trim());
        } catch (NumberFormatException exception) {
            pesanValidasi.setText("Jumlah harus berupa angka bulat.");
            fieldJumlah.requestFocusInWindow();
            return;
        }

        LocalDate masuk;
        LocalDate kedaluwarsa;
        try {
            masuk = LocalDate.parse(fieldTanggalMasuk.getText().trim());
            kedaluwarsa = LocalDate.parse(
                    fieldTanggalKedaluwarsa.getText().trim());
        } catch (DateTimeParseException exception) {
            pesanValidasi.setText("Format tanggal harus yyyy-MM-dd.");
            fieldTanggalMasuk.requestFocusInWindow();
            return;
        }

        PenerimaanStok penerimaan = new PenerimaanStok(masuk);
        try {
            penerimaan.terima(konteks.getBarangAktif(), kode, jumlah, masuk,
                    kedaluwarsa);
        } catch (IllegalArgumentException exception) {
            konteks.muatUlang();
            pesanValidasi.setText("Penerimaan ditolak: " + exception.getMessage());
            konteks.tampilkanPesan("Penerimaan gagal: "
                    + exception.getMessage(), false);
            return;
        }

        if (konteks.simpanTransaksi(penerimaan, "Penerimaan berhasil: "
                + kode + ", " + jumlah + " "
                + konteks.getBarangAktif().getSatuan() + ".")) {
            fieldKode.setText("");
            fieldJumlah.setText("");
            aturTanggalAwal();
            pesanValidasi.setText(" ");
        } else {
            pesanValidasi.setText("Penerimaan gagal disimpan. Lihat status.");
        }
    }

    @Override
    public void perbarui() {
        if (idBarangTerakhir != konteks.getBarangAktif().getId()) {
            idBarangTerakhir = konteks.getBarangAktif().getId();
            fieldKode.setText("");
            fieldJumlah.setText("");
            pesanValidasi.setText(" ");
            aturTanggalAwal();
        }
        infoBarang.setText("Penerimaan untuk "
                + konteks.getBarangAktif().getNama() + " · "
                + konteks.getBarangAktif().getSatuan());
        model.setRowCount(0);
        LocalDate acuan = konteks.getTanggalAcuan();
        for (kantinflow.BatchStok batch : konteks.getBarangAktif().getDaftarBatch()) {
            String status = batch.getSisaJumlah() <= 0
                    ? "Habis" : batch.statusPada(acuan).getLabel();
            model.addRow(new Object[]{
                batch.getKode(),
                batch.getSisaJumlah() + " "
                + konteks.getBarangAktif().getSatuan(),
                batch.getTanggalMasuk().toString(),
                batch.getTanggalKedaluwarsa().toString(),
                status
            });
        }
        kosong.setVisible(model.getRowCount() == 0);
    }

    @Override
    public boolean adaIsianBelumDisimpan() {
        return !fieldKode.getText().trim().isEmpty()
                || !fieldJumlah.getText().trim().isEmpty()
                || !fieldTanggalMasuk.getText().equals(tanggalMasukAwal)
                || !fieldTanggalKedaluwarsa.getText()
                        .equals(tanggalKedaluwarsaAwal);
    }
}
