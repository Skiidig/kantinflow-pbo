package kantinflow.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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
    private final JLabel labelStatus = new JLabel("-");
    private final DefaultTableModel model = Ui.model(
            "Kode Batch", "Sisa", "Tanggal Kedaluwarsa", "Status");
    private final JTable tabel = Ui.tabel(model);
    private final JLabel labelInfo = Ui.keterangan(" ");
    private final EmptyState kosong = new EmptyState(
            "alert-triangle", "Tidak ada batch bersisa",
            "Tidak ada batch dengan sisa stok pada tanggal acuan ini.");
    private final EmptyState kosongBarang = new EmptyState(
            "package", "Belum ada barang aktif",
            "Tambahkan barang melalui halaman Barang, lalu pilih barang "
            + "aktif untuk memantau kedaluwarsa.");
    private final JPanel hasil = new JPanel(new CardLayout());
    private final JLabel infoBarang = Ui.keterangan(" ");
    private int idBarangTerakhir = -1;

    public PanelPemantauan(KonteksKantin konteks) {
        super(konteks, "Pemantauan",
                "Periksa status kedaluwarsa batch aktif.");
        fieldTanggal.setName("tanggalPemantauan");
        infoBarang.setName("konteksPemantauan");
        tombolPeriksa.setName("tombolPeriksa");
        tabel.setName("tabelPemantauan");
        labelFisik.setName("fisikPemantauan");
        labelLayak.setName("layakPemantauan");
        labelMinimum.setName("minimumPemantauan");
        labelStatus.setName("statusPemantauan");
        labelInfo.setName("infoPemantauan");
        pesanValidasi.setName("pesanPemantauan");
        kosong.setName("kosongPemantauan");
        kosongBarang.setName("kosongBarangPemantauan");
        hasil.setName("hasilPemantauan");
        hasil.setOpaque(false);
        fieldTanggal.setText(LocalDate.now().toString());
        tombolPeriksa.addActionListener(event -> periksa());
        Ui.rataKanan(tabel, 1);
        Ui.pasangBadge(tabel, 3);
        Ui.aturKolom(tabel, 135, 75, 140, 165);

        bangunIsi();
    }

    private void bangunIsi() {
        JPanel isi = new JPanel(new BorderLayout(0, 16));
        isi.setOpaque(false);

        JPanel kontrol = Ui.kartu("Tanggal pemeriksaan", "monitor");
        JPanel baris = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        baris.setOpaque(false);
        baris.add(fieldTanggal);
        baris.add(tombolPeriksa);
        JPanel kontrolIsi = new JPanel(new BorderLayout(0, 5));
        kontrolIsi.setOpaque(false);
        JPanel kontrolAtas = new JPanel(new BorderLayout(0, 4));
        kontrolAtas.setOpaque(false);
        kontrolAtas.add(infoBarang, BorderLayout.NORTH);
        kontrolAtas.add(baris, BorderLayout.CENTER);
        kontrolIsi.add(kontrolAtas, BorderLayout.NORTH);
        kontrolIsi.add(pesanValidasi, BorderLayout.CENTER);
        kontrolIsi.add(labelInfo, BorderLayout.SOUTH);
        kontrol.add(kontrolIsi, BorderLayout.CENTER);

        JPanel nilai = Ui.kartu("Ringkasan stok", "chart-column");
        JPanel nilaiBaris = Ui.gridBertahap(4, 900, 520,
                nilaiKecil("Stok fisik", labelFisik, "Seluruh sisa batch"),
                nilaiKecil("Stok layak", labelLayak,
                        "Batch layak pada tanggal"),
                nilaiKecil("Stok minimum", labelMinimum,
                        "Batas kebutuhan aktif"),
                nilaiKecil("Status", labelStatus, "Perlu restok / Cukup"));
        nilai.add(nilaiBaris, BorderLayout.CENTER);

        JPanel daftar = Ui.kartu("Status Batch", "alert-triangle");
        JPanel daftarIsi = new JPanel(new BorderLayout(0, 4));
        daftarIsi.setOpaque(false);
        JScrollPane gulir = Ui.scrollTabel(tabel, Ui.TINGGI_TABEL_PEMANTAUAN);
        hasil.add(gulir, "tabel");
        hasil.add(kosong, "kosong");
        hasil.add(kosongBarang, "kosongBarang");
        daftarIsi.add(hasil, BorderLayout.CENTER);
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
        if (konteks.getBarangAktif() == null) {
            idBarangTerakhir = -1;
            fieldTanggal.setText(konteks.getTanggalAcuan().toString());
            infoBarang.setText("Pemantauan tanpa barang aktif.");
            model.setRowCount(0);
            labelFisik.setText("-");
            labelLayak.setText("-");
            labelMinimum.setText("-");
            labelStatus.setText("-");
            labelStatus.setForeground(Ui.TEKS_LEMBUT);
            labelInfo.setText("Pilih barang aktif melalui halaman Barang "
                    + "untuk mulai memantau kedaluwarsa.");
            pesanValidasi.setText(" ");
            tampilkanHasil("kosongBarang");
            return;
        }

        if (idBarangTerakhir != konteks.getBarangAktif().getId()) {
            idBarangTerakhir = konteks.getBarangAktif().getId();
            pesanValidasi.setText(" ");
        }
        infoBarang.setText("Pemantauan untuk "
                + konteks.getBarangAktif().getNama() + " · "
                + konteks.getBarangAktif().getSatuan());
        LocalDate tanggal = konteks.getTanggalAcuan();
        fieldTanggal.setText(tanggal.toString());
        labelInfo.setText("Diperiksa untuk tanggal " + tanggal
                + ". Hanya batch bersisa yang ditampilkan.");

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
        tampilkanHasil(model.getRowCount() == 0 ? "kosong" : "tabel");
    }

    private void tampilkanHasil(String kartu) {
        ((CardLayout) hasil.getLayout()).show(hasil, kartu);
    }
}
