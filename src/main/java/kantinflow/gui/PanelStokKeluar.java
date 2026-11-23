package kantinflow.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

import kantinflow.BatchStok;
import kantinflow.FefoAllocator;
import kantinflow.PengeluaranStok;
import kantinflow.RincianTransaksi;
import kantinflow.StatusKedaluwarsa;
import kantinflow.StokTidakCukupException;

/**
 * Halaman pengeluaran manual dan FEFO untuk barang terpilih.
 */
public class PanelStokKeluar extends PanelBarangTerpilih {
    private final JComboBox<BatchStok> comboBatch = Ui.combo();
    private final JLabel peringatanBatchManual = Ui.pesan();
    private final JLabel labelSisa = new JLabel("-");
    private final JTextField fieldJumlahManual = Ui.field(8);
    private final JLabel labelJumlahManual = Ui.labelField("Jumlah");
    private final JButton tombolManual = Ui.utama("Keluarkan Manual");
    private final JTextField fieldJumlahFefo = Ui.field(8);
    private final JLabel labelJumlahFefo = Ui.labelField("Jumlah");
    private final PemilihTanggal tanggalFefo = new PemilihTanggal();
    private final JButton tombolFefo = Ui.utama("Keluarkan FEFO");
    private final JLabel pesanManual = Ui.pesan();
    private final JLabel pesanFefo = Ui.pesan();
    private final JLabel pesanTanggalFefo = Ui.pesan();
    private final PanelPratinjau panelPreviewFefo = new PanelPratinjau();
    private final JLabel labelPreviewFefo = new JLabel(" ");
    private final JLabel hasilAlokasi = Ui.keterangan(" ");
    private final DefaultTableModel modelBatch = Ui.model(
            "Kode Batch", "Sisa", "Tanggal Masuk", "Tanggal Kedaluwarsa",
            "Status");
    private final JTable tabelBatch = Ui.tabel(modelBatch);
    private JScrollPane gulirBatch;
    private LocalDate tanggalFefoAwal;

    public PanelStokKeluar(KonteksKantin konteks) {
        super(konteks, "Stok Keluar",
                "Keluarkan stok manual atau dengan FEFO.");
        pilihanBarang.setName("barangStokKeluar");
        comboBatch.setName("batchManual");
        peringatanBatchManual.setName("peringatanBatchManual");
        peringatanBatchManual.setFont(Ui.font(Font.BOLD, Ui.FONT_KECIL));
        peringatanBatchManual.setForeground(Ui.MERAH);
        fieldJumlahManual.setName("jumlahKeluar");
        fieldJumlahManual.putClientProperty("JTextField.placeholderText",
                "Contoh: 10");
        tombolManual.setName("tombolKeluarkan");
        tombolManual.setIcon(
                Ui.ikonAksi("stock-out", 16, java.awt.Color.WHITE));
        tombolManual.setIconTextGap(7);
        fieldJumlahFefo.setName("jumlahFefo");
        fieldJumlahFefo.putClientProperty("JTextField.placeholderText",
                "Contoh: 10");
        tanggalFefo.komponenTeks().setName("tanggalFefo");
        pesanTanggalFefo.setName("pesanTanggalFefo");
        labelPreviewFefo.setName("previewFefo");
        labelPreviewFefo.setFont(Ui.font(Font.PLAIN, Ui.FONT_CATATAN));
        kosongkanPesan(peringatanBatchManual);
        kosongkanPesan(pesanManual);
        kosongkanPesan(pesanFefo);
        kosongkanPesan(pesanTanggalFefo);
        labelSisa.setVisible(false);
        tombolFefo.setName("tombolFefo");
        tombolFefo.setIcon(Ui.ikonAksi("stock-out", 16, java.awt.Color.WHITE));
        tombolFefo.setIconTextGap(7);
        tabelBatch.setName("tabelBatchKeluar");
        Ui.rataKanan(tabelBatch, 1);
        Ui.pasangBadge(tabelBatch, 4);
        Ui.aturKolom(tabelBatch, 175, 85, 125, 145, 175);
        tabelBatch.getColumnModel().getColumn(4).setMinWidth(Ui.skala(165));
        Ui.gulirHorizontalSaatSempit(tabelBatch);
        comboBatch.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                if (value instanceof BatchStok batch) {
                    boolean kedaluwarsa = batchKedaluwarsa(batch);
                    String awalan = kedaluwarsa ? "[KEDALUWARSA] " : "";
                    setText(awalan + batch.getKode() + " — sisa "
                            + batch.getSisaJumlah() + " "
                            + (barang == null ? "-" : barang.getSatuan())
                            + " — kedaluwarsa "
                            + FormatTanggal.format(batch.getTanggalKedaluwarsa()));
                    if (!isSelected) {
                        setForeground(kedaluwarsa ? Ui.MERAH_BADGE : Ui.TEKS);
                    }
                }
                return this;
            }
        });
        comboBatch.addActionListener(event -> perbaruiSisa());
        tombolManual.addActionListener(event -> keluarkanManual());
        tombolFefo.addActionListener(event -> keluarkanFefo());
        tanggalFefoAwal = LocalDate.now();
        tanggalFefo.setTanggal(tanggalFefoAwal);
        tanggalFefo.tambahPerubahan(() -> {
            kosongkanPesan(pesanTanggalFefo);
            perbaruiPreviewFefo();
        });
        DocumentListener pendengarFefo = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                perbaruiPreviewFefo();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                perbaruiPreviewFefo();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                perbaruiPreviewFefo();
            }
        };
        fieldJumlahFefo.getDocument().addDocumentListener(pendengarFefo);
        tanggalFefo.komponenTeks().getDocument()
                .addDocumentListener(pendengarFefo);
        hasilAlokasi.setVisible(false);
        bangunIsi();
    }

    private void bangunIsi() {
        JPanel manualIsi = isiMode();
        manualIsi.add(bantuanMode(
                "Pilih batch sumber, lalu masukkan jumlah yang dikeluarkan."));
        JPanel formManual = buatGridDuaKolom(
                Ui.field("Batch", comboBatch, peringatanBatchManual),
                Ui.field(labelJumlahManual, fieldJumlahManual));
        formManual.setAlignmentX(LEFT_ALIGNMENT);
        manualIsi.add(formManual);

        labelSisa.setName("sisaBatch");
        siapkanInfoMode(labelSisa);
        manualIsi.add(labelSisa);

        pesanManual.setAlignmentX(LEFT_ALIGNMENT);
        pesanManual.setBorder(new EmptyBorder(Ui.skala(6), 0, 0, 0));
        manualIsi.add(pesanManual);
        manualIsi.add(aksiKiri(tombolManual));

        JPanel fefoIsi = isiMode();
        fefoIsi.add(bantuanMode(
                "Stok layak dengan tanggal kedaluwarsa terdekat digunakan lebih dahulu."));

        JPanel formFefo = buatGridDuaKolom(
                Ui.field(labelJumlahFefo, fieldJumlahFefo),
                Ui.field("Tanggal pengeluaran",
                        tanggalFefo, pesanTanggalFefo));
        formFefo.setAlignmentX(LEFT_ALIGNMENT);
        fefoIsi.add(formFefo);

        panelPreviewFefo.setAlignmentX(LEFT_ALIGNMENT);
        panelPreviewFefo.add(labelPreviewFefo, BorderLayout.CENTER);
        JPanel wadahPreview = new JPanel(new BorderLayout()) {
            private static final long serialVersionUID = 1L;

            @Override
            public Dimension getMaximumSize() {
                Dimension pref = getPreferredSize();
                return new Dimension(Integer.MAX_VALUE, pref.height);
            }
        };
        wadahPreview.setOpaque(false);
        wadahPreview.setAlignmentX(LEFT_ALIGNMENT);
        wadahPreview.setBorder(new EmptyBorder(Ui.skala(12), 0, 0, 0));
        wadahPreview.add(panelPreviewFefo, BorderLayout.CENTER);
        fefoIsi.add(wadahPreview);

        pesanFefo.setAlignmentX(LEFT_ALIGNMENT);
        pesanFefo.setBorder(new EmptyBorder(Ui.skala(6), 0, 0, 0));
        fefoIsi.add(pesanFefo);
        fefoIsi.add(aksiKiri(tombolFefo));

        siapkanInfoMode(hasilAlokasi);
        hasilAlokasi.setBorder(new EmptyBorder(Ui.skala(12), 0, 0, 0));
        fefoIsi.add(hasilAlokasi);

        JTabbedPane mode = new JTabbedPane();
        Ui.gayaTab(mode);
        mode.setCursor(java.awt.Cursor.getPredefinedCursor(
                java.awt.Cursor.HAND_CURSOR));
        manualIsi.setCursor(java.awt.Cursor.getDefaultCursor());
        fefoIsi.setCursor(java.awt.Cursor.getDefaultCursor());
        mode.addTab("Manual", manualIsi);
        mode.addTab("Otomatis (FEFO)", fefoIsi);

        JPanel isi = new JPanel();
        isi.setOpaque(false);
        isi.setLayout(new javax.swing.BoxLayout(
                isi, javax.swing.BoxLayout.Y_AXIS));
        JPanel pilihan = fieldBarang("Barang");
        pilihan.setAlignmentX(LEFT_ALIGNMENT);
        pilihan.setBorder(new EmptyBorder(0, 0, Ui.skala(14), 0));
        isi.add(pilihan);
        mode.setAlignmentX(LEFT_ALIGNMENT);
        isi.add(mode);

        JPanel kartu = Ui.kartu("Pengeluaran Stok");
        kartu.add(isi, BorderLayout.CENTER);

        JPanel daftar = Ui.kartu("Batch Tersedia");
        JPanel daftarIsi = new JPanel(new BorderLayout(0, 4));
        daftarIsi.setOpaque(false);
        gulirBatch = Ui.scrollTabel(tabelBatch, Ui.TINGGI_TABEL_STOK_MASUK);
        daftarIsi.add(gulirBatch, BorderLayout.CENTER);
        daftar.add(daftarIsi, BorderLayout.CENTER);

        JPanel halaman = new JPanel(
                new BorderLayout(0, Ui.skala(Ui.GAP_KARTU)));
        halaman.setOpaque(false);
        halaman.add(kartu, BorderLayout.NORTH);
        halaman.add(daftar, BorderLayout.CENTER);
        add(halaman, BorderLayout.CENTER);
    }

    private JPanel buatGridDuaKolom(JPanel kiri, JPanel kanan) {
        JPanel panel = new JPanel(new GridBagLayout()) {
            private static final long serialVersionUID = 1L;

            @Override
            public Dimension getMaximumSize() {
                Dimension pref = getPreferredSize();
                return new Dimension(Integer.MAX_VALUE, pref.height);
            }
        };
        panel.setOpaque(false);
        aturGridDuaKolom(panel, kiri, kanan, false);
        panel.addComponentListener(new ComponentAdapter() {
            private boolean sempit;

            @Override
            public void componentResized(ComponentEvent event) {
                boolean sekarang = panel.getWidth() > 0
                        && panel.getWidth() < Ui.skala(560);
                if (sekarang == sempit) {
                    return;
                }
                sempit = sekarang;
                aturGridDuaKolom(panel, kiri, kanan, sempit);
                panel.revalidate();
                panel.repaint();
            }
        });
        return panel;
    }

    private void aturGridDuaKolom(JPanel panel, JPanel kiri, JPanel kanan,
            boolean sempit) {
        panel.removeAll();
        int jarak = Ui.skala(Ui.GAP_FORM);
        if (!sempit) {
            panel.setLayout(new GridLayout(1, 2, jarak, 0));
            panel.add(kiri);
            panel.add(kanan);
            return;
        }
        panel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTH;
        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;
        panel.add(kiri, c);

        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTH;
        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 1;
        c.insets = new Insets(jarak, 0, 0, 0);
        panel.add(kanan, c);
    }

    private JLabel bantuanMode(String teks) {
        JLabel label = Ui.keterangan(teks);
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.setBorder(new EmptyBorder(0, 0, 12, 0));
        return label;
    }

    private void siapkanInfoMode(JLabel label) {
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.setFont(Ui.font(Font.PLAIN, Ui.FONT_KECIL));
        label.setForeground(Ui.TEKS_LEMBUT);
        label.setBorder(new EmptyBorder(6, 0, 0, 0));
    }

    private JPanel isiMode() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new javax.swing.BoxLayout(
                panel, javax.swing.BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(16, 0, 0, 0));
        return panel;
    }

    private JPanel aksiKiri(JButton tombol) {
        JPanel panel = new JPanel(new java.awt.FlowLayout(
                java.awt.FlowLayout.LEFT, 0, 0));
        panel.setOpaque(false);
        panel.setAlignmentX(LEFT_ALIGNMENT);
        panel.setBorder(new EmptyBorder(Ui.skala(16), 0, 0, 0));
        panel.add(tombol);
        return panel;
    }

    /**
     * Tanggal transaksi pengeluaran manual adalah hari ini. Ini sengaja
     * terpisah dari tanggal pemantauan ({@code konteks.getTanggalAcuan()})
     * yang hanya dipakai untuk memeriksa status stok.
     */
    private LocalDate tanggalTransaksiManual() {
        return LocalDate.now();
    }

    private boolean batchKedaluwarsa(BatchStok batch) {
        return batchKedaluwarsa(batch, tanggalTransaksiManual());
    }

    private boolean batchKedaluwarsa(BatchStok batch, LocalDate tanggalTransaksi) {
        if (batch == null) {
            return false;
        }
        return batch.statusPada(tanggalTransaksi) == StatusKedaluwarsa.KEDALUWARSA;
    }

    private void perbaruiSisa() {
        BatchStok batch = (BatchStok) comboBatch.getSelectedItem();
        if (barang == null) {
            kosongkanPesan(peringatanBatchManual);
            labelSisa.setText(
                    "Belum ada barang. Tambahkan melalui halaman Barang.");
            labelSisa.setForeground(Ui.TEKS_LEMBUT);
            labelSisa.setVisible(true);
            tombolManual.setEnabled(false);
            tombolManual.setToolTipText(null);
            return;
        }
        if (batch == null) {
            kosongkanPesan(peringatanBatchManual);
            labelSisa.setText("Tidak ada batch bersisa.");
            labelSisa.setForeground(Ui.TEKS_LEMBUT);
            labelSisa.setVisible(true);
            tombolManual.setEnabled(false);
            tombolManual.setToolTipText(null);
            return;
        }
        labelSisa.setText(" ");
        labelSisa.setVisible(false);
        if (batchKedaluwarsa(batch)) {
            aturPesan(peringatanBatchManual,
                    "Peringatan: Batch ini sudah kedaluwarsa!");
            tombolManual.setEnabled(false);
            tombolManual.setToolTipText(
                    "Batch kedaluwarsa tidak dapat dikeluarkan. "
                    + "Gunakan halaman Pembuangan.");
        } else {
            kosongkanPesan(peringatanBatchManual);
            tombolManual.setEnabled(true);
            tombolManual.setToolTipText(null);
        }
    }

    private void perbaruiBatchTersedia() {
        modelBatch.setRowCount(0);
        if (barang != null) {
            LocalDate acuan = konteks.getTanggalAcuan();
            List<BatchStok> bersisa = new ArrayList<>();
            for (BatchStok batch : barang.getDaftarBatch()) {
                if (batch.getSisaJumlah() > 0) {
                    bersisa.add(batch);
                }
            }
            bersisa.sort(FefoAllocator.URUTAN_FEFO);
            for (BatchStok batch : bersisa) {
                modelBatch.addRow(new Object[]{
                    batch.getKode(),
                    batch.getSisaJumlah() + " " + barang.getSatuan(),
                    batch.getTanggalMasuk(),
                    batch.getTanggalKedaluwarsa(),
                    batch.statusPada(acuan).getLabel()
                });
            }
        }
        Ui.sesuaikanTinggiTabel(gulirBatch, tabelBatch, 8);
    }

    private void perbaruiPreviewFefo() {
        if (barang == null) {
            aturPreviewFefo(
                    "Preview FEFO: belum ada barang yang dipilih.",
                    Ui.NETRAL_LEMBUT, Ui.GARIS, Ui.TEKS_LEMBUT);
            return;
        }
        LocalDate tanggal = tanggalFefo.getTanggal();
        if (tanggal == null || !tanggalFefo.isTextFieldValid()) {
            aturPreviewFefo(
                    "Preview FEFO: pilih tanggal pengeluaran yang valid "
                    + "untuk menghitung alokasi batch.",
                    Ui.KUNING_LEMBUT, Ui.KUNING, Ui.KUNING_BADGE);
            return;
        }
        String teksJumlah = fieldJumlahFefo.getText() == null
                ? "" : fieldJumlahFefo.getText().trim();
        if (teksJumlah.isEmpty()) {
            int layak = barang.hitungStokLayak(tanggal);
            aturPreviewFefo(
                    "Preview FEFO: masukkan jumlah untuk melihat rincian "
                    + "batch yang akan terpotong (stok layak tersedia: "
                    + layak + " " + barang.getSatuan() + ").",
                    Ui.NETRAL_LEMBUT, Ui.GARIS, Ui.TEKS_LEMBUT);
            return;
        }
        int jumlah;
        try {
            jumlah = Integer.parseInt(teksJumlah);
        } catch (NumberFormatException exception) {
            aturPreviewFefo(
                    "Preview FEFO: jumlah harus berupa angka bulat.",
                    Ui.MERAH_LEMBUT, Ui.MERAH, Ui.MERAH_BADGE);
            return;
        }
        if (jumlah <= 0) {
            aturPreviewFefo(
                    "Preview FEFO: jumlah harus lebih dari 0.",
                    Ui.MERAH_LEMBUT, Ui.MERAH, Ui.MERAH_BADGE);
            return;
        }

        try {
            List<RincianTransaksi> alokasi = new FefoAllocator()
                    .susun(barang, jumlah, tanggal);
            StringBuilder detail = new StringBuilder("Preview FEFO: ");
            for (int i = 0; i < alokasi.size(); i++) {
                if (i > 0) {
                    detail.append(", ");
                }
                RincianTransaksi rincian = alokasi.get(i);
                detail.append(rincian.getJumlah())
                        .append(" ")
                        .append(barang.getSatuan())
                        .append(" dari ")
                        .append(rincian.getBatch().getKode())
                        .append(" (exp ")
                        .append(FormatTanggal.format(
                                rincian.getBatch().getTanggalKedaluwarsa()))
                        .append(")");
            }
            aturPreviewFefo(detail.toString(),
                    Ui.BIRU_LEMBUT, Ui.BIRU_GARIS, Ui.BIRU_BADGE);
        } catch (StokTidakCukupException | IllegalArgumentException exception) {
            aturPreviewFefo("Preview FEFO: " + exception.getMessage(),
                    Ui.MERAH_LEMBUT, Ui.MERAH, Ui.MERAH_BADGE);
        }
    }

    private void aturPreviewFefo(String teks, Color latar, Color garis,
            Color warnaTeks) {
        panelPreviewFefo.aturWarna(latar, garis);
        labelPreviewFefo.setForeground(warnaTeks);
        labelPreviewFefo.setText("<html>" + escapeHtml(teks) + "</html>");
        panelPreviewFefo.revalidate();
        panelPreviewFefo.repaint();
    }

    private static String escapeHtml(String teks) {
        if (teks == null) {
            return "";
        }
        return teks.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private void keluarkanManual() {
        kosongkanPesan(pesanManual);
        if (barang == null) {
            aturPesan(pesanManual,
                    "Belum ada barang. Tambahkan melalui halaman Barang.");
            return;
        }
        BatchStok batch = (BatchStok) comboBatch.getSelectedItem();
        if (batch == null) {
            aturPesan(pesanManual, "Pilih batch terlebih dahulu.");
            comboBatch.requestFocusInWindow();
            return;
        }
        LocalDate tanggalTransaksi = tanggalTransaksiManual();
        if (batchKedaluwarsa(batch, tanggalTransaksi)) {
            aturPesan(peringatanBatchManual,
                    "Peringatan: Batch ini sudah kedaluwarsa!");
            aturPesan(pesanManual,
                    "Batch yang sudah kedaluwarsa tidak dapat dikeluarkan. "
                    + "Gunakan halaman Pembuangan.");
            tombolManual.setEnabled(false);
            return;
        }
        int jumlah;
        try {
            jumlah = Integer.parseInt(fieldJumlahManual.getText().trim());
        } catch (NumberFormatException exception) {
            aturPesan(pesanManual, "Jumlah harus berupa angka bulat.");
            fieldJumlahManual.requestFocusInWindow();
            return;
        }

        PengeluaranStok pengeluaran = new PengeluaranStok(tanggalTransaksi);
        try {
            pengeluaran.keluarkan(barang, batch, jumlah);
        } catch (StokTidakCukupException | IllegalArgumentException exception) {
            aturPesan(pesanManual,
                    "Pengeluaran ditolak: " + exception.getMessage());
            konteks.tampilkanPesan("Pengeluaran gagal: "
                    + exception.getMessage(), false);
            return;
        }

        if (konteks.simpanTransaksi(barang, pengeluaran,
                "Pengeluaran berhasil: " + batch.getKode() + ", " + jumlah
                + " " + barang.getSatuan() + ".")) {
            fieldJumlahManual.setText("");
            kosongkanPesan(pesanManual);
        } else {
            aturPesan(pesanManual,
                    "Pengeluaran gagal disimpan. Lihat status.");
        }
    }

    private void keluarkanFefo() {
        kosongkanPesan(pesanFefo);
        kosongkanPesan(pesanTanggalFefo);
        hasilAlokasi.setVisible(false);
        if (barang == null) {
            aturPesan(pesanFefo,
                    "Belum ada barang. Tambahkan melalui halaman Barang.");
            return;
        }
        int jumlah;
        try {
            jumlah = Integer.parseInt(fieldJumlahFefo.getText().trim());
        } catch (NumberFormatException exception) {
            aturPesan(pesanFefo, "Jumlah harus berupa angka bulat.");
            fieldJumlahFefo.requestFocusInWindow();
            return;
        }

        LocalDate tanggal = tanggalFefo.getTanggal();
        if (tanggal == null) {
            boolean kosong = tanggalFefo.komponenTeks()
                    .getText().trim().isEmpty();
            aturPesan(pesanTanggalFefo, kosong
                    ? "Tanggal pengeluaran wajib diisi."
                    : "Format tanggal pengeluaran tidak valid.");
            tanggalFefo.komponenTeks().requestFocusInWindow();
            return;
        }

        PengeluaranStok pengeluaran = new PengeluaranStok(tanggal);
        try {
            pengeluaran.keluarkanFefo(barang, jumlah, tanggal);
        } catch (StokTidakCukupException | IllegalArgumentException exception) {
            aturPesan(pesanFefo,
                    "Pengeluaran ditolak: " + exception.getMessage());
            konteks.tampilkanPesan("Pengeluaran FEFO gagal: "
                    + exception.getMessage(), false);
            return;
        }

        String alokasi = formatAlokasi(pengeluaran);
        if (konteks.simpanTransaksi(barang, pengeluaran,
                "Pengeluaran FEFO berhasil: " + alokasi + ". Saldo: "
                + barang.hitungSaldo() + " " + barang.getSatuan() + ".")) {
            fieldJumlahFefo.setText("");
            tanggalFefoAwal = LocalDate.now();
            tanggalFefo.setTanggal(tanggalFefoAwal);
            kosongkanPesan(pesanFefo);
            hasilAlokasi.setText("Alokasi: " + alokasi);
            hasilAlokasi.setVisible(true);
            hasilAlokasi.revalidate();
            perbaruiPreviewFefo();
        } else {
            aturPesan(pesanFefo,
                    "Pengeluaran gagal disimpan. Lihat status.");
        }
    }

    private String formatAlokasi(PengeluaranStok pengeluaran) {
        StringBuilder hasil = new StringBuilder();
        for (RincianTransaksi rincian : pengeluaran.getRincian()) {
            if (hasil.length() > 0) {
                hasil.append(", ");
            }
            hasil.append(rincian.getBatch().getKode())
                    .append(" ")
                    .append(rincian.getJumlah())
                    .append(" ")
                    .append(barang == null ? "" : barang.getSatuan());
        }
        return hasil.toString();
    }

    private void perbaruiLabelJumlah() {
        String teks = Ui.teksJumlah(barang);
        labelJumlahManual.setText(teks);
        labelJumlahFefo.setText(teks);
    }

    @Override
    protected void saatBarangBerubah() {
        fieldJumlahManual.setText("");
        fieldJumlahFefo.setText("");
        perbaruiLabelJumlah();
        tanggalFefoAwal = LocalDate.now();
        tanggalFefo.setTanggal(tanggalFefoAwal);
        kosongkanPesan(peringatanBatchManual);
        kosongkanPesan(pesanManual);
        kosongkanPesan(pesanFefo);
        kosongkanPesan(pesanTanggalFefo);
        hasilAlokasi.setText(" ");
        hasilAlokasi.setVisible(false);
        comboBatch.removeAllItems();
        perbaruiPreviewFefo();
        perbaruiBatchTersedia();
    }

    @Override
    public void perbarui() {
        boolean adaBarang = perbaruiBarangTerpilih();
        perbaruiLabelJumlah();
        if (!adaBarang) {
            comboBatch.removeAllItems();
            kosongkanPesan(peringatanBatchManual);
            labelSisa.setText(
                    "Belum ada barang. Tambahkan melalui halaman Barang.");
            labelSisa.setForeground(Ui.TEKS_LEMBUT);
            labelSisa.setVisible(true);
            tombolManual.setEnabled(false);
            tombolFefo.setEnabled(false);
            perbaruiPreviewFefo();
            perbaruiBatchTersedia();
            return;
        }
        tombolFefo.setEnabled(true);
        BatchStok sebelum = (BatchStok) comboBatch.getSelectedItem();
        String kodeTerpilih = sebelum == null ? null : sebelum.getKode();
        comboBatch.removeAllItems();
        for (BatchStok batch : barang.getDaftarBatch()) {
            if (batch.getSisaJumlah() > 0) {
                comboBatch.addItem(batch);
            }
        }
        if (kodeTerpilih != null) {
            for (int i = 0; i < comboBatch.getItemCount(); i++) {
                if (kodeTerpilih.equals(comboBatch.getItemAt(i).getKode())) {
                    comboBatch.setSelectedIndex(i);
                    break;
                }
            }
        }
        perbaruiSisa();
        perbaruiPreviewFefo();
        perbaruiBatchTersedia();
    }

    @Override
    public boolean adaIsianBelumDisimpan() {
        return !fieldJumlahManual.getText().trim().isEmpty()
                || !fieldJumlahFefo.getText().trim().isEmpty()
                || !Objects.equals(tanggalFefo.getTanggal(), tanggalFefoAwal);
    }

    private static final class PanelPratinjau extends JPanel {
        private Color latar = Ui.NETRAL_LEMBUT;
        private Color garis = Ui.GARIS;

        private PanelPratinjau() {
            super(new BorderLayout());
            setOpaque(false);
            setBorder(new EmptyBorder(Ui.skala(10), Ui.skala(12),
                    Ui.skala(10), Ui.skala(12)));
        }

        private void aturWarna(Color latar, Color garis) {
            this.latar = latar;
            this.garis = garis;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                int busur = Ui.skala(Ui.RADIUS_KONTROL);
                int lebar = Math.max(0, getWidth() - 1);
                int tinggi = Math.max(0, getHeight() - 1);
                g2.setColor(latar);
                g2.fillRoundRect(0, 0, lebar, tinggi, busur, busur);
                g2.setColor(garis);
                g2.drawRoundRect(0, 0, lebar, tinggi, busur, busur);
            } finally {
                g2.dispose();
            }
            super.paintComponent(graphics);
        }
    }
}
