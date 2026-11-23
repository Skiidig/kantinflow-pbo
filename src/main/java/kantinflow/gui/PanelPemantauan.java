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
    private static final long serialVersionUID = 1L;
    private final JComboBox<PilihanFilter> pilihanFilter = Ui.combo();
    private final JComboBox<PilihanStatus> pilihanStatus = Ui.combo();
    private final PemilihTanggal tanggalPilihan = new PemilihTanggal();

    private final JButton tombolPeriksa = Ui.utama("Periksa");
    private final JButton tombolResetStatus = Ui.ghost("Tampilkan Semua Status");
    private final JButton tombolBuang = Ui.sekunder("Buang Batch Terpilih");
    private final JLabel petunjukBuang = Ui.keterangan(
            "Pilih baris berstatus Kedaluwarsa untuk membuka form Pembuangan.");
    private JPanel aksiTabel;
    private final JLabel pesanValidasi = Ui.pesan();
    private final JLabel pesanTanggal = Ui.pesan();
    private LocalDate tanggalTerakhir;
    private LocalDate tanggalKonteksTerakhir;
    private int idFilter = -1;
    private StatusKedaluwarsa statusFilter;
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
        pilihanStatus.setName("statusPemantauan");
        pilihanStatus.addItem(PilihanStatus.SEMUA);
        for (StatusKedaluwarsa s : StatusKedaluwarsa.values()) {
            pilihanStatus.addItem(new PilihanStatus(s));
        }
        pilihanStatus.setSelectedIndex(0);
        pilihanStatus.addActionListener(event -> pilihStatus());
        tombolResetStatus.addActionListener(
                event -> terapkanFilterStatus(null));
        tanggalPilihan.komponenTeks().setName("tanggalPemantauan");
        pesanTanggal.setName("pesanTanggalPemantauan");
        tanggalTerakhir = konteks.getTanggalAcuan();
        tanggalKonteksTerakhir = tanggalTerakhir;
        tanggalPilihan.setTanggal(tanggalTerakhir);
        tombolPeriksa.setName("tombolPeriksa");
        tombolPeriksa.setIcon(Ui.ikonAksi("monitor", 16, java.awt.Color.WHITE));
        tombolPeriksa.setIconTextGap(7);
        tombolBuang.setName("tombolBuangPemantauan");
        tombolBuang.setIcon(Ui.ikonAksi("trash", 15, Ui.MERAH_BADGE));
        tombolBuang.setIconTextGap(7);
        tombolBuang.setEnabled(false);
        tombolBuang.addActionListener(event -> buangTerpilih());
        tabel.setName("tabelPemantauan");
        tabel.setRowSorter(sorter);
        sorter.setComparator(2, Comparator.nullsLast(Comparator.naturalOrder()));
        sorter.setComparator(4, Comparator.nullsLast(Comparator.naturalOrder()));
        tabel.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                perbaruiAksiBuang();
            }
        });
        sorter.addRowSorterListener(event -> perbaruiAksiBuang());
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
        Ui.pasangBadge(tabel, 5);
        Ui.aturKolom(tabel, 145, 175, 75, 80, 125, 175);
        tabel.getColumnModel().getColumn(5).setMinWidth(Ui.skala(165));
        Ui.gulirHorizontalSaatSempit(tabel);
        Ui.headerKlik(tabel);

        bangunIsi();
    }

    private void bangunIsi() {
        JPanel isi = new JPanel(new BorderLayout(0, Ui.skala(Ui.GAP_KARTU)));
        isi.setOpaque(false);

        JPanel kontrol = Ui.kartu("Filter Pemantauan");
        JPanel kontrolIsi = new JPanel(new BorderLayout(0, 0));
        kontrolIsi.setOpaque(false);
        pesanValidasi.setBorder(new javax.swing.border.EmptyBorder(
                Ui.skala(6), 0, 0, 0));
        JPanel baris = Ui.barisFilter(
                Ui.field("Barang", pilihanFilter),
                Ui.field("Status", pilihanStatus),
                Ui.field("Tanggal pemeriksaan", tanggalPilihan, pesanTanggal),
                Ui.field(" ", tombolPeriksa));
        kontrolIsi.add(baris, BorderLayout.NORTH);
        kontrolIsi.add(pesanValidasi, BorderLayout.CENTER);
        kontrolIsi.add(labelInfo, BorderLayout.SOUTH);
        kontrol.add(kontrolIsi, BorderLayout.CENTER);

        JPanel daftar = Ui.kartu("Status Batch");
        JPanel daftarIsi = new JPanel(new BorderLayout(0, 4));
        daftarIsi.setOpaque(false);
        gulirTabel = Ui.scrollTabel(tabel, Ui.TINGGI_TABEL_PEMANTAUAN);
        hasil.add(gulirTabel, "tabel");
        hasil.add(kosong, "kosong");
        hasil.add(kosongBarang, "kosongBarang");
        aksiTabel = new JPanel(new java.awt.FlowLayout(
                java.awt.FlowLayout.LEFT, 8, 0));
        aksiTabel.setOpaque(false);
        aksiTabel.setBorder(new javax.swing.border.EmptyBorder(
                Ui.skala(8), 0, 0, 0));
        aksiTabel.add(tombolBuang);
        aksiTabel.add(petunjukBuang);
        aksiTabel.setVisible(false);
        daftarIsi.add(hasil, BorderLayout.CENTER);
        daftarIsi.add(aksiTabel, BorderLayout.SOUTH);
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

    private void pilihStatus() {
        if (memperbaruiFilter) {
            return;
        }
        PilihanStatus dipilih = (PilihanStatus) pilihanStatus.getSelectedItem();
        StatusKedaluwarsa target = dipilih == null ? null : dipilih.status();
        if (target == statusFilter) {
            return;
        }
        statusFilter = target;
        perbarui();
    }

    public void terapkanFilterStatus(StatusKedaluwarsa status) {
        statusFilter = status;
        memperbaruiFilter = true;
        try {
            int indeks = 0;
            if (status != null) {
                for (int i = 1; i < pilihanStatus.getItemCount(); i++) {
                    if (pilihanStatus.getItemAt(i).status() == status) {
                        indeks = i;
                        break;
                    }
                }
            }
            pilihanStatus.setSelectedIndex(indeks);
        } finally {
            memperbaruiFilter = false;
        }
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
            perbaruiAksiBuang();
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
        perbaruiAksiBuang();
    }

    private void tambahBaris(Barang barang, LocalDate acuan) {
        for (BatchStok batch : barang.getDaftarBatch()) {
            if (batch.getSisaJumlah() <= 0) {
                continue;
            }
            StatusKedaluwarsa status = batch.statusPada(acuan);
            if (statusFilter != null && status != statusFilter) {
                continue;
            }
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

    private void perbaruiAksiBuang() {
        boolean adaBaris = model.getRowCount() > 0;
        if (aksiTabel != null) {
            aksiTabel.setVisible(adaBaris);
        }
        BarisPemantauan terpilih = barisTerpilih();
        boolean bisa = adaBaris && terpilih != null && terpilih.bisaDibuang();
        tombolBuang.setEnabled(bisa);
        tombolBuang.setToolTipText(bisa
                ? "Buka form Pembuangan untuk batch " + terpilih.kodeBatch()
                : "Pilih batch bersisa berstatus Kedaluwarsa.");
        petunjukBuang.setVisible(!bisa);
    }

    private void buangTerpilih() {
        BarisPemantauan terpilih = barisTerpilih();
        if (terpilih == null || !terpilih.bisaDibuang()) {
            perbaruiAksiBuang();
            aturPesan(pesanValidasi,
                    "Pilih baris batch bersisa berstatus Kedaluwarsa terlebih dahulu.");
            konteks.tampilkanPesan(
                    "Batch terpilih tidak lagi valid untuk Pembuangan.", false);
            return;
        }
        kosongkanPesan(pesanValidasi);
        konteks.bukaPembuanganDariPemantauan(
                terpilih.idBarang(),
                terpilih.kodeBatch(),
                terpilih.tanggalAcuan());
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
        if (statusFilter != null) {
            kosong.setIcon("circle-check")
                    .setTitle("Tidak ada batch " + statusFilter.getLabel())
                    .setDescription(idFilter < 0
                            ? "Tidak ditemukan batch berstatus "
                                    + statusFilter.getLabel()
                                    + " pada tanggal ini."
                            : "Tidak ditemukan batch berstatus "
                                    + statusFilter.getLabel()
                                    + " untuk barang ini.")
                    .setAction(tombolResetStatus);
            return;
        }
        kosong.setIcon("alert-triangle")
                .setTitle("Tidak ada batch bersisa")
                .setDescription(idFilter < 0
                        ? "Tidak ada batch bersisa untuk seluruh barang."
                        : "Tidak ada batch bersisa untuk barang ini.")
                .setAction(null);
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

    private static final class PilihanStatus {
        private static final PilihanStatus SEMUA = new PilihanStatus(null);
        private final StatusKedaluwarsa status;

        private PilihanStatus(StatusKedaluwarsa status) {
            this.status = status;
        }

        private StatusKedaluwarsa status() {
            return status;
        }

        @Override
        public String toString() {
            return status == null ? "Semua Status" : status.getLabel();
        }
    }
}
