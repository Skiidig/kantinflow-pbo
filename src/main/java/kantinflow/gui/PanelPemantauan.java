package kantinflow.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import kantinflow.BatchStok;
import kantinflow.StatusKedaluwarsa;

/**
 * Halaman pemantauan kedaluwarsa batch bersisa.
 */
public class PanelPemantauan extends PanelBarangTerpilih {
    private final JTextField fieldTanggal = Ui.field(11);
    private final JButton tombolPeriksa = Ui.utama("Periksa");
    private final JLabel pesanValidasi = Ui.pesan();
    private final DefaultTableModel model = Ui.model(
            "Kode Batch", "Sisa", "Tanggal Kedaluwarsa", "Status");
    private final JTable tabel = Ui.tabel(model);
    private final JLabel labelInfo = Ui.keterangan(" ");
    private final EmptyState kosong = new EmptyState(
            "alert-triangle", "Tidak ada batch bersisa",
            "Tidak ada batch dengan sisa stok pada tanggal acuan ini.");
    private final EmptyState kosongBarang = new EmptyState(
            "package", "Belum ada barang",
            "Tambahkan barang melalui halaman Barang, lalu pilih barang "
            + "pada kartu tanggal pemeriksaan.");
    private final JPanel hasil = new JPanel(new CardLayout());

    public PanelPemantauan(KonteksKantin konteks) {
        super(konteks, "Pemantauan",
                "Periksa status kedaluwarsa batch barang terpilih.");
        pilihanBarang.setName("barangPemantauan");
        fieldTanggal.setName("tanggalPemantauan");
        fieldTanggal.putClientProperty("JTextField.placeholderText",
                "yyyy-MM-dd");
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
        fieldTanggal.setText(LocalDate.now().toString());
        tombolPeriksa.addActionListener(event -> periksa());
        Ui.rataKanan(tabel, 1);
        Ui.pasangTeksStatus(tabel, 3);
        Ui.aturKolom(tabel, 135, 75, 140, 165);

        bangunIsi();
    }

    private void bangunIsi() {
        JPanel isi = new JPanel(new BorderLayout(0, 16));
        isi.setOpaque(false);

        JPanel kontrol = Ui.kartu("Filter pemantauan", "monitor");
        JPanel kontrolIsi = new JPanel(new BorderLayout(0, 8));
        kontrolIsi.setOpaque(false);
        JPanel baris = Ui.barisFilter(
                Ui.field("Barang *", pilihanBarang),
                Ui.field("Tanggal pemeriksaan (yyyy-MM-dd)", fieldTanggal),
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

    private void periksa() {
        if (barang == null) {
            pesanValidasi.setText(
                    "Belum ada barang. Tambahkan melalui halaman Barang.");
            return;
        }
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
    protected void saatBarangBerubah() {
        pesanValidasi.setText(" ");
    }

    @Override
    public void perbarui() {
        if (!perbaruiBarangTerpilih()) {
            fieldTanggal.setText(konteks.getTanggalAcuan().toString());
            model.setRowCount(0);
            labelInfo.setText("Tambahkan barang melalui halaman Barang "
                    + "untuk mulai memantau kedaluwarsa.");
            pesanValidasi.setText(" ");
            tombolPeriksa.setEnabled(false);
            tampilkanHasil("kosongBarang");
            return;
        }

        tombolPeriksa.setEnabled(true);
        LocalDate tanggal = konteks.getTanggalAcuan();
        fieldTanggal.setText(tanggal.toString());
        labelInfo.setText("Pemeriksaan terakhir untuk tanggal " + tanggal
                + ". Hanya batch bersisa yang ditampilkan.");

        String satuan = barang.getSatuan();
        model.setRowCount(0);
        for (BatchStok batch : barang.getDaftarBatch()) {
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
