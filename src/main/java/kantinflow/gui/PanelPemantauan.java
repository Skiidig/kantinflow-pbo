package kantinflow.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.time.LocalDate;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import kantinflow.Barang;
import kantinflow.BatchStok;
import kantinflow.StatusKedaluwarsa;

/**
 * Halaman pemantauan kedaluwarsa batch bersisa untuk seluruh barang atau
 * satu barang tertentu.
 */
public class PanelPemantauan extends PanelKantin {
    private final JComboBox<PilihanFilter> pilihanFilter = Ui.combo();
    private final PemilihTanggal tanggalPilihan = new PemilihTanggal();
    private final JButton tombolPeriksa = Ui.utama("Periksa");
    private final JLabel pesanValidasi = Ui.pesan();
    private final JLabel pesanTanggal = Ui.pesan();
    private LocalDate tanggalTerakhir;
    private LocalDate tanggalKonteksTerakhir;
    private int idFilter = -1;
    private boolean memperbaruiFilter;
    private final DefaultTableModel model = Ui.model(
            "Barang", "Kode Batch", "Sisa", "Satuan", "Kedaluwarsa", "Status");
    private final JTable tabel = Ui.tabel(model);
    private final JLabel labelInfo = Ui.keterangan(" ");
    private final EmptyState kosong = new EmptyState(
            "alert-triangle", "Tidak ada batch bersisa",
            "Tidak ada batch bersisa untuk seluruh barang.");
    private final EmptyState kosongBarang = new EmptyState(
            "package", "Belum ada barang",
            "Tambahkan barang melalui halaman Barang untuk mulai memantau "
            + "kedaluwarsa.");
    private final JPanel hasil = new JPanel(new CardLayout());

    public PanelPemantauan(KonteksKantin konteks) {
        super(konteks, "Pemantauan",
                "Periksa status kedaluwarsa batch seluruh barang atau barang "
                + "terpilih.");
        pilihanFilter.setName("barangPemantauan");
        pilihanFilter.addItem(PilihanFilter.SEMUA);
        pilihanFilter.setSelectedIndex(0);
        pilihanFilter.addActionListener(event -> pilihFilter());
        tanggalPilihan.komponenTeks().setName("tanggalPemantauan");
        pesanTanggal.setName("pesanTanggalPemantauan");
        tanggalTerakhir = konteks.getTanggalAcuan();
        tanggalKonteksTerakhir = tanggalTerakhir;
        tanggalPilihan.setTanggal(tanggalTerakhir);
        tombolPeriksa.setName("tombolPeriksa");
        tombolPeriksa.setIcon(Ui.ikonAksi("monitor", 16, java.awt.Color.WHITE));
        tombolPeriksa.setIconTextGap(7);
        tabel.setName("tabelPemantauan");
        labelInfo.setName("infoPemantauan");
        pesanValidasi.setName("pesanPemantauan");
        kosong.setName("kosongPemantauan");
        kosongBarang.setName("kosongBarangPemantauan");
        hasil.setName("hasilPemantauan");
        hasil.setOpaque(false);
        tombolPeriksa.addActionListener(event -> periksa());
        Ui.rataKanan(tabel, 2);
        Ui.pasangTeksStatus(tabel, 5);
        Ui.aturKolom(tabel, 150, 120, 80, 85, 120, 150);

        bangunIsi();
    }

    private void bangunIsi() {
        JPanel isi = new JPanel(new BorderLayout(0, 16));
        isi.setOpaque(false);

        JPanel kontrol = Ui.kartu("Filter pemantauan", "monitor");
        JPanel kontrolIsi = new JPanel(new BorderLayout(0, 8));
        kontrolIsi.setOpaque(false);
        JPanel baris = Ui.barisFilter(
                Ui.field("Barang *", pilihanFilter),
                Ui.field("Tanggal pemeriksaan *", tanggalPilihan, pesanTanggal),
                Ui.field(" ", tombolPeriksa));
        kontrolIsi.add(baris, BorderLayout.NORTH);
        kontrolIsi.add(pesanValidasi, BorderLayout.CENTER);
        kontrolIsi.add(labelInfo, BorderLayout.SOUTH);
        kontrol.add(kontrolIsi, BorderLayout.CENTER);

        JPanel daftar = Ui.kartu("Status Batch", "alert-triangle");
        JPanel daftarIsi = new JPanel(new BorderLayout(0, 4));
        daftarIsi.setOpaque(false);
        JScrollPane gulir = Ui.scrollTabel(tabel, Ui.TINGGI_TABEL_PEMANTAUAN);
        hasil.add(gulir, "tabel");
        hasil.add(kosong, "kosong");
        hasil.add(kosongBarang, "kosongBarang");
        daftarIsi.add(hasil, BorderLayout.CENTER);
        daftar.add(daftarIsi, BorderLayout.CENTER);

        isi.add(kontrol, BorderLayout.NORTH);
        isi.add(daftar, BorderLayout.CENTER);
        add(isi, BorderLayout.CENTER);
    }

    private void pilihFilter() {
        if (memperbaruiFilter) {
            return;
        }
        PilihanFilter dipilih = (PilihanFilter) pilihanFilter.getSelectedItem();
        if (dipilih == null || dipilih.idBarang() == idFilter) {
            return;
        }
        idFilter = dipilih.idBarang();
        pesanValidasi.setText(" ");
        perbarui();
    }

    private void periksa() {
        if (konteks.getDaftarBarang().isEmpty()) {
            pesanValidasi.setText(
                    "Belum ada barang. Tambahkan melalui halaman Barang.");
            return;
        }
        if (tanggalPilihan.ambilTeksTidakValid()) {
            pesanTanggal.setText("Tanggal tidak valid. Pilih tanggal yang benar.");
            tanggalPilihan.komponenTeks().requestFocusInWindow();
            return;
        }
        LocalDate dipilih = tanggalPilihan.getTanggal();
        if (dipilih == null) {
            pesanTanggal.setText("Tanggal pemeriksaan wajib diisi.");
            tanggalPilihan.komponenTeks().requestFocusInWindow();
            return;
        }
        if (!tanggalPilihan.isTextFieldValid()) {
            pesanTanggal.setText("Tanggal tidak valid. Pilih tanggal yang benar.");
            tanggalPilihan.komponenTeks().requestFocusInWindow();
            return;
        }
        pesanTanggal.setText(" ");
        pesanValidasi.setText(" ");
        tanggalTerakhir = dipilih;
        tanggalKonteksTerakhir = dipilih;
        konteks.setTanggalPemantauan(dipilih);
        konteks.tampilkanPesan("Pemantauan "
                + PemilihTanggal.format(dipilih) + " selesai: "
                + model.getRowCount() + " batch bersisa diperiksa.", true);
    }

    /**
     * Menyegarkan pilihan filter dari konteks. Pilihan barang dipertahankan
     * berdasarkan ID agar tidak kembali ke "Semua Barang" setelah transaksi.
     */
    private boolean perbaruiFilter() {
        List<Barang> daftar = konteks.getDaftarBarang();
        memperbaruiFilter = true;
        try {
            pilihanFilter.removeAllItems();
            pilihanFilter.addItem(PilihanFilter.SEMUA);
            for (Barang item : daftar) {
                pilihanFilter.addItem(new PilihanFilter(item));
            }
            if (daftar.isEmpty()) {
                pilihanFilter.setSelectedIndex(0);
                pilihanFilter.setEnabled(false);
                pilihanFilter.setToolTipText(
                        "Belum ada barang. Tambahkan melalui halaman Barang.");
                idFilter = -1;
                return false;
            }
            int indeks = 0;
            if (idFilter >= 0) {
                for (int i = 0; i < daftar.size(); i++) {
                    if (daftar.get(i).getId() == idFilter) {
                        indeks = i + 1;
                        break;
                    }
                }
            }
            pilihanFilter.setSelectedIndex(indeks);
            PilihanFilter terpilih =
                    (PilihanFilter) pilihanFilter.getSelectedItem();
            idFilter = terpilih == null ? -1 : terpilih.idBarang();
            pilihanFilter.setEnabled(true);
            pilihanFilter.setToolTipText(idFilter < 0
                    ? "Menampilkan batch bersisa dari seluruh barang."
                    : "Menampilkan batch bersisa dari barang terpilih.");
            return true;
        } finally {
            memperbaruiFilter = false;
        }
    }

    private Barang barangFilter() {
        Object terpilih = pilihanFilter.getSelectedItem();
        return terpilih instanceof PilihanFilter filter
                ? filter.barang() : null;
    }

    @Override
    public void perbarui() {
        boolean adaBarang = perbaruiFilter();
        LocalDate dariKonteks = konteks.getTanggalAcuan();
        LocalDate dipilih = tanggalPilihan.getTanggal();
        if (dipilih != null && dipilih.equals(tanggalKonteksTerakhir)
                && !dipilih.equals(dariKonteks)) {
            tanggalPilihan.setTanggal(dariKonteks);
            dipilih = dariKonteks;
        }
        tanggalKonteksTerakhir = dariKonteks;
        if (dipilih != null) {
            tanggalTerakhir = dipilih;
        }
        LocalDate acuan = tanggalTerakhir;

        if (!adaBarang) {
            model.setRowCount(0);
            labelInfo.setText("Tambahkan barang melalui halaman Barang "
                    + "untuk mulai memantau kedaluwarsa.");
            pesanValidasi.setText(" ");
            tombolPeriksa.setEnabled(false);
            tampilkanHasil("kosongBarang");
            return;
        }

        tombolPeriksa.setEnabled(true);
        Barang barang = barangFilter();
        if (idFilter < 0) {
            labelInfo.setText("Pemeriksaan terakhir untuk tanggal "
                    + PemilihTanggal.format(acuan)
                    + ". Menampilkan batch bersisa dari seluruh barang.");
        } else {
            labelInfo.setText("Pemeriksaan terakhir untuk tanggal "
                    + PemilihTanggal.format(acuan) + " untuk "
                    + (barang == null ? "barang terpilih" : barang.getNama())
                    + ".");
        }

        model.setRowCount(0);
        if (idFilter < 0) {
            for (Barang item : konteks.getDaftarBarang()) {
                tambahBaris(item, acuan);
            }
        } else if (barang != null) {
            tambahBaris(barang, acuan);
        }

        if (model.getRowCount() == 0) {
            siapkanKosong();
            tampilkanHasil("kosong");
        } else {
            tampilkanHasil("tabel");
        }
    }

    private void tambahBaris(Barang barang, LocalDate acuan) {
        for (BatchStok batch : barang.getDaftarBatch()) {
            if (batch.getSisaJumlah() <= 0) {
                continue;
            }
            StatusKedaluwarsa status = batch.statusPada(acuan);
            model.addRow(new Object[]{
                barang.getNama(),
                batch.getKode(),
                batch.getSisaJumlah(),
                barang.getSatuan(),
                batch.getTanggalKedaluwarsa().toString(),
                status.getLabel()
            });
        }
    }

    private void siapkanKosong() {
        kosong.setTitle("Tidak ada batch bersisa");
        kosong.setDescription(idFilter < 0
                ? "Tidak ada batch bersisa untuk seluruh barang."
                : "Tidak ada batch bersisa untuk barang ini.");
    }

    private void tampilkanHasil(String kartu) {
        ((CardLayout) hasil.getLayout()).show(hasil, kartu);
    }

    /**
     * Pilihan filter terpisah dari {@link Barang}; {@code null} mewakili
     * seluruh barang tanpa membuat barang semu.
     */
    private static final class PilihanFilter {
        private static final PilihanFilter SEMUA = new PilihanFilter(null);

        private final Barang barang;

        private PilihanFilter(Barang barang) {
            this.barang = barang;
        }

        private int idBarang() {
            return barang == null ? -1 : barang.getId();
        }

        private Barang barang() {
            return barang;
        }

        @Override
        public String toString() {
            return barang == null ? "Semua Barang"
                    : barang.getNama() + " (" + barang.getSatuan() + ")";
        }
    }
}
