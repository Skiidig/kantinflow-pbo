package kantinflow.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowSorter;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

import kantinflow.Barang;
import kantinflow.RincianTransaksi;
import kantinflow.Transaksi;

/**
 * Halaman riwayat dengan pencarian, filter, dan detail transaksi.
 */
public class PanelRiwayat extends PanelKantin {
    private static final String SEMUA = "Semua";
    private static final String MASUK = "Masuk";
    private static final String KELUAR_FEFO = "Keluar (FEFO)";
    private static final String KELUAR_MANUAL = "Keluar (Manual)";
    private static final String PEMBUANGAN = "Pembuangan";

    private final JComboBox<PilihanBarang> pilihanBarang = Ui.combo();
    private final JTextField fieldCari = Ui.field(16);
    private final PemilihTanggal tanggalMulai = new PemilihTanggal();
    private final PemilihTanggal tanggalAkhir = new PemilihTanggal();
    private final JLabel pesanTanggalMulai = Ui.pesan();
    private final JLabel pesanTanggalAkhir = Ui.pesan();
    private final JComboBox<String> comboJenis = Ui.combo();
    private final JButton tombolTerapkan = Ui.utama("Terapkan Filter");
    private final JButton tombolReset = Ui.sekunder("Reset");
    private final JButton tombolResetKosong = Ui.ghost("Reset Filter");
    private final JButton tombolDetail = Ui.sekunder("Lihat Detail");
    private JPanel aksiTabel;
    private final JLabel petunjukDetail = Ui.keterangan(
            "Pilih baris transaksi untuk melihat detail.");
    private final JButton tombolEkspor = Ui.sekunder("Ekspor CSV");
    private final JButton tombolPdf = Ui.sekunder("Ekspor PDF");
    private final JLabel pesanFilter = Ui.pesan();
    private final JLabel labelJumlah = Ui.keterangan(
            "0 transaksi · 0 rincian ditampilkan");
    private final EmptyState kosong = new EmptyState(
            "history", "Belum ada riwayat",
            "Belum ada transaksi untuk barang aktif ini.");
    private final DefaultTableModel model = Ui.model(
            "Tanggal", "Barang", "Jenis", "Kode Batch",
            "Jumlah", "Satuan", "Alasan");
    private final JTable tabel = Ui.tabel(model);
    private JScrollPane gulirTabel;
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
    private final List<BarisRiwayat> barisModel = new ArrayList<>();
    private Barang barang;
    private int idBarang = -1;
    private boolean memperbaruiPilihan;
    private boolean mengekspor;
    private String cariAktif = "";
    private String jenisAktif = SEMUA;
    private LocalDate dariAktif;
    private LocalDate sampaiAktif;

    public PanelRiwayat(KonteksKantin konteks) {
        super(konteks, "Riwayat",
                "Cari, filter, dan periksa rincian transaksi barang aktif.");
        pilihanBarang.setName("barangRiwayat");
        pilihanBarang.addActionListener(event -> pilihDariPengguna());
        fieldCari.setName("cariRiwayat");
        fieldCari.putClientProperty("JTextField.placeholderText",
                "Cari kode batch atau alasan...");
        tanggalMulai.komponenTeks().setName("tanggalAwalRiwayat");
        tanggalAkhir.komponenTeks().setName("tanggalAkhirRiwayat");
        tanggalMulai.setPertahankanTeksTidakValid(true);
        tanggalAkhir.setPertahankanTeksTidakValid(true);
        pesanTanggalMulai.setName("pesanTanggalAwalRiwayat");
        pesanTanggalAkhir.setName("pesanTanggalAkhirRiwayat");
        comboJenis.setName("jenisRiwayat");
        tombolTerapkan.setName("terapkanFilterRiwayat");
        tombolReset.setName("resetFilterRiwayat");
        tombolDetail.setName("detailRiwayat");
        tombolEkspor.setName("eksporRiwayat");
        tombolEkspor.setIcon(Ui.ikonAksi("export", 16, Ui.TEKS_LEMBUT));
        tombolEkspor.setIconTextGap(8);
        tombolPdf.setName("eksporPdf");
        tombolPdf.setIcon(Ui.ikonAksi("pdf", 16, Ui.TEKS_LEMBUT));
        tombolPdf.setIconTextGap(8);
        tabel.setName("tabelRiwayat");
        labelJumlah.setName("jumlahRiwayat");
        pesanFilter.setName("pesanFilterRiwayat");
        kosongkanPesan(pesanFilter);
        kosongkanPesan(pesanTanggalMulai);
        kosongkanPesan(pesanTanggalAkhir);
        kosong.setName("kosongRiwayat");

        comboJenis.addItem(SEMUA);
        comboJenis.addItem(MASUK);
        comboJenis.addItem(KELUAR_FEFO);
        comboJenis.addItem(KELUAR_MANUAL);
        comboJenis.addItem(PEMBUANGAN);
        comboJenis.setSelectedIndex(0);
        tombolDetail.setEnabled(false);
        tombolDetail.setToolTipText("Pilih transaksi terlebih dahulu.");
        tombolEkspor.setEnabled(false);
        tombolPdf.setEnabled(false);

        tombolTerapkan.addActionListener(event -> terapkanFilter());
        tombolReset.addActionListener(event -> resetFilter());
        tombolResetKosong.addActionListener(event -> resetFilter());
        tombolDetail.addActionListener(event -> tampilkanDetail());
        tombolEkspor.addActionListener(event -> eksporCsv());
        tombolPdf.addActionListener(event -> eksporPdf());
        fieldCari.addActionListener(event -> terapkanFilter());
        tabel.setRowSorter(sorter);
        pasangKomparatorSorter();
        tabel.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                perbaruiDetail();
            }
        });
        tabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2
                        && tabel.getSelectedRow() >= 0) {
                    tampilkanDetail();
                }
            }
        });
        Ui.rataKanan(tabel, 4);
        Ui.pasangBadge(tabel, 2);
        aturKolomRiwayat();
        Ui.jarakKiriKolom(tabel, 6, 12);
        Ui.gulirHorizontalSaatSempit(tabel);
        Ui.headerKlik(tabel);
        bangunIsi();
    }

    private void aturKolomRiwayat() {
        tabel.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        TableColumnModel kolomModel = tabel.getColumnModel();
        int[] preferensi = {118, 145, 155, 190, 78, 84, 240};
        int[] minimum = {110, 120, 146, 175, 72, 75, 155};
        for (int i = 0; i < preferensi.length
                && i < kolomModel.getColumnCount(); i++) {
            TableColumn kolom = kolomModel.getColumn(i);
            kolom.setPreferredWidth(Ui.skala(preferensi[i]));
            kolom.setMinWidth(Ui.skala(minimum[i]));
        }
    }

    private void pasangKomparatorSorter() {
        sorter.setComparator(0, Comparator.nullsLast(Comparator.naturalOrder()));
        sorter.setComparator(4, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    @Override
    public javax.swing.JComponent getAksiHeader() {
        JPanel kanan = new JPanel(new java.awt.FlowLayout(
                java.awt.FlowLayout.RIGHT, Ui.skala(8), 0));
        kanan.setOpaque(false);
        kanan.add(tombolEkspor);
        kanan.add(tombolPdf);
        return kanan;
    }

    private void bangunIsi() {
        JPanel isi = new JPanel(new BorderLayout(0, Ui.skala(Ui.GAP_KARTU)));
        isi.setOpaque(false);

        JPanel filter = Ui.kartu("Filter Riwayat");
        JPanel[] kolom = {
            Ui.field("Barang", pilihanBarang),
            Ui.field("Cari kode batch atau alasan", fieldCari),
            Ui.field("Jenis transaksi", comboJenis),
            Ui.field("Tanggal mulai", tanggalMulai, pesanTanggalMulai),
            Ui.field("Tanggal akhir", tanggalAkhir, pesanTanggalAkhir)
        };
        JPanel isian = new JPanel();
        isian.setOpaque(false);
        aturGridFilter(isian, kolom, false);
        isian.addComponentListener(new ComponentAdapter() {
            private boolean sempit;

            @Override
            public void componentResized(ComponentEvent event) {
                boolean sekarang = isian.getWidth() > 0
                        && isian.getWidth() < Ui.skala(520);
                if (sekarang == sempit) {
                    return;
                }
                sempit = sekarang;
                aturGridFilter(isian, kolom, sempit);
                isian.revalidate();
                isian.repaint();
            }
        });

        JPanel aksi = new JPanel(new java.awt.FlowLayout(
                java.awt.FlowLayout.LEFT, 8, 0));
        aksi.setOpaque(false);
        aksi.setBorder(new EmptyBorder(Ui.skala(16), 0, 0, 0));
        aksi.add(tombolTerapkan);
        aksi.add(tombolReset);

        JPanel filterIsi = new JPanel();
        filterIsi.setOpaque(false);
        filterIsi.setLayout(new javax.swing.BoxLayout(
                filterIsi, javax.swing.BoxLayout.Y_AXIS));
        isian.setAlignmentX(LEFT_ALIGNMENT);
        filterIsi.add(isian);
        pesanFilter.setAlignmentX(LEFT_ALIGNMENT);
        pesanFilter.setBorder(new EmptyBorder(Ui.skala(6), 0, 0, 0));
        filterIsi.add(pesanFilter);
        aksi.setAlignmentX(LEFT_ALIGNMENT);
        filterIsi.add(aksi);
        filter.add(filterIsi, BorderLayout.CENTER);

        JPanel daftar = Ui.kartu();
        JPanel kepala = new JPanel(new BorderLayout(8, 0));
        kepala.setOpaque(false);
        JLabel judulDaftar = new JLabel("Daftar Transaksi");
        judulDaftar.setFont(Ui.fontSemiBold(Ui.FONT_KARTU));
        judulDaftar.setForeground(Ui.TEKS);
        kepala.add(judulDaftar, BorderLayout.WEST);
        kepala.add(labelJumlah, BorderLayout.EAST);
        aksiTabel = new JPanel(new java.awt.FlowLayout(
                java.awt.FlowLayout.LEFT, 8, 0));
        aksiTabel.setOpaque(false);
        aksiTabel.setBorder(new EmptyBorder(Ui.skala(8), 0, 0, 0));
        aksiTabel.add(tombolDetail);
        aksiTabel.add(petunjukDetail);
        JPanel bawahTabel = new JPanel(new BorderLayout(0, 4));
        bawahTabel.setOpaque(false);
        bawahTabel.add(aksiTabel, BorderLayout.NORTH);
        bawahTabel.add(kosong, BorderLayout.CENTER);
        JPanel daftarIsi = new JPanel(new BorderLayout(0, Ui.skala(10)));
        daftarIsi.setOpaque(false);
        daftarIsi.add(kepala, BorderLayout.NORTH);
        gulirTabel = Ui.scrollTabel(tabel, Ui.TINGGI_TABEL_RIWAYAT);
        daftarIsi.add(gulirTabel, BorderLayout.CENTER);
        daftarIsi.add(bawahTabel, BorderLayout.SOUTH);
        daftar.add(daftarIsi, BorderLayout.CENTER);

        isi.add(filter, BorderLayout.NORTH);
        isi.add(daftar, BorderLayout.CENTER);
        add(isi, BorderLayout.CENTER);
    }

    private void aturGridFilter(JPanel panel, JPanel[] kolom,
            boolean sempit) {
        panel.removeAll();
        panel.setLayout(new GridBagLayout());
        int jarak = Ui.skala(Ui.GAP_FORM);
        if (sempit) {
            for (int i = 0; i < kolom.length; i++) {
                GridBagConstraints c = new GridBagConstraints();
                c.gridx = 0;
                c.gridy = i;
                c.weightx = 1;
                c.fill = GridBagConstraints.HORIZONTAL;
                c.anchor = GridBagConstraints.NORTH;
                c.insets = new Insets(i == 0 ? 0 : jarak, 0, 0, 0);
                panel.add(kolom[i], c);
            }
            return;
        }
        GridBagConstraints awal = new GridBagConstraints();
        awal.gridx = 0;
        awal.gridy = 0;
        awal.gridwidth = 2;
        awal.weightx = 1;
        awal.fill = GridBagConstraints.HORIZONTAL;
        awal.anchor = GridBagConstraints.NORTH;
        panel.add(kolom[0], awal);
        for (int i = 1; i < kolom.length; i += 2) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 1 + (i - 1) / 2;
            c.gridwidth = 2;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.NORTH;
            c.insets = new Insets(jarak, 0, 0, 0);
            panel.add(i + 1 < kolom.length
                    ? pasangan(kolom[i], kolom[i + 1], jarak) : kolom[i], c);
        }
    }

    private JPanel pasangan(JPanel kiri, JPanel kanan, int jarak) {
        JPanel panel = new JPanel(new GridLayout(1, 2, jarak, 0));
        panel.setOpaque(false);
        panel.add(kiri);
        panel.add(kanan);
        return panel;
    }

    @Override
    protected void aturPesan(JLabel label, String teks) {
        aturPesanKompak(label, teks);
    }

    private void terapkanFilter() {
        kosongkanPesan(pesanFilter);
        kosongkanPesan(pesanTanggalMulai);
        kosongkanPesan(pesanTanggalAkhir);
        List<? extends RowSorter.SortKey> kunciUrut = sorter.getSortKeys();
        LocalDate dari = bacaTanggal(
                tanggalMulai, pesanTanggalMulai, "Tanggal mulai");
        if (dari == INVALID) {
            return;
        }
        LocalDate sampai = bacaTanggal(
                tanggalAkhir, pesanTanggalAkhir, "Tanggal akhir");
        if (sampai == INVALID) {
            return;
        }
        if (dari != null && sampai != null && dari.isAfter(sampai)) {
            aturPesan(pesanTanggalAkhir,
                    "Tanggal mulai tidak boleh setelah tanggal akhir.");
            tanggalAkhir.komponenTeks().requestFocusInWindow();
            return;
        }
        bersihkanSeleksiDetail();

        cariAktif = fieldCari.getText() == null
                ? "" : fieldCari.getText().trim();
        jenisAktif = (String) comboJenis.getSelectedItem();
        dariAktif = dari;
        sampaiAktif = sampai;
        String cari = cariAktif.toLowerCase(Locale.ROOT);
        String jenis = jenisAktif;
        List<BarisRiwayat> hasil = new ArrayList<>();
        Set<Transaksi> transaksiUnik = new LinkedHashSet<>();
        boolean adaRiwayat = false;
        for (Barang item : lingkupBarang()) {
            List<Transaksi> riwayat = konteks.getRiwayat(item);
            if (!riwayat.isEmpty()) {
                adaRiwayat = true;
            }
            for (Transaksi transaksi : riwayat) {
                if (!cocokJenis(transaksi, jenis)
                        || !cocokTanggal(transaksi, dari, sampai)) {
                    continue;
                }
                String alasan = transaksi.getAlasan() == null
                        ? "" : transaksi.getAlasan();
                for (RincianTransaksi rincian : transaksi.getRincian()) {
                    String batch = rincian.getBatch().getKode();
                    if (!cari.isEmpty()
                            && !batch.toLowerCase(Locale.ROOT).contains(cari)
                            && !alasan.toLowerCase(Locale.ROOT).contains(cari)) {
                        continue;
                    }
                    hasil.add(new BarisRiwayat(item, transaksi, rincian));
                    transaksiUnik.add(transaksi);
                }
            }
        }

        tabel.setRowSorter(null);
        model.setRowCount(0);
        barisModel.clear();
        for (BarisRiwayat baris : hasil) {
            Transaksi transaksi = baris.transaksi();
            RincianTransaksi rincian = baris.rincian();
            Barang asal = baris.barang();
            model.addRow(new Object[]{
                transaksi.getTanggal(),
                asal.getNama(),
                jenisTampil(transaksi),
                rincian.getBatch().getKode(),
                rincian.getJumlah(),
                asal.getSatuan(),
                transaksi.getAlasan() == null ? "" : transaksi.getAlasan()
            });
            barisModel.add(baris);
        }
        tabel.setRowSorter(sorter);
        sorter.modelStructureChanged();
        pasangKomparatorSorter();
        if (!kunciUrut.isEmpty()) {
            sorter.setSortKeys(kunciUrut);
        }
        labelJumlah.setText(transaksiUnik.size() + " transaksi · "
                + hasil.size() + " rincian ditampilkan");
        if (hasil.isEmpty()) {
            if (konteks.getDaftarBarang().isEmpty()) {
                kosong.setIcon("boxes")
                        .setTitle("Belum ada barang")
                        .setDescription(
                                "Tambahkan barang melalui halaman Barang untuk "
                                + "melihat riwayatnya.")
                        .setAction(null);
            } else if (barang == null) {
                kosong.setIcon(adaRiwayat ? "search" : "history")
                        .setTitle(adaRiwayat
                                ? "Tidak ada hasil filter" : "Belum ada riwayat")
                        .setDescription(adaRiwayat
                                ? "Tidak ada riwayat yang cocok dengan filter."
                                : "Belum ada transaksi untuk seluruh barang.")
                        .setAction(adaRiwayat ? tombolResetKosong : null);
            } else {
                kosong.setIcon(adaRiwayat ? "search" : "history")
                        .setTitle(adaRiwayat
                                ? "Tidak ada hasil filter" : "Belum ada riwayat")
                        .setDescription(adaRiwayat
                                ? "Tidak ada riwayat yang cocok dengan filter."
                                : "Belum ada transaksi untuk "
                                + barang.getNama() + ".")
                        .setAction(adaRiwayat ? tombolResetKosong : null);
            }
        }
        boolean kosongTabel = hasil.isEmpty();
        kosong.setVisible(kosongTabel);
        if (aksiTabel != null) {
            aksiTabel.setVisible(!kosongTabel);
        }
        if (gulirTabel != null) {
            gulirTabel.setVisible(!kosongTabel);
        }
        Ui.sesuaikanTinggiTabel(gulirTabel, tabel, 10);
        perbaruiTombolAksi();
    }

    private void perbaruiTombolAksi() {
        boolean adaBaris = !barisModel.isEmpty();
        tombolEkspor.setEnabled(adaBaris && !mengekspor);
        tombolPdf.setEnabled(adaBaris && !mengekspor);
        perbaruiDetail();
    }

    private void perbaruiDetail() {
        boolean adaBaris = tabel.getSelectedRow() >= 0;
        tombolDetail.setEnabled(adaBaris);
        petunjukDetail.setVisible(!adaBaris);
    }

    private void bersihkanSeleksiDetail() {
        tabel.clearSelection();
        perbaruiDetail();
    }

    private void eksporCsv() {
        if (mengekspor) {
            return;
        }
        SnapshotTampilan snapshot = snapshotTampilan();
        if (snapshot.baris().isEmpty()) {
            perbaruiTombolAksi();
            return;
        }
        Path tujuan = pilihTujuanEkspor("Ekspor Riwayat ke CSV", "csv",
                "CSV (*.csv)", namaAwalEkspor("csv"));
        if (tujuan == null) {
            return;
        }
        mengekspor = true;
        perbaruiTombolAksi();
        konteks.tampilkanPesan("Menyimpan " + snapshot.baris().size()
                + " rincian riwayat ke " + tujuan.getFileName() + "...", true);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                RiwayatCsvExporter.tulis(snapshot.baris(), tujuan);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    konteks.tampilkanPesan("Ekspor CSV berhasil: "
                            + snapshot.baris().size() + " rincian ke "
                            + tujuan + ".", true);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    tampilkanGagalEkspor("CSV", exception);
                } catch (CancellationException exception) {
                    tampilkanGagalEkspor("CSV", exception);
                } catch (ExecutionException exception) {
                    tampilkanGagalEkspor("CSV", exception.getCause());
                } finally {
                    mengekspor = false;
                    perbaruiTombolAksi();
                }
            }
        }.execute();
    }

    private void eksporPdf() {
        if (mengekspor) {
            return;
        }
        SnapshotTampilan snapshot = snapshotTampilan();
        if (snapshot.baris().isEmpty()) {
            perbaruiTombolAksi();
            return;
        }
        Path tujuan = pilihTujuanEkspor("Ekspor Riwayat ke PDF", "pdf",
                "PDF (*.pdf)", namaAwalEkspor("pdf"));
        if (tujuan == null) {
            return;
        }
        RiwayatPdfExporter.Info info = new RiwayatPdfExporter.Info(
                lingkupNama(),
                barang == null ? "Berbagai satuan" : barang.getSatuan(),
                LocalDateTime.now(),
                snapshot.cari(),
                snapshot.jenis(),
                snapshot.mulai(),
                snapshot.akhir(),
                snapshot.jumlahTransaksi(),
                snapshot.baris());
        mengekspor = true;
        perbaruiTombolAksi();
        konteks.tampilkanPesan("Menyimpan " + snapshot.baris().size()
                + " rincian riwayat ke " + tujuan.getFileName() + "...", true);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                RiwayatPdfExporter.tulis(info, tujuan);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    konteks.tampilkanPesan("Ekspor PDF berhasil: "
                            + snapshot.baris().size() + " rincian ke "
                            + tujuan + ".", true);
                    tawarkanBukaPdf(tujuan, snapshot.baris().size());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    tampilkanGagalEkspor("PDF", exception);
                } catch (CancellationException exception) {
                    tampilkanGagalEkspor("PDF", exception);
                } catch (ExecutionException exception) {
                    tampilkanGagalEkspor("PDF", exception.getCause());
                } finally {
                    mengekspor = false;
                    perbaruiTombolAksi();
                }
            }
        }.execute();
    }

    private Path pilihTujuanEkspor(String judul, String ekstensi,
            String deskripsiFilter, String namaAwal) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(judul);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(new FileNameExtensionFilter(
                deskripsiFilter, ekstensi));
        chooser.setSelectedFile(new File(namaAwal));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        Path tujuan = pathEkstensi(
                chooser.getSelectedFile().toPath(), ekstensi);
        if (Files.isDirectory(tujuan)) {
            aturPesan(pesanFilter, "Pilih nama file "
                    + ekstensi.toUpperCase(Locale.ROOT)
                    + ", bukan folder tujuan.");
            return null;
        }
        if (Files.exists(tujuan)) {
            int jawab = JOptionPane.showConfirmDialog(
                    this,
                    "File berikut sudah ada. Timpa file tersebut?\n" + tujuan,
                    "Konfirmasi timpa file",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (jawab != JOptionPane.YES_OPTION) {
                return null;
            }
        }
        return tujuan;
    }

    private SnapshotTampilan snapshotTampilan() {
        List<RiwayatCsvExporter.Baris> hasil = new ArrayList<>();
        Set<Transaksi> unik = new LinkedHashSet<>();
        for (int tampilan = 0; tampilan < tabel.getRowCount(); tampilan++) {
            int modelIndex = tabel.convertRowIndexToModel(tampilan);
            if (modelIndex < 0 || modelIndex >= barisModel.size()) {
                continue;
            }
            BarisRiwayat baris = barisModel.get(modelIndex);
            Barang asal = baris.barang();
            Transaksi transaksi = baris.transaksi();
            RincianTransaksi rincian = baris.rincian();
            unik.add(transaksi);
            hasil.add(new RiwayatCsvExporter.Baris(
                    asal.getNama(),
                    asal.getSatuan(),
                    transaksi.getTanggal(),
                    jenisTampil(transaksi),
                    rincian.getBatch().getKode(),
                    rincian.getJumlah(),
                    transaksi.getAlasan() == null ? "" : transaksi.getAlasan()));
        }
        return new SnapshotTampilan(List.copyOf(hasil), unik.size(),
                cariAktif, jenisAktif, dariAktif, sampaiAktif);
    }

    private String namaAwalEkspor(String ekstensi) {
        String nama = sanitasiNamaFile(lingkupNama());
        return "kantinflow-riwayat-" + nama + "-"
                + LocalDate.now() + "." + ekstensi;
    }

    private static String sanitasiNamaFile(String nama) {
        String hasil = nama == null ? "" : nama
                .replaceAll("[\\/:*?\"<>|\\p{Cntrl}]", "-")
                .replaceAll("[. ]+$", "")
                .trim();
        if (hasil.isEmpty()) {
            hasil = "barang";
        }
        if (hasil.length() > 80) {
            hasil = hasil.substring(0, 80).replaceAll("[. ]+$", "");
        }
        return hasil.isEmpty() ? "barang" : hasil;
    }

    private Path pathEkstensi(Path target, String ekstensi) {
        String nama = target.getFileName().toString();
        if (!nama.toLowerCase(Locale.ROOT).endsWith("." + ekstensi)) {
            return target.resolveSibling(nama + "." + ekstensi);
        }
        return target;
    }

    private void tawarkanBukaPdf(Path file, int jumlah) {
        Object[] opsi = {"Buka PDF", "Tutup"};
        int pilihan = JOptionPane.showOptionDialog(
                this,
                "PDF berhasil dibuat: " + jumlah + " rincian.\n" + file
                + "\n\nBuka PDF dengan aplikasi default?",
                "Ekspor PDF",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                opsi,
                opsi[1]);
        if (pilihan == 0) {
            bukaPdf(file);
        }
    }

    private void bukaPdf(Path file) {
        if (!Desktop.isDesktopSupported()
                || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            JOptionPane.showMessageDialog(
                    this,
                    "PDF tersimpan di:\n" + file
                    + "\n\nAplikasi pembaca PDF tidak tersedia. "
                    + "Buka file tersebut secara manual.",
                    "Ekspor PDF",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            Desktop.getDesktop().open(file.toFile());
        } catch (IOException | SecurityException exception) {
            JOptionPane.showMessageDialog(
                    this,
                    "PDF tersimpan di:\n" + file
                    + "\n\nPDF tidak dapat dibuka otomatis: "
                    + exception.getMessage(),
                    "Ekspor PDF",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private record SnapshotTampilan(
            List<RiwayatCsvExporter.Baris> baris,
            int jumlahTransaksi,
            String cari,
            String jenis,
            LocalDate mulai,
            LocalDate akhir) {
    }

    private void tampilkanGagalEkspor(String format, Throwable penyebab) {
        String detail = penyebab == null || penyebab.getMessage() == null
                || penyebab.getMessage().isBlank()
                        ? "penulisan file gagal" : penyebab.getMessage();
        konteks.tampilkanPesan("Ekspor " + format + " gagal: " + detail
                + ". File tujuan lama tidak diubah.", false);
    }

    private void resetFilter() {
        fieldCari.setText("");
        tanggalMulai.setTanggal(null);
        tanggalAkhir.setTanggal(null);
        comboJenis.setSelectedIndex(0);
        kosongkanPesan(pesanFilter);
        terapkanFilter();
    }

    private LocalDate bacaTanggal(PemilihTanggal pemilih, JLabel pesan,
            String nama) {
        if (pemilih.ambilTeksTidakValid() || !pemilih.isTextFieldValid()) {
            aturPesan(pesan,
                    nama + " tidak valid. Pilih tanggal yang benar.");
            pemilih.komponenTeks().requestFocusInWindow();
            return INVALID;
        }
        return pemilih.getTanggal();
    }

    private static final LocalDate INVALID = LocalDate.of(1, 1, 1);

    private boolean cocokJenis(Transaksi transaksi, String filter) {
        return SEMUA.equals(filter) || jenisTampil(transaksi).equals(filter);
    }

    private boolean cocokTanggal(Transaksi transaksi, LocalDate dari,
            LocalDate sampai) {
        LocalDate tanggal = transaksi.getTanggal();
        return (dari == null || !tanggal.isBefore(dari))
                && (sampai == null || !tanggal.isAfter(sampai));
    }

    private String jenisTampil(Transaksi transaksi) {
        String jenis = transaksi.getJenis();
        if ("Penerimaan".equals(jenis)) {
            return MASUK;
        }
        if ("Pengeluaran FEFO".equals(jenis)) {
            return KELUAR_FEFO;
        }
        if ("Pengeluaran".equals(jenis)) {
            return KELUAR_MANUAL;
        }
        if ("Pembuangan".equals(jenis)) {
            return PEMBUANGAN;
        }
        return jenis;
    }

    private void tampilkanDetail() {
        int barisTampilan = tabel.getSelectedRow();
        if (barisTampilan < 0) {
            aturPesan(pesanFilter,
                    "Pilih salah satu baris riwayat terlebih dahulu.");
            return;
        }
        int barisData = tabel.convertRowIndexToModel(barisTampilan);
        if (barisData < 0 || barisData >= barisModel.size()) {
            return;
        }
        BarisRiwayat baris = barisModel.get(barisData);
        Transaksi transaksi = baris.transaksi();
        Barang asal = baris.barang();
        JDialog dialog = new JDialog(
                SwingUtilities.getWindowAncestor(this), "Detail Transaksi",
                Dialog.ModalityType.APPLICATION_MODAL);
        Ui.pasangTitleBar(dialog.getRootPane());
        JPanel konten = new JPanel(new BorderLayout(0, 14));
        konten.setOpaque(true);
        konten.setBackground(Ui.LATAR);
        konten.setBorder(new EmptyBorder(18, 18, 18, 18));

        JPanel info = Ui.kartu("Informasi transaksi");
        JPanel nilai = new JPanel(new GridLayout(6, 2, 10, 6));
        nilai.setOpaque(false);
        nilai.add(Ui.labelField("Jenis"));
        nilai.add(new JLabel(jenisTampil(transaksi)));
        nilai.add(Ui.labelField("Tanggal"));
        nilai.add(new JLabel(FormatTanggal.format(transaksi.getTanggal())));
        nilai.add(Ui.labelField("Barang"));
        nilai.add(new JLabel(asal.getNama() + " (" + asal.getSatuan() + ")"));
        nilai.add(Ui.labelField("Jumlah transaksi"));
        nilai.add(new JLabel(transaksi.getJumlah() + " " + asal.getSatuan()));
        nilai.add(Ui.labelField("Rincian"));
        nilai.add(new JLabel(transaksi.getRincian().size() + " batch"));
        nilai.add(Ui.labelField("Alasan"));
        nilai.add(new JLabel(transaksi.getAlasan() == null
                || transaksi.getAlasan().isBlank()
                        ? "Tidak ada" : transaksi.getAlasan()));
        info.add(nilai, BorderLayout.CENTER);

        JPanel rincian = Ui.kartu("Rincian per batch");
        DefaultTableModel modelDetail = Ui.model(
                "Kode Batch", "Jumlah", "Satuan");
        JTable tabelDetail = Ui.tabel(modelDetail);
        tabelDetail.setName("tabelDetailRiwayat");
        Ui.rataKanan(tabelDetail, 1);
        Ui.aturKolom(tabelDetail, 160, 90, 90);
        for (RincianTransaksi item : transaksi.getRincian()) {
            modelDetail.addRow(new Object[]{
                item.getBatch().getKode(),
                item.getJumlah(),
                asal.getSatuan()
            });
        }
        rincian.add(Ui.scrollTabel(tabelDetail, 150), BorderLayout.CENTER);

        JPanel bawah = new JPanel(new java.awt.FlowLayout(
                java.awt.FlowLayout.RIGHT, 8, 0));
        bawah.setOpaque(false);
        JButton tutup = Ui.sekunder("Tutup");
        tutup.addActionListener(event -> dialog.dispose());
        bawah.add(tutup);
        konten.add(info, BorderLayout.NORTH);
        konten.add(rincian, BorderLayout.CENTER);
        konten.add(bawah, BorderLayout.SOUTH);

        dialog.setName("dialogDetailRiwayat");
        dialog.setContentPane(konten);
        dialog.getContentPane().setBackground(Ui.LATAR);
        dialog.pack();
        dialog.setMinimumSize(new java.awt.Dimension(480, 360));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void pilihDariPengguna() {
        if (memperbaruiPilihan) {
            return;
        }
        PilihanBarang dipilih = (PilihanBarang) pilihanBarang.getSelectedItem();
        if (dipilih == null || dipilih.idBarang() == idBarang) {
            return;
        }
        barang = dipilih.barang();
        idBarang = dipilih.idBarang();
        saatBarangBerubah();
        perbarui();
    }

    private boolean perbaruiPilihanBarang() {
        List<Barang> daftar = konteks.getDaftarBarang();
        memperbaruiPilihan = true;
        try {
            pilihanBarang.removeAllItems();
            pilihanBarang.addItem(PilihanBarang.SEMUA);
            for (Barang item : daftar) {
                pilihanBarang.addItem(new PilihanBarang(item));
            }
            if (daftar.isEmpty()) {
                barang = null;
                idBarang = -1;
                pilihanBarang.setSelectedIndex(0);
                pilihanBarang.setEnabled(false);
                pilihanBarang.setToolTipText(
                        "Belum ada barang. Tambahkan melalui halaman Barang.");
                return false;
            }
            pilihanBarang.setEnabled(true);
            int indeks = 0;
            if (idBarang >= 0) {
                for (int i = 1; i < pilihanBarang.getItemCount(); i++) {
                    if (pilihanBarang.getItemAt(i).idBarang() == idBarang) {
                        indeks = i;
                        break;
                    }
                }
            }
            if (indeks == 0) {
                idBarang = -1;
            }
            pilihanBarang.setSelectedIndex(indeks);
            PilihanBarang dipilih = pilihanBarang.getItemAt(indeks);
            barang = dipilih == null ? null : dipilih.barang();
            idBarang = dipilih == null ? -1 : dipilih.idBarang();
            pilihanBarang.setToolTipText(null);
            return true;
        } finally {
            memperbaruiPilihan = false;
        }
    }

    private List<Barang> lingkupBarang() {
        return barang == null ? konteks.getDaftarBarang() : List.of(barang);
    }

    private String lingkupNama() {
        return barang == null ? "Semua Barang" : barang.getNama();
    }

    private void saatBarangBerubah() {
        kosongkanPesan(pesanFilter);
        bersihkanSeleksiDetail();
    }

    @Override
    public void perbarui() {
        perbaruiPilihanBarang();
        terapkanFilter();
    }

    private record BarisRiwayat(
            Barang barang, Transaksi transaksi, RincianTransaksi rincian) {
    }

    private static final class PilihanBarang {
        private static final PilihanBarang SEMUA = new PilihanBarang(null);
        private final Barang barang;

        private PilihanBarang(Barang barang) {
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
