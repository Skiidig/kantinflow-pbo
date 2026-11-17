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

    public PanelRiwayat(KonteksKantin konteks) {
        super(konteks, "Riwayat",
                "Seluruh rincian transaksi barang aktif, terbaru di bawah.");
        tabel.setName("tabelRiwayat");
        bangunIsi();
    }

    private void bangunIsi() {
        JPanel isi = new JPanel(new BorderLayout(0, 12));
        isi.setOpaque(false);
        isi.add(labelJumlah, BorderLayout.NORTH);
        isi.add(Ui.scrollTabel(tabel), BorderLayout.CENTER);
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
    }
}
