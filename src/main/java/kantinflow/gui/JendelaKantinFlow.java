package kantinflow.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import kantinflow.Barang;
import kantinflow.BatchStok;
import kantinflow.PembuanganStok;
import kantinflow.PenerimaanStok;
import kantinflow.PengeluaranStok;
import kantinflow.RincianTransaksi;
import kantinflow.StatusKedaluwarsa;
import kantinflow.StokTidakCukupException;
import kantinflow.Transaksi;
import kantinflow.penyimpanan.PenyimpananException;
import kantinflow.penyimpanan.PenyimpananSQLite;

public class JendelaKantinFlow {
    private static final Color BIRU = new Color(30, 42, 85);
    private static final Color ABU = new Color(89, 89, 89);
    private static final Color LATAR = new Color(245, 247, 251);
    private static final Color HIJAU = new Color(30, 123, 77);
    private static final Color MERAH = new Color(176, 0, 32);
    private static final Color GARIS = new Color(197, 208, 222);

    private final PenyimpananSQLite penyimpanan;
    private final JFrame frame;
    private final DefaultTableModel modelBatch;
    private final JTable tabelBatch;
    private final DefaultTableModel modelRiwayat;
    private final JTable tabelRiwayat;
    private final DefaultTableModel modelPemantauan;
    private final JTable tabelPemantauan;
    private final JTextField fieldKode = new JTextField(16);
    private final JTextField fieldJumlah = new JTextField(16);
    private final JTextField fieldTanggalMasuk = new JTextField(16);
    private final JTextField fieldTanggalKedaluwarsa = new JTextField(16);
    private final JTextField fieldJumlahKeluar = new JTextField(7);
    private final JTextField fieldJumlahFefo = new JTextField(5);
    private final JTextField fieldTanggalFefo = new JTextField(11);
    private final JTextField fieldTanggalPemantauan = new JTextField(11);
    private final JTextField fieldStokMinimum = new JTextField(6);
    private final JComboBox<Barang> comboBarangAktif = new JComboBox<>();
    private final JLabel labelNamaBarang = new JLabel();
    private final JTextField fieldNamaBarang = new JTextField(18);
    private final JTextField fieldSatuanBarang = new JTextField(12);
    private final JComboBox<BatchStok> comboBatchBuang = new JComboBox<>();
    private final JTextField fieldJumlahBuang = new JTextField(7);
    private final JTextField fieldTanggalBuang = new JTextField(11);
    private final JTextField fieldAlasanBuang = new JTextField(24);
    private final JLabel labelBatchDipilih = new JLabel("Belum ada batch dipilih");
    private final JLabel labelSaldo = new JLabel("0 kotak");
    private final JLabel labelJumlahBatch = new JLabel("0 batch");
    private final JLabel labelStokFisik = new JLabel("Fisik: 0");
    private final JLabel labelStokLayak = new JLabel("Layak: -");
    private final JLabel labelStokMinimum = new JLabel("Minimum: 0");
    private final JLabel labelStatusRestok = new JLabel("Status: Belum diperiksa");
    private final JTextArea areaStatus = new JTextArea(3, 40);
    private final JButton tombolSimpan = new JButton("Simpan Penerimaan");
    private final JButton tombolKeluarkan = new JButton("Keluarkan Stok");
    private final JButton tombolFefo = new JButton("Keluarkan FEFO");
    private final JButton tombolPeriksa = new JButton("Periksa Kedaluwarsa");
    private final JButton tombolSimpanMinimum = new JButton("Simpan Minimum");
    private final JButton tombolBuang = new JButton("Buang Stok");
    private final JButton tombolTambahBarang = new JButton("Tambah Barang");
    private final JButton tombolMuatUlang = new JButton("Muat Ulang");
    private final List<Barang> daftarBarang;
    private final List<Transaksi> riwayat = new ArrayList<>();
    private Barang barang;
    private LocalDate tanggalPemantauanTerakhir;
    private boolean menggantiBarang;
    private boolean sudahDitutup;

    public JendelaKantinFlow(PenyimpananSQLite penyimpanan) {
        if (penyimpanan == null) {
            throw new IllegalArgumentException("Penyimpanan wajib diisi");
        }
        this.penyimpanan = penyimpanan;
        this.daftarBarang = new ArrayList<>(penyimpanan.muatSemuaBarang());
        if (this.daftarBarang.isEmpty()) {
            throw new IllegalArgumentException("Database belum memiliki barang");
        }
        this.barang = this.daftarBarang.get(0);
        this.riwayat.addAll(penyimpanan.muatRiwayat(this.barang));
        this.frame = new JFrame("KantinFlow - Stok Barang");

        this.modelBatch = new DefaultTableModel(
                new Object[]{"Kode Batch", "Sisa", "Tanggal Masuk", "Tanggal Kedaluwarsa"},
                0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.tabelBatch = new JTable(modelBatch);
        this.modelRiwayat = new DefaultTableModel(
                new Object[]{"Tanggal", "Jenis Transaksi", "Kode Batch", "Jumlah", "Alasan"},
                0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.tabelRiwayat = new JTable(modelRiwayat);
        this.modelPemantauan = new DefaultTableModel(
                new Object[]{"Kode Batch", "Sisa", "Tanggal Kedaluwarsa", "Status"},
                0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.tabelPemantauan = new JTable(modelPemantauan);

        bangunTampilan();
        aturNilaiAwal();
        perbaruiTampilan();
        setStatus("Data dan riwayat dimuat dari database. Siap menerima stok.", ABU);
    }

    private void bangunTampilan() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(LATAR);
        root.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BIRU);
        header.setBorder(new EmptyBorder(16, 22, 16, 22));

        JPanel identitas = new JPanel();
        identitas.setOpaque(false);
        identitas.setLayout(new javax.swing.BoxLayout(identitas, javax.swing.BoxLayout.Y_AXIS));
        labelNamaBarang.setName("labelNamaBarang");
        labelNamaBarang.setText(barang.getNama());
        labelNamaBarang.setFont(new Font("Segoe UI", Font.BOLD, 24));
        labelNamaBarang.setForeground(Color.WHITE);
        labelNamaBarang.setAlignmentX(0.0f);
        JLabel keterangan = new JLabel("Stok fisik, batch, dan riwayat transaksi");
        keterangan.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        keterangan.setForeground(new Color(200, 212, 235));
        keterangan.setAlignmentX(0.0f);
        identitas.add(labelNamaBarang);
        identitas.add(keterangan);

        JPanel pemilih = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        pemilih.setOpaque(false);
        pemilih.setAlignmentX(0.0f);
        JLabel labelPemilih = new JLabel("Barang aktif:");
        labelPemilih.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        labelPemilih.setForeground(new Color(200, 212, 235));
        comboBarangAktif.setName("barangAktif");
        comboBarangAktif.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        comboBarangAktif.setPreferredSize(new Dimension(240, 26));
        comboBarangAktif.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus) {
                super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                if (value instanceof Barang item) {
                    setText(item.getNama() + " (" + item.getSatuan() + ")");
                }
                return this;
            }
        });
        comboBarangAktif.addActionListener(event -> {
            if (!menggantiBarang) {
                gantiBarangAktif((Barang) comboBarangAktif.getSelectedItem());
            }
        });
        pemilih.add(labelPemilih);
        pemilih.add(comboBarangAktif);
        identitas.add(pemilih);

        JPanel saldoPanel = new JPanel();
        saldoPanel.setOpaque(false);
        saldoPanel.setLayout(new javax.swing.BoxLayout(saldoPanel, javax.swing.BoxLayout.Y_AXIS));
        labelSaldo.setName("labelSaldo");
        labelSaldo.setFont(new Font("Segoe UI", Font.BOLD, 28));
        labelSaldo.setForeground(new Color(126, 230, 226));
        labelSaldo.setHorizontalAlignment(SwingConstants.RIGHT);
        labelJumlahBatch.setName("labelJumlahBatch");
        labelJumlahBatch.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        labelJumlahBatch.setForeground(new Color(200, 212, 235));
        labelJumlahBatch.setHorizontalAlignment(SwingConstants.RIGHT);
        saldoPanel.add(labelSaldo);
        saldoPanel.add(labelJumlahBatch);

        header.add(identitas, BorderLayout.WEST);
        header.add(saldoPanel, BorderLayout.EAST);

        siapkanTabelBatch();
        siapkanTabelRiwayat();
        siapkanTabelPemantauan();

        JScrollPane scrollBatch = new JScrollPane(tabelBatch);
        scrollBatch.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(GARIS), "Daftar Batch"));
        scrollBatch.getViewport().setBackground(Color.WHITE);

        JPanel panelAksi = new JPanel(new GridLayout(2, 1, 0, 6));
        panelAksi.setOpaque(false);
        panelAksi.add(buatPanelPengeluaran());
        panelAksi.add(buatPanelFefo());

        JPanel batchSection = new JPanel(new BorderLayout(0, 8));
        batchSection.setOpaque(false);
        batchSection.add(scrollBatch, BorderLayout.CENTER);
        batchSection.add(panelAksi, BorderLayout.SOUTH);

        JPanel isi = new JPanel(new BorderLayout(14, 0));
        isi.setOpaque(false);
        isi.add(batchSection, BorderLayout.CENTER);
        isi.add(buatFormPenerimaan(), BorderLayout.EAST);

        JScrollPane scrollRiwayat = new JScrollPane(tabelRiwayat);
        scrollRiwayat.setBorder(BorderFactory.createLineBorder(GARIS));
        scrollRiwayat.getViewport().setBackground(Color.WHITE);

        JTabbedPane tabBawah = new JTabbedPane();
        tabBawah.setName("tabBawah");
        tabBawah.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tabBawah.addTab("Riwayat Transaksi", scrollRiwayat);
        tabBawah.addTab("Pemantauan Kedaluwarsa", buatPanelPemantauan());
        tabBawah.addTab("Pembuangan Stok", buatPanelPembuangan());
        tabBawah.addTab("Tambah Barang", buatPanelTambahBarang());

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, isi, tabBawah);
        split.setResizeWeight(0.68);
        split.setDividerSize(8);
        split.setBorder(null);
        split.setContinuousLayout(true);
        split.setDividerLocation(500);

        areaStatus.setName("status");
        areaStatus.setEditable(false);
        areaStatus.setLineWrap(true);
        areaStatus.setWrapStyleWord(true);
        areaStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        areaStatus.setBorder(new EmptyBorder(9, 12, 9, 12));
        areaStatus.setBackground(new Color(238, 243, 250));
        areaStatus.setOpaque(true);

        root.add(header, BorderLayout.NORTH);
        root.add(split, BorderLayout.CENTER);
        root.add(areaStatus, BorderLayout.SOUTH);

        frame.setContentPane(root);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                tutup();
            }
        });
        frame.setMinimumSize(new Dimension(1000, 820));
        frame.setSize(1120, 1020);
        frame.setLocationRelativeTo(null);
        frame.getRootPane().setDefaultButton(tombolSimpan);
    }

    private void siapkanTabelBatch() {
        tabelBatch.setName("tabelBatch");
        tabelBatch.setRowHeight(28);
        tabelBatch.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tabelBatch.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        pasangHeader(tabelBatch);
        tabelBatch.setSelectionBackground(new Color(210, 235, 235));
        tabelBatch.setGridColor(new Color(220, 227, 238));
        tabelBatch.setFillsViewportHeight(true);
        tabelBatch.getColumnModel().getColumn(0).setPreferredWidth(140);
        tabelBatch.getColumnModel().getColumn(1).setPreferredWidth(70);
        tabelBatch.getColumnModel().getColumn(2).setPreferredWidth(130);
        tabelBatch.getColumnModel().getColumn(3).setPreferredWidth(150);
        tabelBatch.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                perbaruiBatchDipilih();
            }
        });
    }

    private void siapkanTabelRiwayat() {
        tabelRiwayat.setName("tabelRiwayat");
        tabelRiwayat.setRowHeight(24);
        tabelRiwayat.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        tabelRiwayat.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        pasangHeader(tabelRiwayat);
        tabelRiwayat.setGridColor(new Color(220, 227, 238));
        tabelRiwayat.setFillsViewportHeight(true);
        tabelRiwayat.getColumnModel().getColumn(0).setPreferredWidth(95);
        tabelRiwayat.getColumnModel().getColumn(1).setPreferredWidth(135);
        tabelRiwayat.getColumnModel().getColumn(2).setPreferredWidth(110);
        tabelRiwayat.getColumnModel().getColumn(3).setPreferredWidth(85);
        tabelRiwayat.getColumnModel().getColumn(4).setPreferredWidth(220);
    }

    private void siapkanTabelPemantauan() {
        tabelPemantauan.setName("tabelPemantauan");
        tabelPemantauan.setRowHeight(26);
        tabelPemantauan.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tabelPemantauan.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        pasangHeader(tabelPemantauan);
        tabelPemantauan.setGridColor(new Color(220, 227, 238));
        tabelPemantauan.setFillsViewportHeight(true);
        tabelPemantauan.getColumnModel().getColumn(0).setPreferredWidth(150);
        tabelPemantauan.getColumnModel().getColumn(1).setPreferredWidth(90);
        tabelPemantauan.getColumnModel().getColumn(2).setPreferredWidth(160);
        tabelPemantauan.getColumnModel().getColumn(3).setPreferredWidth(220);
        tabelPemantauan.getColumnModel().getColumn(3).setCellRenderer(
                new DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                String status = String.valueOf(value);
                if ("Kedaluwarsa".equals(status)) {
                    label.setForeground(MERAH);
                } else if ("Segera kedaluwarsa".equals(status)) {
                    label.setForeground(new Color(166, 91, 0));
                } else if ("Aman".equals(status)) {
                    label.setForeground(HIJAU);
                } else {
                    label.setForeground(ABU);
                }
                return label;
            }
        });
    }

    private JPanel buatPanelPemantauan() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel kontrol = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        kontrol.setOpaque(false);
        kontrol.add(new JLabel("Tanggal pengecekan (yyyy-MM-dd):"));
        fieldTanggalPemantauan.setName("tanggalPemantauan");
        fieldTanggalPemantauan.setToolTipText("Format yyyy-MM-dd");
        kontrol.add(fieldTanggalPemantauan);
        tombolPeriksa.setName("tombolPeriksa");
        tombolPeriksa.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tombolPeriksa.addActionListener(event -> periksaKedaluwarsa());
        kontrol.add(tombolPeriksa);

        kontrol.add(new JLabel("Stok minimum:"));
        fieldStokMinimum.setName("stokMinimum");
        kontrol.add(fieldStokMinimum);
        tombolSimpanMinimum.setName("tombolSimpanMinimum");
        tombolSimpanMinimum.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tombolSimpanMinimum.addActionListener(event -> simpanStokMinimum());
        kontrol.add(tombolSimpanMinimum);

        JPanel ringkasan = new JPanel(new GridLayout(1, 4, 8, 0));
        ringkasan.setOpaque(false);
        ringkasan.add(buatLabelRingkasan(labelStokFisik, "labelStokFisik"));
        ringkasan.add(buatLabelRingkasan(labelStokLayak, "labelStokLayak"));
        ringkasan.add(buatLabelRingkasan(labelStokMinimum, "labelStokMinimum"));
        ringkasan.add(buatLabelRingkasan(labelStatusRestok, "labelStatusRestok"));

        JLabel petunjuk = new JLabel(
                "Hanya batch bersisa yang ditampilkan. Stok layak memakai aturan FEFO.");
        petunjuk.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        petunjuk.setForeground(ABU);

        JPanel atas = new JPanel(new BorderLayout(0, 4));
        atas.setOpaque(false);
        atas.add(kontrol, BorderLayout.NORTH);
        atas.add(ringkasan, BorderLayout.CENTER);
        atas.add(petunjuk, BorderLayout.SOUTH);

        JScrollPane scroll = new JScrollPane(tabelPemantauan);
        scroll.setBorder(BorderFactory.createLineBorder(GARIS));
        scroll.getViewport().setBackground(Color.WHITE);

        panel.add(atas, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JLabel buatLabelRingkasan(JLabel label, String nama) {
        label.setName(nama);
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        label.setForeground(ABU);
        label.setBorder(new EmptyBorder(3, 2, 3, 2));
        return label;
    }

    private JPanel buatPanelTambahBarang() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        fieldNamaBarang.setName("namaBarangBaru");
        fieldSatuanBarang.setName("satuanBarangBaru");
        tombolTambahBarang.setName("tombolTambahBarang");
        tombolTambahBarang.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tombolTambahBarang.addActionListener(event -> tambahBarang());

        JPanel isian = new JPanel(new GridLayout(1, 2, 12, 4));
        isian.setOpaque(false);
        isian.add(buatField("Nama barang", fieldNamaBarang));
        isian.add(buatField("Satuan", fieldSatuanBarang));

        JPanel form = new JPanel(new BorderLayout(0, 8));
        form.setOpaque(false);
        form.add(isian, BorderLayout.CENTER);
        form.add(tombolTambahBarang, BorderLayout.SOUTH);

        JLabel petunjuk = new JLabel(
                "Nama dan satuan wajib diisi. Barang baru langsung menjadi barang aktif.");
        petunjuk.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        petunjuk.setForeground(ABU);

        panel.add(form, BorderLayout.CENTER);
        panel.add(petunjuk, BorderLayout.SOUTH);
        return panel;
    }

    private void tambahBarang() {
        String nama = fieldNamaBarang.getText().trim();
        String satuan = fieldSatuanBarang.getText().trim();
        try {
            Barang baru = new Barang(nama, satuan);
            baru = penyimpanan.simpanBarang(baru);
            daftarBarang.add(baru);
            this.barang = baru;
            this.riwayat.clear();
            this.tanggalPemantauanTerakhir = null;
            perbaruiTampilan();
            fieldNamaBarang.setText("");
            fieldSatuanBarang.setText("");
            setStatus("Barang ditambahkan: " + barang.getNama()
                    + " (" + barang.getSatuan() + ").", HIJAU);
        } catch (IllegalArgumentException | PenyimpananException exception) {
            setStatus("Tambah barang gagal: " + exception.getMessage(), MERAH);
        }
    }

    private void gantiBarangAktif(Barang dipilih) {
        if (dipilih == null || (barang != null && dipilih.getId() == barang.getId())) {
            return;
        }
        try {
            Barang terbaru = penyimpanan.muatBarang(dipilih.getId());
            for (int i = 0; i < daftarBarang.size(); i++) {
                if (daftarBarang.get(i).getId() == terbaru.getId()) {
                    daftarBarang.set(i, terbaru);
                    break;
                }
            }
            this.barang = terbaru;
            this.riwayat.clear();
            this.riwayat.addAll(penyimpanan.muatRiwayat(terbaru));
            this.tanggalPemantauanTerakhir = null;
            fieldJumlahKeluar.setText("");
            perbaruiTampilan();
            setStatus("Barang aktif: " + barang.getNama()
                    + " (" + barang.getSatuan() + ").", ABU);
        } catch (PenyimpananException exception) {
            setStatus("Gagal mengganti barang: " + exception.getMessage(), MERAH);
            perbaruiComboBarang();
        }
    }

    private void perbaruiComboBarang() {
        menggantiBarang = true;
        try {
            comboBarangAktif.removeAllItems();
            for (Barang item : daftarBarang) {
                comboBarangAktif.addItem(item);
            }
            for (int i = 0; i < comboBarangAktif.getItemCount(); i++) {
                if (comboBarangAktif.getItemAt(i).getId() == barang.getId()) {
                    comboBarangAktif.setSelectedIndex(i);
                    break;
                }
            }
        } finally {
            menggantiBarang = false;
        }
    }

    private JPanel buatPanelPembuangan() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        comboBatchBuang.setName("batchBuang");
        comboBatchBuang.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus) {
                super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                if (value instanceof BatchStok batch) {
                    setText(batch.getKode() + " | sisa " + batch.getSisaJumlah()
                            + " " + barang.getSatuan() + " | kedaluwarsa "
                            + batch.getTanggalKedaluwarsa());
                }
                return this;
            }
        });

        fieldJumlahBuang.setName("jumlahBuang");
        fieldTanggalBuang.setName("tanggalBuang");
        fieldTanggalBuang.setToolTipText("Format yyyy-MM-dd");
        fieldAlasanBuang.setName("alasanBuang");

        JPanel barisAtas = new JPanel(new GridLayout(1, 2, 12, 4));
        barisAtas.setOpaque(false);
        barisAtas.add(buatField("Batch", comboBatchBuang));
        barisAtas.add(buatField("Jumlah", fieldJumlahBuang));

        JPanel barisBawah = new JPanel(new GridLayout(1, 2, 12, 4));
        barisBawah.setOpaque(false);
        barisBawah.add(buatField("Tanggal pembuangan (yyyy-MM-dd)",
                fieldTanggalBuang));
        barisBawah.add(buatField("Alasan", fieldAlasanBuang));

        JPanel isian = new JPanel(new GridLayout(2, 1, 0, 6));
        isian.setOpaque(false);
        isian.add(barisAtas);
        isian.add(barisBawah);

        tombolBuang.setName("tombolBuang");
        tombolBuang.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tombolBuang.addActionListener(event -> buangStokKedaluwarsa());

        JPanel form = new JPanel(new BorderLayout(0, 8));
        form.setOpaque(false);
        form.add(isian, BorderLayout.CENTER);
        form.add(tombolBuang, BorderLayout.SOUTH);

        JLabel petunjuk = new JLabel(
                "Hanya batch berstatus Kedaluwarsa pada tanggal pembuangan yang dapat dibuang.");
        petunjuk.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        petunjuk.setForeground(ABU);

        panel.add(form, BorderLayout.CENTER);
        panel.add(petunjuk, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buatField(String teks, javax.swing.JComponent komponen) {
        JPanel panel = new JPanel(new BorderLayout(0, 3));
        panel.setOpaque(false);
        JLabel label = new JLabel(teks);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        panel.add(label, BorderLayout.NORTH);
        panel.add(komponen, BorderLayout.CENTER);
        return panel;
    }

    private void buangStokKedaluwarsa() {
        BatchStok batch = (BatchStok) comboBatchBuang.getSelectedItem();
        if (batch == null) {
            setStatus("Pilih batch yang akan dibuang terlebih dahulu.", MERAH);
            return;
        }

        int jumlah;
        try {
            jumlah = Integer.parseInt(fieldJumlahBuang.getText().trim());
        } catch (NumberFormatException exception) {
            setStatus("Jumlah harus berupa angka bulat.", MERAH);
            return;
        }

        LocalDate tanggalPembuangan;
        try {
            tanggalPembuangan = LocalDate.parse(fieldTanggalBuang.getText().trim());
        } catch (DateTimeParseException exception) {
            setStatus("Format tanggal tidak valid. Gunakan yyyy-MM-dd.", MERAH);
            return;
        }
        String alasan = fieldAlasanBuang.getText().trim();

        PembuanganStok pembuangan = new PembuanganStok(tanggalPembuangan);
        try {
            pembuangan.buang(barang, batch, jumlah, alasan);
        } catch (StokTidakCukupException exception) {
            muatUlang();
            setStatus(exception.getMessage(), MERAH);
            return;
        } catch (IllegalArgumentException exception) {
            muatUlang();
            setStatus("Pembuangan gagal: " + exception.getMessage(), MERAH);
            return;
        }

        try {
            penyimpanan.simpanTransaksi(barang, pembuangan);
        } catch (PenyimpananException exception) {
            boolean dimuatUlang = muatUlang();
            setStatus(
                    "Penyimpanan gagal: " + exception.getMessage()
                    + (dimuatUlang
                            ? ". Tampilan dimuat ulang dari database."
                            : ". Tampilan gagal dimuat ulang."),
                    MERAH);
            return;
        }

        String kode = batch.getKode();
        if (!muatUlang()) {
            return;
        }
        fieldJumlahBuang.setText("");
        fieldAlasanBuang.setText("");
        fieldTanggalBuang.setText(LocalDate.now().toString());
        setStatus("Pembuangan berhasil: " + kode + ", " + jumlah + " "
                + barang.getSatuan() + " pada " + tanggalPembuangan
                + ". Alasan: " + alasan + ".", HIJAU);
    }

    private void simpanStokMinimum() {
        int minimum;
        try {
            minimum = Integer.parseInt(fieldStokMinimum.getText().trim());
        } catch (NumberFormatException exception) {
            setStatus("Stok minimum harus berupa angka bulat.", MERAH);
            return;
        }
        if (minimum < 0) {
            setStatus("Stok minimum tidak boleh negatif.", MERAH);
            return;
        }

        try {
            penyimpanan.simpanStokMinimum(barang, minimum);
            barang.setStokMinimum(minimum);
            fieldStokMinimum.setText(String.valueOf(minimum));
            perbaruiRingkasanStok();
            setStatus("Stok minimum " + barang.getNama() + " disimpan: "
                    + minimum + " " + barang.getSatuan() + ".", HIJAU);
        } catch (IllegalArgumentException | PenyimpananException exception) {
            setStatus("Gagal menyimpan stok minimum: "
                    + exception.getMessage(), MERAH);
        }
    }

    private void perbaruiRingkasanStok() {
        int fisik = barang.hitungSaldo();
        labelStokFisik.setText("Fisik: " + fisik + " " + barang.getSatuan());
        labelStokMinimum.setText("Minimum: " + barang.getStokMinimum()
                + " " + barang.getSatuan());
        if (tanggalPemantauanTerakhir == null) {
            labelStokLayak.setText("Layak: -");
            labelStatusRestok.setText("Status: Belum diperiksa");
            labelStatusRestok.setForeground(ABU);
            return;
        }

        int layak = barang.hitungStokLayak(tanggalPemantauanTerakhir);
        labelStokLayak.setText("Layak: " + layak + " " + barang.getSatuan());
        boolean perluRestok = layak <= barang.getStokMinimum();
        labelStatusRestok.setText(
                "Status: " + (perluRestok ? "Perlu restok" : "Cukup"));
        labelStatusRestok.setForeground(perluRestok ? MERAH : HIJAU);
    }

    private void periksaKedaluwarsa() {
        LocalDate tanggalPengecekan;
        try {
            tanggalPengecekan = LocalDate.parse(
                    fieldTanggalPemantauan.getText().trim());
        } catch (DateTimeParseException exception) {
            setStatus("Format tanggal tidak valid. Gunakan yyyy-MM-dd.", MERAH);
            return;
        }

        tanggalPemantauanTerakhir = tanggalPengecekan;
        perbaruiPemantauan();
        if (modelPemantauan.getRowCount() == 0) {
            setStatus("Pemantauan " + tanggalPengecekan
                    + ": tidak ada batch bersisa.", ABU);
        } else {
            setStatus("Pemantauan " + tanggalPengecekan + " selesai: "
                    + modelPemantauan.getRowCount() + " batch bersisa diperiksa.",
                    ABU);
        }
    }

    private void perbaruiPemantauan() {
        modelPemantauan.setRowCount(0);
        if (tanggalPemantauanTerakhir == null) {
            perbaruiRingkasanStok();
            return;
        }
        for (BatchStok batch : barang.getDaftarBatch()) {
            if (batch.getSisaJumlah() <= 0) {
                continue;
            }
            StatusKedaluwarsa status = batch.statusPada(tanggalPemantauanTerakhir);
            modelPemantauan.addRow(new Object[]{
                batch.getKode(),
                batch.getSisaJumlah() + " " + barang.getSatuan(),
                batch.getTanggalKedaluwarsa().toString(),
                status.getLabel()
            });
        }
        perbaruiRingkasanStok();
    }

    private void pasangHeader(JTable tabel) {
        tabel.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabel.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                label.setOpaque(true);
                label.setBackground(BIRU);
                label.setForeground(Color.WHITE);
                label.setBorder(new EmptyBorder(8, 8, 8, 8));
                return label;
            }
        });
    }

    private JPanel buatFormPenerimaan() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setPreferredSize(new Dimension(325, 0));
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(GARIS), "Form Penerimaan"),
                new EmptyBorder(12, 14, 12, 14)));

        GridBagConstraints label = new GridBagConstraints();
        label.gridx = 0;
        label.gridy = 0;
        label.anchor = GridBagConstraints.WEST;
        label.insets = new Insets(5, 0, 2, 8);

        GridBagConstraints input = new GridBagConstraints();
        input.gridx = 0;
        input.gridy = 1;
        input.weightx = 1.0;
        input.fill = GridBagConstraints.HORIZONTAL;
        input.insets = new Insets(0, 0, 8, 0);

        fieldKode.setName("kodeBatch");
        fieldJumlah.setName("jumlah");
        fieldTanggalMasuk.setName("tanggalMasuk");
        fieldTanggalKedaluwarsa.setName("tanggalKedaluwarsa");
        fieldTanggalMasuk.setToolTipText("Format yyyy-MM-dd");
        fieldTanggalKedaluwarsa.setToolTipText("Format yyyy-MM-dd");

        tambahField(form, label, input, "Kode batch", fieldKode, 0);
        tambahField(form, label, input, "Jumlah", fieldJumlah, 2);
        tambahField(form, label, input, "Tanggal masuk (yyyy-MM-dd)",
                fieldTanggalMasuk, 4);
        tambahField(form, label, input, "Tanggal kedaluwarsa (yyyy-MM-dd)",
                fieldTanggalKedaluwarsa, 6);

        GridBagConstraints tombol = new GridBagConstraints();
        tombol.gridx = 0;
        tombol.gridy = 8;
        tombol.weightx = 1.0;
        tombol.fill = GridBagConstraints.HORIZONTAL;
        tombol.insets = new Insets(8, 0, 4, 0);
        tombolSimpan.setName("tombolSimpan");
        tombolSimpan.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tombolSimpan.addActionListener(event -> simpanPenerimaan());
        form.add(tombolSimpan, tombol);

        GridBagConstraints tombolKedua = new GridBagConstraints();
        tombolKedua.gridx = 0;
        tombolKedua.gridy = 9;
        tombolKedua.weightx = 1.0;
        tombolKedua.fill = GridBagConstraints.HORIZONTAL;
        tombolKedua.insets = new Insets(0, 0, 0, 0);
        tombolMuatUlang.setName("tombolMuatUlang");
        tombolMuatUlang.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tombolMuatUlang.addActionListener(event -> {
            if (muatUlang()) {
                setStatus("Data dan riwayat dimuat ulang dari database.", ABU);
            }
        });
        form.add(tombolMuatUlang, tombolKedua);

        GridBagConstraints pengisi = new GridBagConstraints();
        pengisi.gridx = 0;
        pengisi.gridy = 10;
        pengisi.weighty = 1.0;
        pengisi.fill = GridBagConstraints.VERTICAL;
        form.add(new JLabel(""), pengisi);

        return form;
    }

    private JPanel buatPanelPengeluaran() {
        JPanel panel = new JPanel(new BorderLayout(8, 4));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(GARIS), "Pengeluaran Manual"),
                new EmptyBorder(7, 10, 7, 10)));

        labelBatchDipilih.setName("labelBatchDipilih");
        labelBatchDipilih.setFont(new Font("Segoe UI", Font.BOLD, 11));
        labelBatchDipilih.setForeground(BIRU);
        panel.add(labelBatchDipilih, BorderLayout.NORTH);

        JPanel kontrol = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        kontrol.setOpaque(false);
        kontrol.add(new JLabel("Jumlah:"));
        fieldJumlahKeluar.setName("jumlahKeluar");
        kontrol.add(fieldJumlahKeluar);
        tombolKeluarkan.setName("tombolKeluarkan");
        tombolKeluarkan.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tombolKeluarkan.addActionListener(event -> keluarkanStokManual());
        kontrol.add(tombolKeluarkan);
        panel.add(kontrol, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buatPanelFefo() {
        JPanel panel = new JPanel(new BorderLayout(8, 4));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(GARIS),
                        "Pengeluaran FEFO (otomatis)"),
                new EmptyBorder(7, 10, 7, 10)));

        JPanel kontrol = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        kontrol.setOpaque(false);
        kontrol.add(new JLabel("Jumlah:"));
        fieldJumlahFefo.setName("jumlahFefo");
        kontrol.add(fieldJumlahFefo);
        kontrol.add(new JLabel("Tanggal (yyyy-MM-dd):"));
        fieldTanggalFefo.setName("tanggalFefo");
        fieldTanggalFefo.setToolTipText("Format yyyy-MM-dd");
        kontrol.add(fieldTanggalFefo);
        tombolFefo.setName("tombolFefo");
        tombolFefo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tombolFefo.addActionListener(event -> keluarkanStokFefo());
        kontrol.add(tombolFefo);
        panel.add(kontrol, BorderLayout.CENTER);

        return panel;
    }

    private void tambahField(
            JPanel form,
            GridBagConstraints label,
            GridBagConstraints input,
            String teks,
            javax.swing.JComponent field,
            int baris) {
        label.gridy = baris;
        form.add(new JLabel(teks), label);
        input.gridy = baris + 1;
        form.add(field, input);
    }

    private void aturNilaiAwal() {
        LocalDate hariIni = LocalDate.now();
        fieldTanggalMasuk.setText(hariIni.toString());
        fieldTanggalKedaluwarsa.setText(hariIni.plusMonths(1).toString());
        fieldTanggalFefo.setText(hariIni.toString());
        fieldTanggalPemantauan.setText(hariIni.toString());
        fieldTanggalBuang.setText(hariIni.toString());
    }

    public void tampilkan() {
        frame.setVisible(true);
        fieldKode.requestFocusInWindow();
    }

    public JFrame getFrame() {
        return frame;
    }

    public void tutup() {
        if (sudahDitutup) {
            return;
        }
        sudahDitutup = true;
        try {
            penyimpanan.close();
        } catch (PenyimpananException exception) {
            System.err.println("Gagal menutup database: " + exception.getMessage());
        } finally {
            frame.dispose();
        }
    }

    private void simpanPenerimaan() {
        String kode = fieldKode.getText().trim();
        String teksJumlah = fieldJumlah.getText().trim();

        int jumlah;
        try {
            jumlah = Integer.parseInt(teksJumlah);
        } catch (NumberFormatException exception) {
            setStatus("Jumlah harus berupa angka bulat.", MERAH);
            return;
        }

        LocalDate tanggalMasuk;
        LocalDate tanggalKedaluwarsa;
        try {
            tanggalMasuk = LocalDate.parse(fieldTanggalMasuk.getText().trim());
            tanggalKedaluwarsa = LocalDate.parse(
                    fieldTanggalKedaluwarsa.getText().trim());
        } catch (DateTimeParseException exception) {
            setStatus("Format tanggal tidak valid. Gunakan yyyy-MM-dd.", MERAH);
            return;
        }

        PenerimaanStok penerimaan = new PenerimaanStok(tanggalMasuk);
        try {
            penerimaan.terima(
                    barang,
                    kode,
                    jumlah,
                    tanggalMasuk,
                    tanggalKedaluwarsa);
        } catch (IllegalArgumentException exception) {
            muatUlang();
            setStatus("Penerimaan gagal: " + exception.getMessage(), MERAH);
            return;
        }

        try {
            penyimpanan.simpanTransaksi(barang, penerimaan);
        } catch (PenyimpananException exception) {
            boolean dimuatUlang = muatUlang();
            setStatus(
                    "Penyimpanan gagal: " + exception.getMessage()
                    + (dimuatUlang
                            ? ". Tampilan dimuat ulang dari database."
                            : ". Tampilan gagal dimuat ulang."),
                    MERAH);
            return;
        }

        if (!muatUlang()) {
            return;
        }
        kosongkanFormPenerimaan();
        setStatus("Penerimaan berhasil: " + kode + ", " + jumlah + " "
                + barang.getSatuan() + ".", HIJAU);
    }

    private void keluarkanStokManual() {
        BatchStok batch = batchTerpilih();
        if (batch == null) {
            setStatus("Pilih satu batch pada tabel terlebih dahulu.", MERAH);
            return;
        }

        int jumlah;
        try {
            jumlah = Integer.parseInt(fieldJumlahKeluar.getText().trim());
        } catch (NumberFormatException exception) {
            setStatus("Jumlah harus berupa angka bulat.", MERAH);
            return;
        }

        PengeluaranStok pengeluaran = new PengeluaranStok(LocalDate.now());
        try {
            pengeluaran.keluarkan(barang, batch, jumlah);
        } catch (StokTidakCukupException exception) {
            muatUlang();
            setStatus(exception.getMessage(), MERAH);
            return;
        } catch (IllegalArgumentException exception) {
            muatUlang();
            setStatus("Pengeluaran gagal: " + exception.getMessage(), MERAH);
            return;
        }

        try {
            penyimpanan.simpanTransaksi(barang, pengeluaran);
        } catch (PenyimpananException exception) {
            boolean dimuatUlang = muatUlang();
            setStatus(
                    "Penyimpanan gagal: " + exception.getMessage()
                    + (dimuatUlang
                            ? ". Tampilan dimuat ulang dari database."
                            : ". Tampilan gagal dimuat ulang."),
                    MERAH);
            return;
        }

        String kode = batch.getKode();
        if (!muatUlang()) {
            return;
        }
        fieldJumlahKeluar.setText("");
        setStatus("Pengeluaran berhasil: " + kode + ", " + jumlah + " "
                + barang.getSatuan() + ".", HIJAU);
    }

    private void keluarkanStokFefo() {
        int jumlah;
        try {
            jumlah = Integer.parseInt(fieldJumlahFefo.getText().trim());
        } catch (NumberFormatException exception) {
            setStatus("Jumlah harus berupa angka bulat.", MERAH);
            return;
        }

        LocalDate tanggalPengeluaran;
        try {
            tanggalPengeluaran = LocalDate.parse(fieldTanggalFefo.getText().trim());
        } catch (DateTimeParseException exception) {
            setStatus("Format tanggal tidak valid. Gunakan yyyy-MM-dd.", MERAH);
            return;
        }

        PengeluaranStok pengeluaran = new PengeluaranStok(tanggalPengeluaran);
        try {
            pengeluaran.keluarkanFefo(barang, jumlah, tanggalPengeluaran);
        } catch (StokTidakCukupException exception) {
            muatUlang();
            setStatus(exception.getMessage(), MERAH);
            return;
        } catch (IllegalArgumentException exception) {
            muatUlang();
            setStatus("Pengeluaran FEFO gagal: " + exception.getMessage(), MERAH);
            return;
        }

        try {
            penyimpanan.simpanTransaksi(barang, pengeluaran);
        } catch (PenyimpananException exception) {
            boolean dimuatUlang = muatUlang();
            setStatus(
                    "Penyimpanan gagal: " + exception.getMessage()
                    + (dimuatUlang
                            ? ". Tampilan dimuat ulang dari database."
                            : ". Tampilan gagal dimuat ulang."),
                    MERAH);
            return;
        }

        String alokasi = formatAlokasi(pengeluaran);
        if (!muatUlang()) {
            return;
        }
        fieldJumlahFefo.setText("");
        fieldTanggalFefo.setText(LocalDate.now().toString());
        setStatus("Pengeluaran FEFO berhasil: " + alokasi + ". Saldo: "
                + barang.hitungSaldo() + " " + barang.getSatuan() + ".", HIJAU);
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
                    .append(barang.getSatuan());
        }
        return hasil.toString();
    }

    private BatchStok batchTerpilih() {
        int baris = tabelBatch.getSelectedRow();
        if (baris < 0) {
            return null;
        }
        int barisModel = tabelBatch.convertRowIndexToModel(baris);
        return barang.getDaftarBatch().get(barisModel);
    }

    private void perbaruiBatchDipilih() {
        BatchStok batch = batchTerpilih();
        if (batch == null) {
            labelBatchDipilih.setText("Belum ada batch dipilih");
        } else {
            labelBatchDipilih.setText("Batch dipilih: " + batch.getKode()
                    + " | sisa " + batch.getSisaJumlah() + " " + barang.getSatuan());
        }
    }

    private boolean muatUlang() {
        BatchStok batchDipilih = batchTerpilih();
        String kodeDipilih = batchDipilih == null ? null : batchDipilih.getKode();
        try {
            Barang barangTerbaru = penyimpanan.muatBarang(barang.getId());
            List<Transaksi> riwayatTerbaru = penyimpanan.muatRiwayat(barangTerbaru);
            for (int i = 0; i < daftarBarang.size(); i++) {
                if (daftarBarang.get(i).getId() == barangTerbaru.getId()) {
                    daftarBarang.set(i, barangTerbaru);
                    break;
                }
            }
            barang = barangTerbaru;
            riwayat.clear();
            riwayat.addAll(riwayatTerbaru);
            perbaruiTampilan();
            if (kodeDipilih != null) {
                pilihBatch(kodeDipilih);
            }
            return true;
        } catch (PenyimpananException exception) {
            setStatus("Gagal memuat data: " + exception.getMessage(), MERAH);
            return false;
        }
    }

    private void pilihBatch(String kode) {
        for (int baris = 0; baris < modelBatch.getRowCount(); baris++) {
            if (kode.equals(modelBatch.getValueAt(baris, 0))) {
                tabelBatch.setRowSelectionInterval(baris, baris);
                return;
            }
        }
    }

    private void perbaruiTampilan() {
        labelNamaBarang.setText(barang.getNama());
        perbaruiComboBarang();
        modelBatch.setRowCount(0);
        for (BatchStok batch : barang.getDaftarBatch()) {
            modelBatch.addRow(new Object[]{
                batch.getKode(),
                batch.getSisaJumlah() + " " + barang.getSatuan(),
                batch.getTanggalMasuk().toString(),
                batch.getTanggalKedaluwarsa().toString()
            });
        }
        tabelBatch.clearSelection();
        labelSaldo.setText(barang.hitungSaldo() + " " + barang.getSatuan());
        labelJumlahBatch.setText(barang.jumlahBatch() + " batch");
        fieldStokMinimum.setText(String.valueOf(barang.getStokMinimum()));
        perbaruiBatchDipilih();
        perbaruiRiwayat();
        perbaruiPilihanBatchBuang();
        perbaruiPemantauan();
    }

    private void perbaruiPilihanBatchBuang() {
        String kodeDipilih = comboBatchBuang.getItemCount() == 0
                ? null
                : ((BatchStok) comboBatchBuang.getSelectedItem()).getKode();
        comboBatchBuang.removeAllItems();
        for (BatchStok batch : barang.getDaftarBatch()) {
            if (batch.getSisaJumlah() > 0) {
                comboBatchBuang.addItem(batch);
            }
        }
        if (kodeDipilih != null) {
            for (int i = 0; i < comboBatchBuang.getItemCount(); i++) {
                if (kodeDipilih.equals(
                        comboBatchBuang.getItemAt(i).getKode())) {
                    comboBatchBuang.setSelectedIndex(i);
                    break;
                }
            }
        }
        tombolBuang.setEnabled(comboBatchBuang.getItemCount() > 0);
    }

    private void perbaruiRiwayat() {
        modelRiwayat.setRowCount(0);
        for (Transaksi transaksi : riwayat) {
            String alasan = transaksi.getAlasan();
            for (RincianTransaksi rincian : transaksi.getRincian()) {
                modelRiwayat.addRow(new Object[]{
                    transaksi.getTanggal().toString(),
                    transaksi.getJenis(),
                    rincian.getBatch().getKode(),
                    rincian.getJumlah() + " " + barang.getSatuan(),
                    alasan == null ? "" : alasan
                });
            }
        }
    }

    private void kosongkanFormPenerimaan() {
        fieldKode.setText("");
        fieldJumlah.setText("");
        LocalDate hariIni = LocalDate.now();
        fieldTanggalMasuk.setText(hariIni.toString());
        fieldTanggalKedaluwarsa.setText(hariIni.plusMonths(1).toString());
        fieldTanggalFefo.setText(hariIni.toString());
        fieldKode.requestFocusInWindow();
    }

    private void setStatus(String pesan, Color warna) {
        areaStatus.setText(pesan);
        areaStatus.setForeground(warna);
    }
}
