package kantinflow.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

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
    private JScrollPane gulirTabel;
    private final TableRowSorter<DefaultTableModel> sorter =
            new TableRowSorter<>(model);
    private final List<BarisPemantauan> barisModel = new ArrayList<>();
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
        tabel.setRowSorter(sorter);
        sorter.setComparator(2, Comparator.nullsLast(Comparator.naturalOrder()));
        sorter.setComparator(4, Comparator.nullsLast(Comparator.naturalOrder()));
        labelInfo.setName("infoPemantauan");
        pesanValidasi.setName("pesanPemantauan");
        kosongkanPesan(pesanValidasi);
        kosongkanPesan(pesanTanggal);
        kosong.setName("kosongPemantauan");
        kosongBarang.setName("kosongBarangPemantauan");
        hasil.setName("hasilPemantauan");
        hasil.setOpaque(false);
        tombolPeriksa.addActionListener(event -> periksa());
        Ui.rataKanan(tabel, 2);
        Ui.pasangTeksStatus(tabel, 5);
        Ui.aturKolom(tabel, 145, 175, 75, 80, 125, 150);
        Ui.gulirHorizontalSaatSempit(tabel);

        bangunIsi();
    }

    private void bangunIsi() {
        JPanel isi = new JPanel(new BorderLayout(0, 16));
        isi.setOpaque(false);

        JPanel kontrol = Ui.kartu("Filter pemantauan", "monitor");
        JPanel kontrolIsi = new JPanel(new BorderLayout(0, 0));
        kontrolIsi.setOpaque(false);
        pesanValidasi.setBorder(new javax.swing.border.EmptyBorder(
                Ui.skala(6), 0, 0, 0));
        JPanel baris = Ui.barisFilter(
                Ui.field("Barang", pilihanFilter),
                Ui.field("Tanggal pemeriksaan", tanggalPilihan, pesanTanggal),
                Ui.field(" ", tombolPeriksa));
        kontrolIsi.add(baris, BorderLayout.NORTH);
        kontrolIsi.add(pesanValidasi, BorderLayout.CENTER);
        kontrolIsi.add(labelInfo, BorderLayout.SOUTH);
        kontrol.add(kontrolIsi, BorderLayout.CENTER);

        JPanel daftar = Ui.kartu("Status Batch", "alert-triangle");
        JPanel daftarIsi = new JPanel(new BorderLayout(0, 4));
        daftarIsi.setOpaque(false);
        gulirTabel = Ui.scrollTabel(tabel, Ui.TINGGI_TABEL_PEMANTAUAN);
        hasil.add(gulirTabel, "tabel");
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
        kosongkanPesan(pesanValidasi);
        perbarui();
    }

    private void periksa() {
        if (konteks.getDaftarBarang().isEmpty()) {
            aturPesan(pesanValidasi,
                    "Belum ada barang. Tambahkan melalui halaman Barang.");
            return;
        }
        if (tanggalPilihan.ambilTeksTidakValid()) {
            aturPesan(pesanTanggal,
                    "Tanggal tidak valid. Pilih tanggal yang benar.");
            tanggalPilihan.komponenTeks().requestFocusInWindow();
            return;
        }
        LocalDate dipilih = tanggalPilihan.getTanggal();
        if (dipilih == null) {
            aturPesan(pesanTanggal, "Tanggal pemeriksaan wajib diisi.");
            tanggalPilihan.komponenTeks().requestFocusInWindow();
            return;
        }
        if (!tanggalPilihan.isTextFieldValid()) {
            aturPesan(pesanTanggal,
                    "Tanggal tidak valid. Pilih tanggal yang benar.");
            tanggalPilihan.komponenTeks().requestFocusInWindow();
            return;
        }
        kosongkanPesan(pesanTanggal);
        kosongkanPesan(pesanValidasi);
        tanggalTerakhir = dipilih;
        tanggalKonteksTerakhir = dipilih;
        konteks.setTanggalPemantauan(dipilih);
        konteks.tampilkanPesan("Pemantauan "
                + FormatTanggal.format(dipilih) + " selesai: "
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
        if (!dariKonteks.equals(tanggalKonteksTerakhir)) {
            tanggalPilihan.setTanggal(dariKonteks);
            kosongkanPesan(pesanTanggal);
        }
        tanggalKonteksTerakhir = dariKonteks;
        tanggalTerakhir = dariKonteks;
        LocalDate acuan = dariKonteks;

        if (!adaBarang) {
            tabel.clearSelection();
            barisModel.clear();
            model.setRowCount(0);
            labelInfo.setText("Tambahkan barang melalui halaman Barang "
                    + "untuk mulai memantau kedaluwarsa.");
            kosongkanPesan(pesanValidasi);
            tombolPeriksa.setEnabled(false);
            tampilkanHasil("kosongBarang");
            return;
        }

        tombolPeriksa.setEnabled(true);
        Barang barang = barangFilter();
        if (idFilter < 0) {
            labelInfo.setText("Pemeriksaan terakhir untuk tanggal "
                    + FormatTanggal.format(acuan)
                    + ". Menampilkan batch bersisa dari seluruh barang.");
        } else {
            labelInfo.setText("Pemeriksaan terakhir untuk tanggal "
                    + FormatTanggal.format(acuan) + " untuk "
                    + (barang == null ? "barang terpilih" : barang.getNama())
                    + ".");
        }

        tabel.clearSelection();
        barisModel.clear();
        model.setRowCount(0);
        if (idFilter < 0) {
            for (Barang item : konteks.getDaftarBarang()) {
                tambahBaris(item, acuan);
            }
        } else if (barang != null) {
            tambahBaris(barang, acuan);
        }

        Ui.sesuaikanTinggiTabel(gulirTabel, tabel, 8);
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
            barisModel.add(new BarisPemantauan(
                    barang.getId(),
                    batch.getKode(),
                    acuan,
                    status,
                    batch.getSisaJumlah()));
            model.addRow(new Object[]{
                barang.getNama(),
                batch.getKode(),
                batch.getSisaJumlah(),
                barang.getSatuan(),
                batch.getTanggalKedaluwarsa(),
                status.getLabel()
            });
        }
    }

    private BarisPemantauan barisTerpilih() {
        int tampilan = tabel.getSelectedRow();
        if (tampilan < 0 || tampilan >= tabel.getRowCount()
                || tampilan >= barisModel.size()) {
            return null;
        }
        int modelIndex;
        try {
            modelIndex = tabel.convertRowIndexToModel(tampilan);
        } catch (IndexOutOfBoundsException exception) {
            return null;
        }
        if (modelIndex < 0 || modelIndex >= barisModel.size()) {
            return null;
        }
        return barisModel.get(modelIndex);
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

    private void siapkanKosong() {
        kosong.setTitle("Tidak ada batch bersisa");
        kosong.setDescription(idFilter < 0
                ? "Tidak ada batch bersisa untuk seluruh barang."
                : "Tidak ada batch bersisa untuk barang ini.");
    }

    private void tampilkanHasil(String kartu) {
        ((CardLayout) hasil.getLayout()).show(hasil, kartu);
    }

    private record BarisPemantauan(
            int idBarang,
            String kodeBatch,
            LocalDate tanggalAcuan,
            StatusKedaluwarsa status,
            int sisa) {

        private boolean bisaDibuang() {
            return sisa > 0 && status == StatusKedaluwarsa.KEDALUWARSA;
        }
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
