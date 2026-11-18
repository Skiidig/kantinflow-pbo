package kantinflow.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import kantinflow.BatchStok;
import kantinflow.StatusKedaluwarsa;

/**
 * Halaman pemantauan kedaluwarsa batch bersisa.
 */
public class PanelPemantauan extends PanelKantin {
    private final JTextField fieldTanggal = Ui.field(11);
    private final JButton tombolPeriksa = Ui.sekunder("Periksa Kedaluwarsa");
    private final JLabel pesanValidasi = Ui.pesan();
    private final JLabel labelFisik = new JLabel("-");
    private final JLabel labelLayak = new JLabel("-");
    private final JLabel labelMinimum = new JLabel("-");
    private final JLabel labelStatus = new JLabel("Belum diperiksa");
    private final DefaultTableModel model = Ui.model(
            "Kode Batch", "Sisa", "Tanggal Kedaluwarsa", "Status");
    private final JTable tabel = Ui.tabel(model);
    private final JLabel labelInfo = Ui.keterangan(
            "Belum ada tanggal pemeriksaan.");
    private final JLabel kosong = Ui.kosong(
            "Belum ada batch bersisa yang dapat dipantau.");

    public PanelPemantauan(KonteksKantin konteks) {
        super(konteks, "Pemantauan",
                "Periksa status kedaluwarsa batch aktif.");
        fieldTanggal.setName("tanggalPemantauan");
        tombolPeriksa.setName("tombolPeriksa");
        tabel.setName("tabelPemantauan");
        tombolPeriksa.addActionListener(event -> periksa());
        Ui.rataKanan(tabel, 1);
        tabel.getColumnModel().getColumn(3).setCellRenderer(
                new DefaultTableCellRenderer() {
                    @Override
                    public java.awt.Component getTableCellRendererComponent(
                            JTable table, Object value, boolean isSelected,
                            boolean hasFocus, int row, int column) {
                        java.awt.Component c = super.getTableCellRendererComponent(
                                table, value, isSelected, hasFocus, row, column);
                        setHorizontalAlignment(SwingConstants.LEFT);
                        setBorder(new EmptyBorder(0, Ui.PADDING_SEL_TABEL, 0, Ui.PADDING_SEL_TABEL));
                        if (!isSelected) {
                            String status = value == null ? "" : value.toString();
                            if (StatusKedaluwarsa.KEDALUWARSA.getLabel().equals(status)) {
                                setForeground(Ui.MERAH);
                            } else if (StatusKedaluwarsa.SEGERA_KEDALUWARSA.getLabel()
                                    .equals(status)) {
                                setForeground(Ui.KUNING);
                            } else {
                                setForeground(Ui.HIJAU);
                            }
                        }
                        return c;
                    }
                });
        bangunIsi();
    }

    private void bangunIsi() {
        JPanel isi = new JPanel(new BorderLayout(0, 16));
        isi.setOpaque(false);

        JPanel kontrol = Ui.kartu("Tanggal pemeriksaan");
        JPanel baris = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        baris.setOpaque(false);
        baris.add(fieldTanggal);
        baris.add(tombolPeriksa);
        JPanel kontrolIsi = new JPanel(new BorderLayout(0, 5));
        kontrolIsi.setOpaque(false);
        kontrolIsi.add(baris, BorderLayout.NORTH);
        kontrolIsi.add(pesanValidasi, BorderLayout.CENTER);
        kontrolIsi.add(labelInfo, BorderLayout.SOUTH);
        kontrol.add(kontrolIsi, BorderLayout.CENTER);

        JPanel nilai = Ui.kartu("Ringkasan stok");
        JPanel nilaiBaris = new JPanel(new GridLayout(1, 4, 16, 0));
        nilaiBaris.setOpaque(false);
        nilaiBaris.add(nilaiKecil("Stok fisik", labelFisik,
                "Seluruh sisa batch"));
        nilaiBaris.add(nilaiKecil("Stok layak", labelLayak,
                "Batch layak pada tanggal"));
        nilaiBaris.add(nilaiKecil("Stok minimum", labelMinimum,
                "Batas kebutuhan aktif"));
        nilaiBaris.add(nilaiKecil("Status", labelStatus,
                "Perlu restok / Cukup"));
        nilai.add(nilaiBaris, BorderLayout.CENTER);

        JPanel daftar = Ui.kartu("Status Batch");
        JPanel daftarIsi = new JPanel(new BorderLayout(0, 4));
        daftarIsi.setOpaque(false);
        daftarIsi.add(Ui.scrollTabel(tabel, Ui.TINGGI_TABEL_PEMANTAUAN),
                BorderLayout.CENTER);
        daftarIsi.add(kosong, BorderLayout.SOUTH);
        daftar.add(daftarIsi, BorderLayout.CENTER);

        JPanel atas = new JPanel(new BorderLayout(0, 12));
        atas.setOpaque(false);
        atas.add(kontrol, BorderLayout.NORTH);
        atas.add(nilai, BorderLayout.CENTER);

        isi.add(atas, BorderLayout.NORTH);
        isi.add(daftar, BorderLayout.CENTER);
        add(isi, BorderLayout.CENTER);
    }

    private JPanel nilaiKecil(String judul, JLabel nilai, String detail) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new javax.swing.BoxLayout(
                panel, javax.swing.BoxLayout.Y_AXIS));
        JLabel label = Ui.keterangan(judul);
        label.setFont(Ui.font(Font.PLAIN, Ui.FONT_KECIL));
        nilai.setFont(Ui.font(Font.BOLD, Ui.FONT_NILAI));
        nilai.setForeground(Ui.TEKS);
        JLabel catatan = Ui.keterangan(detail);
        catatan.setFont(Ui.font(Font.PLAIN, Ui.FONT_KECIL));
        panel.add(label);
        panel.add(javax.swing.Box.createVerticalStrut(3));
        panel.add(nilai);
        panel.add(javax.swing.Box.createVerticalStrut(2));
        panel.add(catatan);
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Ui.GARIS));
        return panel;
    }

    private void periksa() {
        try {
            LocalDate tanggal = LocalDate.parse(fieldTanggal.getText().trim());
            pesanValidasi.setText(" ");
            konteks.setTanggalPemantauan(tanggal);
            konteks.tampilkanPesan("Pemantauan " + tanggal
                    + " selesai: " + model.getRowCount()
                    + " batch bersisa diperiksa.", true);
        } catch (DateTimeParseException exception) {
            pesanValidasi.setText("Format tanggal harus yyyy-MM-dd.");
            fieldTanggal.requestFocusInWindow();
            konteks.tampilkanPesan("Format tanggal tidak valid. Gunakan yyyy-MM-dd.",
                    false);
        }
    }

    @Override
    public void perbarui() {
        LocalDate tanggal = konteks.getTanggalPemantauan();
        if (tanggal == null) {
            if (fieldTanggal.getText().isBlank()) {
                fieldTanggal.setText(LocalDate.now().toString());
            }
            tanggal = LocalDate.now();
            model.setRowCount(0);
            labelInfo.setText("Belum diperiksa. Isi tanggal lalu klik Periksa.");
        } else {
            fieldTanggal.setText(tanggal.toString());
            model.setRowCount(0);
            labelInfo.setText("Diperiksa pada " + tanggal
                    + ". Hanya batch bersisa yang ditampilkan.");
        }
        int fisik = konteks.getBarangAktif().hitungSaldo();
        int layak = konteks.getBarangAktif().hitungStokLayak(tanggal);
        int minimum = konteks.getBarangAktif().getStokMinimum();
        String satuan = konteks.getBarangAktif().getSatuan();
        labelFisik.setText(fisik + " " + satuan);
        labelLayak.setText(layak + " " + satuan);
        labelMinimum.setText(minimum + " " + satuan);
        boolean restok = layak <= minimum;
        labelStatus.setText(restok ? "Perlu restok" : "Cukup");
        labelStatus.setForeground(restok ? Ui.MERAH : Ui.HIJAU);

        if (konteks.getTanggalPemantauan() != null) {
            model.setRowCount(0);
            for (BatchStok batch : konteks.getBarangAktif().getDaftarBatch()) {
                if (batch.getSisaJumlah() <= 0) {
                    continue;
                }
                StatusKedaluwarsa status = batch.statusPada(tanggal);
                model.addRow(new Object[]{
                    batch.getKode(),
                    batch.getSisaJumlah() + " " + satuan,
                    batch.getTanggalKedaluwarsa().toString(),
                    status.getLabel()
                });
            }
        }
        kosong.setVisible(model.getRowCount() == 0);
    }
}
