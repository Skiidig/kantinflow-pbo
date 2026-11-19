package kantinflow.gui;

import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import kantinflow.Barang;

/**
 * Dasar halaman yang memiliki pilihan barang sendiri. Identitas barang
 * disimpan sebagai ID agar pilihan tetap stabil setelah data dimuat ulang.
 */
public abstract class PanelBarangTerpilih extends PanelKantin {
    protected final JComboBox<Barang> pilihanBarang = Ui.comboBarang();
    protected Barang barang;
    private int idBarang = -1;
    private boolean memperbaruiPilihan;

    protected PanelBarangTerpilih(KonteksKantin konteks, String judul,
            String keterangan) {
        super(konteks, judul, keterangan);
        pilihanBarang.addActionListener(event -> pilihDariPengguna());
    }

    protected JPanel fieldBarang(String label) {
        return Ui.field(label, pilihanBarang);
    }

    /**
     * Menyegarkan daftar pilihan dari konteks dan mengembalikan true bila ada
     * barang yang dapat dipakai halaman ini.
     */
    protected boolean perbaruiBarangTerpilih() {
        List<Barang> daftar = konteks.getDaftarBarang();
        memperbaruiPilihan = true;
        try {
            pilihanBarang.removeAllItems();
            for (Barang item : daftar) {
                pilihanBarang.addItem(item);
            }
            if (daftar.isEmpty()) {
                barang = null;
                idBarang = -1;
                pilihanBarang.setEnabled(false);
                pilihanBarang.setToolTipText(
                        "Belum ada barang. Tambahkan melalui halaman Barang.");
                return false;
            }
            pilihanBarang.setEnabled(true);
            Barang terpilih = null;
            if (idBarang >= 0) {
                for (Barang item : daftar) {
                    if (item.getId() == idBarang) {
                        terpilih = item;
                        break;
                    }
                }
            }
            if (terpilih == null) {
                // Halaman yang belum pernah memilih (atau barangnya hilang)
                // memakai barang pertama tanpa memicu konfirmasi.
                boolean pergantian = idBarang >= 0 && barang != null;
                terpilih = daftar.get(0);
                if (pergantian) {
                    barang = terpilih;
                    idBarang = terpilih.getId();
                    pilihanBarang.setSelectedItem(terpilih);
                    pilihanBarang.setToolTipText(terpilih.getNama()
                            + " (" + terpilih.getSatuan() + ")");
                    saatBarangBerubah();
                    return true;
                }
            }
            barang = terpilih;
            idBarang = terpilih.getId();
            pilihanBarang.setSelectedItem(terpilih);
            pilihanBarang.setToolTipText(terpilih.getNama()
                    + " (" + terpilih.getSatuan() + ")");
            return true;
        } finally {
            memperbaruiPilihan = false;
        }
    }

    private void pilihDariPengguna() {
        if (memperbaruiPilihan) {
            return;
        }
        Barang dipilih = (Barang) pilihanBarang.getSelectedItem();
        if (dipilih == null) {
            return;
        }
        if (barang != null && dipilih.getId() == barang.getId()) {
            return;
        }
        if (barang != null && adaIsianBelumDisimpan()
                && !DialogKonfirmasi.tampilkan(this, "Ganti barang",
                        "Formulir masih berisi isian. Ganti barang dari "
                        + barang.getNama() + " ke " + dipilih.getNama()
                        + "? Isian formulir pada halaman ini akan dibuang.",
                        "Ganti barang")) {
            kembalikanPilihan();
            konteks.tampilkanPesan(
                    "Perpindahan barang dibatalkan. Isian formulir dipertahankan.",
                    true);
            return;
        }
        barang = dipilih;
        idBarang = dipilih.getId();
        saatBarangBerubah();
        perbarui();
    }

    private void kembalikanPilihan() {
        memperbaruiPilihan = true;
        try {
            pilihanBarang.setSelectedItem(barang);
        } finally {
            memperbaruiPilihan = false;
        }
    }

    /**
     * Dipanggil hanya setelah pengguna menyetujui pergantian barang.
     */
    protected void saatBarangBerubah() {
    }
}
