package kantinflow.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dialog;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowSorter;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import kantinflow.RincianTransaksi;
import kantinflow.Transaksi;

/**
 * Halaman riwayat dengan pencarian, filter, dan detail transaksi.
 */
public class PanelRiwayat extends PanelBarangTerpilih {
    private static final String SEMUA = "Semua";
    private static final String MANUAL = "Pengeluaran Manual";
    private static final String FEFO = "Pengeluaran FEFO";

    private final JTextField fieldCari = Ui.field(16);
    private final PemilihTanggal tanggalMulai = new PemilihTanggal();
    private final PemilihTanggal tanggalAkhir = new PemilihTanggal();
    private final JLabel pesanTanggalMulai = Ui.pesan();
    private final JLabel pesanTanggalAkhir = Ui.pesan();
    private final JComboBox<String> comboJenis = Ui.combo();
    private final JButton tombolTerapkan = Ui.utama("Terapkan Filter");
    private final JButton tombolReset = Ui.sekunder("Reset");
    private final JButton tombolDetail = Ui.sekunder("Lihat Detail");
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
    private final JLabel infoBarang = Ui.keterangan(" ");
    private final DefaultTableModel model = Ui.model(
            "Tanggal", "Jenis", "Kode Batch", "Jumlah", "Alasan");
    private final JTable tabel = Ui.tabel(model);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
    private final List<BarisRiwayat> barisModel = new ArrayList<>();
    private boolean mengekspor;
    private String cariAktif = "";
    private String jenisAktif = SEMUA;
    private LocalDate dariAktif;
    private LocalDate sampaiAktif;

    public PanelRiwayat(KonteksKantin konteks) {
        super(konteks, "Riwayat",
                "Cari, filter, dan periksa rincian transaksi barang aktif.");
        pilihanBarang.setName("barangRiwayat");
        fieldCari.setName("cariRiwayat");
        infoBarang.setName("konteksRiwayat");
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
        kosong.setName("kosongRiwayat");

        comboJenis.addItem(SEMUA);
        comboJenis.addItem("Penerimaan");
        comboJenis.addItem(MANUAL);
        comboJenis.addItem(FEFO);
        comboJenis.addItem("Pembuangan");
        comboJenis.setSelectedIndex(0);
        tombolDetail.setEnabled(false);
        tombolDetail.setToolTipText("Pilih transaksi terlebih dahulu.");
        tombolEkspor.setEnabled(false);
        tombolPdf.setEnabled(false);

        tombolTerapkan.addActionListener(event -> terapkanFilter());
        tombolReset.addActionListener(event -> resetFilter());
        tombolDetail.addActionListener(event -> tampilkanDetail());
        tombolEkspor.addActionListener(event -> eksporCsv());
        tombolPdf.addActionListener(event -> eksporPdf());
        fieldCari.addActionListener(event -> terapkanFilter());
        tabel.setRowSorter(sorter);
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
        Ui.rataKanan(tabel, 3);
        Ui.pasangTeksStatus(tabel, 1);
        Ui.aturKolom(tabel, 100, 140, 95, 95, 210);
        Ui.jarakKiriKolom(tabel, 4, 20);
        bangunIsi();
    }

    private void bangunIsi() {
        JPanel isi = new JPanel(new BorderLayout(0, 12));
        isi.setOpaque(false);

        JPanel filter = Ui.kartu("Filter riwayat", "search");
        JPanel aksi = new JPanel(new java.awt.FlowLayout(
                java.awt.FlowLayout.LEFT, 8, 0));
        aksi.setOpaque(false);
        aksi.add(tombolTerapkan);
        aksi.add(tombolReset);
        JPanel isian = Ui.gridForm(2, 520,
                fieldBarang("Barang *"),
                Ui.field("Cari kode batch atau alasan", fieldCari),
                Ui.field("Jenis transaksi", comboJenis),
                Ui.field("Tanggal mulai", tanggalMulai, pesanTanggalMulai),
                Ui.field("Tanggal akhir", tanggalAkhir, pesanTanggalAkhir),
                Ui.field("Aksi", aksi));
        JPanel filterIsi = new JPanel(new BorderLayout(0, 6));
        filterIsi.setOpaque(false);
        JPanel isianDenganKonteks = new JPanel(new BorderLayout(0, 6));
        isianDenganKonteks.setOpaque(false);
        isianDenganKonteks.add(infoBarang, BorderLayout.NORTH);
        isianDenganKonteks.add(isian, BorderLayout.CENTER);
        filterIsi.add(isianDenganKonteks, BorderLayout.NORTH);
        filterIsi.add(pesanFilter, BorderLayout.SOUTH);
        filter.add(filterIsi, BorderLayout.CENTER);

        JPanel daftar = Ui.kartu();
        JPanel kepala = new JPanel(new BorderLayout(8, 0));
        kepala.setOpaque(false);
        kepala.add(labelJumlah, BorderLayout.WEST);
        JPanel kanan = new JPanel(new java.awt.FlowLayout(
                java.awt.FlowLayout.RIGHT, 8, 0));
        kanan.setOpaque(false);
        kanan.add(tombolEkspor);
        kanan.add(tombolPdf);
        kepala.add(kanan, BorderLayout.EAST);
        JPanel aksiTabel = new JPanel(new java.awt.FlowLayout(
                java.awt.FlowLayout.LEFT, 8, 0));
        aksiTabel.setOpaque(false);
        aksiTabel.add(tombolDetail);
        aksiTabel.add(petunjukDetail);
        JPanel bawahTabel = new JPanel(new BorderLayout(0, 4));
        bawahTabel.setOpaque(false);
        bawahTabel.add(aksiTabel, BorderLayout.NORTH);
        bawahTabel.add(kosong, BorderLayout.CENTER);
        JPanel daftarIsi = new JPanel(new BorderLayout(0, 6));
        daftarIsi.setOpaque(false);
        daftarIsi.add(kepala, BorderLayout.NORTH);
        daftarIsi.add(Ui.scrollTabel(tabel, Ui.TINGGI_TABEL_RIWAYAT),
                BorderLayout.CENTER);
        daftarIsi.add(bawahTabel, BorderLayout.SOUTH);
        daftar.add(daftarIsi, BorderLayout.CENTER);

        isi.add(filter, BorderLayout.NORTH);
        isi.add(daftar, BorderLayout.CENTER);
        add(isi, BorderLayout.CENTER);
    }

    private void terapkanFilter() {
        pesanFilter.setText(" ");
        pesanTanggalMulai.setText(" ");
        pesanTanggalAkhir.setText(" ");
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
            pesanTanggalAkhir.setText(
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
        List<Transaksi> riwayat = barang == null
                ? List.of() : konteks.getRiwayat(barang);

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
                hasil.add(new BarisRiwayat(transaksi, rincian));
                transaksiUnik.add(transaksi);
            }
        }

        tabel.setRowSorter(null);
        model.setRowCount(0);
        barisModel.clear();
        for (BarisRiwayat baris : hasil) {
            Transaksi transaksi = baris.transaksi();
            RincianTransaksi rincian = baris.rincian();
            model.addRow(new Object[]{
                transaksi.getTanggal().toString(),
                jenisTampil(transaksi),
                rincian.getBatch().getKode(),
                rincian.getJumlah() + " "
                + (barang == null ? "-" : barang.getSatuan()),
                transaksi.getAlasan() == null ? "" : transaksi.getAlasan()
            });
            barisModel.add(baris);
        }
        tabel.setRowSorter(sorter);
        sorter.modelStructureChanged();
        if (!kunciUrut.isEmpty()) {
            sorter.setSortKeys(kunciUrut);
        }
        labelJumlah.setText(transaksiUnik.size() + " transaksi · "
                + hasil.size() + " rincian ditampilkan");
        boolean adaRiwayat = !riwayat.isEmpty();
        if (hasil.isEmpty()) {
            if (barang == null) {
                kosong.setTitle("Belum ada barang");
                kosong.setDescription(
                        "Tambahkan barang melalui halaman Barang untuk "
                        + "melihat riwayatnya.");
            } else {
                kosong.setTitle(adaRiwayat
                        ? "Tidak ada hasil filter" : "Belum ada riwayat");
                kosong.setDescription(adaRiwayat
                        ? "Tidak ada riwayat yang cocok dengan filter."
                        : "Belum ada transaksi untuk "
                        + barang.getNama() + ".");
            }
        }
        kosong.setVisible(hasil.isEmpty());
        perbaruiTombolAksi();
    }

    private void perbaruiTombolAksi() {
        boolean adaBarang = barang != null;
        tombolEkspor.setEnabled(adaBarang && !mengekspor
                && !barisModel.isEmpty());
        tombolPdf.setEnabled(adaBarang && !mengekspor
                && !barisModel.isEmpty());
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
        if (barang == null) {
            perbaruiTombolAksi();
            return;
        }
        RiwayatPdfExporter.Info info = new RiwayatPdfExporter.Info(
                barang.getNama(),
                barang.getSatuan(),
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
            pesanFilter.setText("Pilih nama file "
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
        if (barang == null) {
            return new SnapshotTampilan(List.of(), 0,
                    cariAktif, jenisAktif, dariAktif, sampaiAktif);
        }
        String namaBarang = barang.getNama();
        String satuan = barang.getSatuan();
        List<RiwayatCsvExporter.Baris> hasil = new ArrayList<>();
        Set<Transaksi> unik = new LinkedHashSet<>();
        for (int tampilan = 0; tampilan < tabel.getRowCount(); tampilan++) {
            int modelIndex = tabel.convertRowIndexToModel(tampilan);
            if (modelIndex < 0 || modelIndex >= barisModel.size()) {
                continue;
            }
            BarisRiwayat baris = barisModel.get(modelIndex);
            Transaksi transaksi = baris.transaksi();
            RincianTransaksi rincian = baris.rincian();
            unik.add(transaksi);
            hasil.add(new RiwayatCsvExporter.Baris(
                    namaBarang,
                    satuan,
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
        String nama = sanitasiNamaFile(barang == null ? null : barang.getNama());
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
        pesanFilter.setText(" ");
        terapkanFilter();
    }

    private LocalDate bacaTanggal(PemilihTanggal pemilih, JLabel pesan,
            String nama) {
        if (pemilih.ambilTeksTidakValid() || !pemilih.isTextFieldValid()) {
            pesan.setText(nama + " tidak valid. Pilih tanggal yang benar.");
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
        return "Pengeluaran".equals(transaksi.getJenis())
                ? MANUAL : transaksi.getJenis();
    }

    private void tampilkanDetail() {
        int barisTampilan = tabel.getSelectedRow();
        if (barisTampilan < 0) {
            pesanFilter.setText("Pilih salah satu baris riwayat terlebih dahulu.");
            return;
        }
        int barisData = tabel.convertRowIndexToModel(barisTampilan);
        if (barisData < 0 || barisData >= barisModel.size()) {
            return;
        }
        Transaksi transaksi = barisModel.get(barisData).transaksi();
        JDialog dialog = new JDialog(
                SwingUtilities.getWindowAncestor(this), "Detail Transaksi",
                Dialog.ModalityType.APPLICATION_MODAL);
        JPanel konten = new JPanel(new BorderLayout(0, 14));
        konten.setOpaque(true);
        konten.setBackground(Ui.LATAR);
        konten.setBorder(new EmptyBorder(18, 18, 18, 18));

        JPanel info = Ui.kartu("Informasi transaksi");
        JPanel nilai = new JPanel(new GridLayout(5, 2, 10, 6));
        nilai.setOpaque(false);
        nilai.add(Ui.labelField("Jenis"));
        nilai.add(new JLabel(jenisTampil(transaksi)));
        nilai.add(Ui.labelField("Tanggal"));
        nilai.add(new JLabel(transaksi.getTanggal().toString()));
        if (barang != null) {
            nilai.add(Ui.labelField("Barang"));
            nilai.add(new JLabel(barang.getNama()));
            nilai.add(Ui.labelField("Jumlah transaksi"));
            nilai.add(new JLabel(transaksi.getJumlah() + " "
                    + barang.getSatuan()));
        } else {
            nilai.add(Ui.labelField("Barang"));
            nilai.add(new JLabel("Tidak ada barang dipilih"));
            nilai.add(Ui.labelField("Jumlah transaksi"));
            nilai.add(new JLabel(String.valueOf(transaksi.getJumlah())));
        }
        nilai.add(Ui.labelField("Alasan"));
        nilai.add(new JLabel(transaksi.getAlasan() == null
                || transaksi.getAlasan().isBlank()
                        ? "Tidak ada" : transaksi.getAlasan()));
        info.add(nilai, BorderLayout.CENTER);

        JPanel rincian = Ui.kartu("Rincian per batch");
        DefaultTableModel modelDetail = Ui.model("Kode Batch", "Jumlah");
        JTable tabelDetail = Ui.tabel(modelDetail);
        tabelDetail.setName("tabelDetailRiwayat");
        Ui.rataKanan(tabelDetail, 1);
        for (RincianTransaksi item : transaksi.getRincian()) {
            modelDetail.addRow(new Object[]{
                item.getBatch().getKode(),
                barang == null ? String.valueOf(item.getJumlah())
                        : item.getJumlah() + " " + barang.getSatuan()
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

    @Override
    protected void saatBarangBerubah() {
        pesanFilter.setText(" ");
        bersihkanSeleksiDetail();
    }

    @Override
    public void perbarui() {
        boolean adaBarang = perbaruiBarangTerpilih();
        infoBarang.setText(adaBarang
                ? "Riwayat untuk " + barang.getNama() + " · "
                + barang.getSatuan()
                : "Belum ada barang. Tambahkan melalui halaman Barang.");
        terapkanFilter();
    }

    private record BarisRiwayat(Transaksi transaksi, RincianTransaksi rincian) {
    }

}
