package kantinflow.gui;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import kantinflow.RincianTransaksi;
import kantinflow.Transaksi;

/**
 * Halaman riwayat seluruh rincian transaksi barang aktif.
 */
public class PanelRiwayat extends PanelKantin {
    private final DefaultTableModel model = Ui.model(
            "Tanggal", "Jenis", "Kode Batch", "Jumlah", "Alasan");
    private final JTable tabel = Ui.tabel(model);
    private final JLabel labelJumlah = Ui.keterangan("0 rincian transaksi");
    private final JLabel kosong = Ui.kosong(
            "Belum ada transaksi untuk barang aktif ini.");

    public PanelRiwayat(KonteksKantin konteks) {
        super(konteks, "Riwayat",
                "Rincian transaksi barang aktif.");
        tabel.setName("tabelRiwayat");
        Ui.rataKanan(tabel, 3);
        tabel.getColumnModel().getColumn(0).setPreferredWidth(90);
        tabel.getColumnModel().getColumn(1).setPreferredWidth(110);
        tabel.getColumnModel().getColumn(2).setPreferredWidth(95);
        tabel.getColumnModel().getColumn(3).setPreferredWidth(80);
        tabel.getColumnModel().getColumn(4).setPreferredWidth(190);
        bangunIsi();
    }

    private void bangunIsi() {
        JPanel isi = new JPanel(new BorderLayout(0, 10));
        isi.setOpaque(false);
        JPanel kepala = new JPanel(new BorderLayout());
        kepala.setOpaque(false);
        kepala.add(labelJumlah, BorderLayout.WEST);
        JLabel petunjuk = Ui.keterangan(
                "Tanggal · jenis transaksi · batch · jumlah · alasan");
        petunjuk.setBorder(new javax.swing.border.EmptyBorder(0, 0, 0, 0));
        kepala.add(petunjuk, BorderLayout.EAST);
        isi.add(kepala, BorderLayout.NORTH);
        JPanel daftar = Ui.kartu();
        JPanel daftarIsi = new JPanel(new BorderLayout(0, 4));
        daftarIsi.setOpaque(false);
        javax.swing.JScrollPane scrollTabel = Ui.scrollTabel(tabel);
        scrollTabel.setPreferredSize(new java.awt.Dimension(600, 300));
        daftarIsi.add(scrollTabel, BorderLayout.CENTER);
        daftarIsi.add(kosong, BorderLayout.SOUTH);
        daftar.add(daftarIsi, BorderLayout.CENTER);
        isi.add(daftar, BorderLayout.CENTER);
        add(isi, BorderLayout.CENTER);
    }

    @Override
    public void perbarui() {
        model.setRowCount(0);
        for (Transaksi transaksi : konteks.getRiwayat()) {
            String alasan = transaksi.getAlasan() == null
                    ? "" : transaksi.getAlasan();
            for (RincianTransaksi rincian : transaksi.getRincian()) {
                model.addRow(new Object[]{
                    transaksi.getTanggal().toString(),
                    transaksi.getJenis(),
                    rincian.getBatch().getKode(),
                    rincian.getJumlah() + " "
                    + konteks.getBarangAktif().getSatuan(),
                    alasan
                });
            }
        }
        labelJumlah.setText(model.getRowCount() + " rincian transaksi");
        kosong.setVisible(model.getRowCount() == 0);
    }
}
