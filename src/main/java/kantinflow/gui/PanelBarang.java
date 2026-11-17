package kantinflow.gui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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
    private boolean memperbaruiTabel;

    public PanelBarang(KonteksKantin konteks) {
        super(konteks, "Barang",
                "Kelola daftar barang dan batas stok minimum barang aktif.");
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
        bangunIsi();
    }

    private void bangunIsi() {
        JPanel isi = new JPanel(new BorderLayout(0, 16));
        isi.setOpaque(false);

        JPanel daftar = Ui.kartu("Daftar Barang");
        daftar.add(Ui.scrollTabel(tabel), BorderLayout.CENTER);

        JPanel tambah = Ui.kartu("Tambah Barang");
        JPanel tambahIsi = new JPanel(new GridLayout(2, 1, 0, 8));
        tambahIsi.setOpaque(false);
        tambahIsi.add(Ui.field("Nama barang", fieldNama));
        tambahIsi.add(Ui.field("Satuan", fieldSatuan));
        tambah.add(tambahIsi, BorderLayout.CENTER);
        tambah.add(tombolTambah, BorderLayout.SOUTH);

        JPanel minimum = Ui.kartu("Minimum Barang Aktif");
        JPanel minimumIsi = new JPanel(new BorderLayout(0, 8));
        minimumIsi.setOpaque(false);
        labelMinimumAktif.setFont(Ui.font(Font.BOLD, 13f));
        labelMinimumAktif.setForeground(Ui.AKSEN);
        minimumIsi.add(labelMinimumAktif, BorderLayout.NORTH);
        JPanel minBaris = new JPanel(new BorderLayout(8, 4));
        minBaris.setOpaque(false);
        minBaris.add(Ui.field("Stok minimum (bilangan bulat >= 0)", fieldMinimum),
                BorderLayout.NORTH);
        minBaris.add(tombolMinimum, BorderLayout.SOUTH);
        minimumIsi.add(minBaris, BorderLayout.CENTER);
        minimum.add(minimumIsi, BorderLayout.CENTER);

        JPanel form = new JPanel(new GridLayout(1, 2, 16, 0));
        form.setOpaque(false);
        form.add(tambah);
        form.add(minimum);

        isi.add(daftar, BorderLayout.NORTH);
        isi.add(form, BorderLayout.CENTER);
        add(isi, BorderLayout.CENTER);
    }

    private void tambah() {
        String nama = fieldNama.getText().trim();
        String satuan = fieldSatuan.getText().trim();
        if (konteks.tambahBarang(nama, satuan)) {
            fieldNama.setText("");
            fieldSatuan.setText("");
        }
    }

    private void simpanMinimum() {
        try {
            int minimum = Integer.parseInt(fieldMinimum.getText().trim());
            if (minimum < 0) {
                konteks.tampilkanPesan("Stok minimum tidak boleh negatif.", false);
                return;
            }
            konteks.simpanStokMinimum(minimum);
        } catch (NumberFormatException exception) {
            konteks.tampilkanPesan("Stok minimum harus berupa angka bulat.", false);
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
        } finally {
            memperbaruiTabel = false;
        }
    }
}
