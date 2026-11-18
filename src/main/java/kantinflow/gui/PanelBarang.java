package kantinflow.gui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import kantinflow.Barang;

/**
 * Halaman daftar barang, penambahan barang, dan minimum stok.
 */
public class PanelBarang extends PanelKantin {
    private final DefaultTableModel model = Ui.model(
            "Nama", "Satuan", "Minimum", "Saldo");
    private final JTable tabel = Ui.tabel(model);
    private final List<Barang> urutanBarang = new ArrayList<>();
    private final JTextField fieldNama = Ui.field(16);
    private final JTextField fieldSatuan = Ui.field(10);
    private final JButton tombolTambah = Ui.utama("Tambah Barang");
    private final JTextField fieldMinimum = Ui.field(8);
    private final JButton tombolMinimum = Ui.utama("Simpan Minimum");
    private final JLabel labelMinimumAktif = new JLabel("-");
    private final JLabel pesanNama = Ui.pesan();
    private final JLabel pesanSatuan = Ui.pesan();
    private final JLabel pesanTambah = Ui.pesan();
    private final JLabel pesanMinimum = Ui.pesan();
    private final JLabel kosong = Ui.kosong(
            "Belum ada barang. Tambahkan barang pertama melalui formulir di bawah.");
    private boolean memperbaruiTabel;

    public PanelBarang(KonteksKantin konteks) {
        super(konteks, "Barang",
                "Kelola daftar barang dan batas minimum.");
        tabel.setName("tabelBarang");
        fieldNama.setName("namaBarangBaru");
        fieldSatuan.setName("satuanBarangBaru");
        tombolTambah.setName("tombolTambahBarang");
        fieldMinimum.setName("minimumBarang");
        tombolMinimum.setName("tombolSimpanMinimum");
        tombolTambah.addActionListener(event -> tambah());
        tombolMinimum.addActionListener(event -> simpanMinimum());
        tabel.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && !memperbaruiTabel) {
                int baris = tabel.getSelectedRow();
                if (baris >= 0 && baris < urutanBarang.size()) {
                    Barang dipilih = urutanBarang.get(baris);
                    if (dipilih.getId() != konteks.getBarangAktif().getId()) {
                        konteks.gantiBarang(dipilih);
                    }
                }
            }
        });
        Ui.rataKanan(tabel, 2, 3);
        bangunIsi();
    }

    private void bangunIsi() {
        JPanel isi = new JPanel(new BorderLayout(0, 16));
        isi.setOpaque(false);

        JPanel daftar = Ui.kartu("Daftar Barang");
        JLabel petunjuk = Ui.keterangan(
                "Pilih baris untuk menjadikan barang aktif.");
        JPanel daftarIsi = new JPanel(new BorderLayout(0, 6));
        daftarIsi.setOpaque(false);
        daftarIsi.add(petunjuk, BorderLayout.NORTH);
        daftarIsi.add(Ui.scrollTabel(tabel, Ui.TINGGI_TABEL_BARANG),
                BorderLayout.CENTER);
        JPanel bawahDaftar = new JPanel();
        bawahDaftar.setOpaque(false);
        bawahDaftar.setLayout(new BoxLayout(bawahDaftar, BoxLayout.Y_AXIS));
        bawahDaftar.add(kosong);
        daftarIsi.add(bawahDaftar, BorderLayout.SOUTH);
        daftar.add(daftarIsi, BorderLayout.CENTER);

        JPanel tambah = Ui.kartu("Tambah Barang");
        JPanel tambahIsi = new JPanel();
        tambahIsi.setOpaque(false);
        tambahIsi.setLayout(new BoxLayout(tambahIsi, BoxLayout.Y_AXIS));
        tambahIsi.add(Ui.field("Nama barang *", fieldNama, pesanNama));
        tambahIsi.add(Box.createVerticalStrut(8));
        tambahIsi.add(Ui.field("Satuan *", fieldSatuan, pesanSatuan));
        tambahIsi.add(Box.createVerticalStrut(8));
        tambahIsi.add(pesanTambah);
        tambahIsi.add(Box.createVerticalStrut(4));
        JPanel tombolTambahPanel = new JPanel(new BorderLayout());
        tombolTambahPanel.setOpaque(false);
        tombolTambahPanel.add(tombolTambah, BorderLayout.EAST);
        tambahIsi.add(tombolTambahPanel);
        tambah.add(tambahIsi, BorderLayout.CENTER);

        JPanel minimum = Ui.kartu("Minimum Barang Aktif");
        JPanel minimumIsi = new JPanel();
        minimumIsi.setOpaque(false);
        minimumIsi.setLayout(new BoxLayout(minimumIsi, BoxLayout.Y_AXIS));
        labelMinimumAktif.setFont(Ui.font(Font.BOLD, Ui.FONT_BODY));
        labelMinimumAktif.setForeground(Ui.AKSEN);
        labelMinimumAktif.setBorder(new EmptyBorder(0, 0, 10, 0));
        minimumIsi.add(labelMinimumAktif);
        minimumIsi.add(Ui.field(
                "Stok minimum * (bilangan bulat >= 0)", fieldMinimum, pesanMinimum));
        minimumIsi.add(Box.createVerticalStrut(6));
        JPanel tombolMinimumPanel = new JPanel(new BorderLayout());
        tombolMinimumPanel.setOpaque(false);
        tombolMinimumPanel.add(tombolMinimum, BorderLayout.EAST);
        minimumIsi.add(tombolMinimumPanel);
        minimumIsi.add(Box.createVerticalStrut(8));
        JLabel catatan = Ui.keterangan(
                "Nilai minimum dipakai Ringkasan untuk menentukan status restok.");
        minimumIsi.add(catatan);
        minimum.add(minimumIsi, BorderLayout.CENTER);

        JPanel form = Ui.gridResponsif(2, Ui.AMBANG_FORM_SEMPIT, tambah, minimum);

        isi.add(daftar, BorderLayout.NORTH);
        isi.add(form, BorderLayout.CENTER);
        add(isi, BorderLayout.CENTER);
    }

    private void tambah() {
        pesanNama.setText(" ");
        pesanSatuan.setText(" ");
        pesanTambah.setText(" ");
        String nama = fieldNama.getText().trim();
        String satuan = fieldSatuan.getText().trim();
        if (nama.isEmpty()) {
            pesanNama.setText("Nama barang wajib diisi.");
            fieldNama.requestFocusInWindow();
            return;
        }
        if (satuan.isEmpty()) {
            pesanSatuan.setText("Satuan wajib diisi.");
            fieldSatuan.requestFocusInWindow();
            return;
        }
        if (konteks.tambahBarang(nama, satuan)) {
            fieldNama.setText("");
            fieldSatuan.setText("");
        } else {
            pesanTambah.setText("Tambah barang gagal. Lihat status di bawah.");
        }
    }

    private void simpanMinimum() {
        pesanMinimum.setText(" ");
        try {
            int minimum = Integer.parseInt(fieldMinimum.getText().trim());
            if (minimum < 0) {
                pesanMinimum.setText("Stok minimum tidak boleh negatif.");
                fieldMinimum.requestFocusInWindow();
                return;
            }
            if (konteks.simpanStokMinimum(minimum)) {
                pesanMinimum.setText(" ");
            } else {
                pesanMinimum.setText("Gagal menyimpan minimum. Lihat status.");
            }
        } catch (NumberFormatException exception) {
            pesanMinimum.setText("Stok minimum harus berupa angka bulat.");
            fieldMinimum.requestFocusInWindow();
        }
    }

    @Override
    public void perbarui() {
        memperbaruiTabel = true;
        try {
            urutanBarang.clear();
            urutanBarang.addAll(konteks.getDaftarBarang());
            model.setRowCount(0);
            int barisAktif = -1;
            for (int i = 0; i < urutanBarang.size(); i++) {
                Barang barang = urutanBarang.get(i);
                model.addRow(new Object[]{
                    barang.getNama(),
                    barang.getSatuan(),
                    barang.getStokMinimum() + " " + barang.getSatuan(),
                    barang.hitungSaldo() + " " + barang.getSatuan()
                });
                if (barang.getId() == konteks.getBarangAktif().getId()) {
                    barisAktif = i;
                }
            }
            tabel.clearSelection();
            if (barisAktif >= 0) {
                tabel.setRowSelectionInterval(barisAktif, barisAktif);
            }
            labelMinimumAktif.setText("Barang aktif: "
                    + konteks.getBarangAktif().getNama()
                    + " (" + konteks.getBarangAktif().getSatuan() + ")");
            fieldMinimum.setText(String.valueOf(
                    konteks.getBarangAktif().getStokMinimum()));
            kosong.setVisible(model.getRowCount() == 0);
        } finally {
            memperbaruiTabel = false;
        }
    }
}
