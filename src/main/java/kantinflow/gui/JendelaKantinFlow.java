package kantinflow.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import kantinflow.Barang;
import kantinflow.BatchStok;
import kantinflow.PenerimaanStok;
import kantinflow.penyimpanan.PenyimpananException;
import kantinflow.penyimpanan.PenyimpananSQLite;

public class JendelaKantinFlow {
    private static final Color BIRU = new Color(30, 42, 85);
    private static final Color TEAL = new Color(0, 131, 143);
    private static final Color ABU = new Color(89, 89, 89);
    private static final Color LATAR = new Color(245, 247, 251);
    private static final Color HIJAU = new Color(30, 123, 77);
    private static final Color MERAH = new Color(176, 0, 32);

    private final PenyimpananSQLite penyimpanan;
    private final JFrame frame;
    private final DefaultTableModel modelBatch;
    private final JTable tabelBatch;
    private final JTextField fieldKode = new JTextField(16);
    private final JTextField fieldJumlah = new JTextField(16);
    private final JTextField fieldTanggalMasuk = new JTextField(16);
    private final JTextField fieldTanggalKedaluwarsa = new JTextField(16);
    private final JLabel labelSaldo = new JLabel("0 kotak");
    private final JLabel labelJumlahBatch = new JLabel("0 batch");
    private final JTextArea areaStatus = new JTextArea(2, 40);
    private final JButton tombolSimpan = new JButton("Simpan Penerimaan");
    private final JButton tombolMuatUlang = new JButton("Muat Ulang");
    private Barang barang;
    private boolean sudahDitutup;

    public JendelaKantinFlow(PenyimpananSQLite penyimpanan) {
        if (penyimpanan == null) {
            throw new IllegalArgumentException("Penyimpanan wajib diisi");
        }
        this.penyimpanan = penyimpanan;
        this.barang = penyimpanan.muatBarang();
        this.frame = new JFrame("KantinFlow - Penerimaan Stok");
        this.modelBatch = new DefaultTableModel(
                new Object[]{"Kode Batch", "Sisa", "Tanggal Masuk", "Tanggal Kedaluwarsa"},
                0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.tabelBatch = new JTable(modelBatch);

        bangunTampilan();
        aturNilaiAwal();
        perbaruiTampilan();
        setStatus("Data dimuat dari database. Siap menerima stok.", ABU);
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
        JLabel namaBarang = new JLabel(barang.getNama());
        namaBarang.setFont(new Font("Segoe UI", Font.BOLD, 24));
        namaBarang.setForeground(Color.WHITE);
        JLabel keterangan = new JLabel("Stok fisik dan batch tersimpan");
        keterangan.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        keterangan.setForeground(new Color(200, 212, 235));
        identitas.add(namaBarang);
        identitas.add(keterangan);

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

        tabelBatch.setName("tabelBatch");
        tabelBatch.setRowHeight(28);
        tabelBatch.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tabelBatch.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabelBatch.getTableHeader().setDefaultRenderer(new javax.swing.table.DefaultTableCellRenderer() {
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
        tabelBatch.setSelectionBackground(new Color(210, 235, 235));
        tabelBatch.setGridColor(new Color(220, 227, 238));
        tabelBatch.setFillsViewportHeight(true);
        tabelBatch.getColumnModel().getColumn(0).setPreferredWidth(140);
        tabelBatch.getColumnModel().getColumn(1).setPreferredWidth(70);
        tabelBatch.getColumnModel().getColumn(2).setPreferredWidth(130);
        tabelBatch.getColumnModel().getColumn(3).setPreferredWidth(150);

        JScrollPane scrollBatch = new JScrollPane(tabelBatch);
        scrollBatch.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(197, 208, 222)),
                "Daftar Batch"));
        scrollBatch.getViewport().setBackground(Color.WHITE);

        JPanel isi = new JPanel(new BorderLayout(14, 0));
        isi.setOpaque(false);
        isi.add(scrollBatch, BorderLayout.CENTER);
        isi.add(buatFormPenerimaan(), BorderLayout.EAST);

        areaStatus.setName("status");
        areaStatus.setEditable(false);
        areaStatus.setLineWrap(true);
        areaStatus.setWrapStyleWord(true);
        areaStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        areaStatus.setBorder(new EmptyBorder(9, 12, 9, 12));
        areaStatus.setBackground(new Color(238, 243, 250));
        areaStatus.setOpaque(true);

        root.add(header, BorderLayout.NORTH);
        root.add(isi, BorderLayout.CENTER);
        root.add(areaStatus, BorderLayout.SOUTH);

        frame.setContentPane(root);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                tutup();
            }
        });
        frame.setMinimumSize(new Dimension(900, 560));
        frame.setSize(1040, 640);
        frame.setLocationRelativeTo(null);
        frame.getRootPane().setDefaultButton(tombolSimpan);
    }

    private JPanel buatFormPenerimaan() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setPreferredSize(new Dimension(325, 0));
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(new Color(197, 208, 222)),
                        "Form Penerimaan"),
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
                setStatus("Data dimuat ulang dari database.", ABU);
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

    private void tambahField(
            JPanel form,
            GridBagConstraints label,
            GridBagConstraints input,
            String teks,
            JTextField field,
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
            penyimpanan.simpanTransaksi(penerimaan);
        } catch (PenyimpananException exception) {
            boolean dimuatUlang = muatUlang();
            setStatus(
                    "Penyimpanan gagal: " + exception.getMessage()
                    + (dimuatUlang
                            ? " Tampilan dimuat ulang dari database."
                            : " Tampilan gagal dimuat ulang."),
                    MERAH);
            return;
        }

        if (!muatUlang()) {
            return;
        }
        kosongkanForm();
        setStatus("Penerimaan berhasil: " + kode + ", " + jumlah + " "
                + barang.getSatuan() + ".", HIJAU);
    }

    private boolean muatUlang() {
        try {
            barang = penyimpanan.muatBarang();
            perbaruiTampilan();
            return true;
        } catch (PenyimpananException exception) {
            setStatus("Gagal memuat data: " + exception.getMessage(), MERAH);
            return false;
        }
    }

    private void perbaruiTampilan() {
        modelBatch.setRowCount(0);
        for (BatchStok batch : barang.getDaftarBatch()) {
            modelBatch.addRow(new Object[]{
                batch.getKode(),
                batch.getSisaJumlah() + " " + barang.getSatuan(),
                batch.getTanggalMasuk().toString(),
                batch.getTanggalKedaluwarsa().toString()
            });
        }
        labelSaldo.setText(barang.hitungSaldo() + " " + barang.getSatuan());
        labelJumlahBatch.setText(barang.jumlahBatch() + " batch");
    }

    private void kosongkanForm() {
        fieldKode.setText("");
        fieldJumlah.setText("");
        LocalDate hariIni = LocalDate.now();
        fieldTanggalMasuk.setText(hariIni.toString());
        fieldTanggalKedaluwarsa.setText(hariIni.plusMonths(1).toString());
        fieldKode.requestFocusInWindow();
    }

    private void setStatus(String pesan, Color warna) {
        areaStatus.setText(pesan);
        areaStatus.setForeground(warna);
    }
}
