package kantinflow.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.time.LocalDate;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import kantinflow.BatchStok;
import kantinflow.StatusKedaluwarsa;

/**
 * Halaman pemantauan kedaluwarsa batch bersisa.
 */
public class PanelPemantauan extends PanelBarangTerpilih {
    private final PemilihTanggal tanggalPilihan = new PemilihTanggal();
    private final JButton tombolPeriksa = Ui.utama("Periksa");
    private final JLabel pesanValidasi = Ui.pesan();
    private final JLabel pesanTanggal = Ui.pesan();
    private LocalDate tanggalTerakhir;
    private LocalDate tanggalKonteksTerakhir;
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
        tanggalPilihan.komponenTeks().setName("tanggalPemantauan");
        pesanTanggal.setName("pesanTanggalPemantauan");
        tanggalTerakhir = konteks.getTanggalAcuan();
        tanggalKonteksTerakhir = tanggalTerakhir;
        tanggalPilihan.setTanggal(tanggalTerakhir);
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
                Ui.field("Tanggal pemeriksaan *", tanggalPilihan, pesanTanggal),
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
        if (tanggalPilihan.ambilTeksTidakValid()) {
            pesanTanggal.setText("Tanggal tidak valid. Pilih tanggal yang benar.");
            tanggalPilihan.komponenTeks().requestFocusInWindow();
            return;
        }
        LocalDate dipilih = tanggalPilihan.getTanggal();
        if (dipilih == null) {
            pesanTanggal.setText("Tanggal pemeriksaan wajib diisi.");
            tanggalPilihan.komponenTeks().requestFocusInWindow();
            return;
        }
        if (!tanggalPilihan.isTextFieldValid()) {
            pesanTanggal.setText("Tanggal tidak valid. Pilih tanggal yang benar.");
            tanggalPilihan.komponenTeks().requestFocusInWindow();
            return;
        }
        pesanTanggal.setText(" ");
        pesanValidasi.setText(" ");
        tanggalTerakhir = dipilih;
        tanggalKonteksTerakhir = dipilih;
        konteks.setTanggalPemantauan(dipilih);
        konteks.tampilkanPesan("Pemantauan "
                + PemilihTanggal.format(dipilih) + " selesai: "
                + model.getRowCount() + " batch bersisa diperiksa.", true);
    }

    @Override
    protected void saatBarangBerubah() {
        pesanValidasi.setText(" ");
    }

    @Override
    public void perbarui() {
        boolean adaBarang = perbaruiBarangTerpilih();
        LocalDate dariKonteks = konteks.getTanggalAcuan();
        LocalDate dipilih = tanggalPilihan.getTanggal();
        if (dipilih != null && dipilih.equals(tanggalKonteksTerakhir)
                && !dipilih.equals(dariKonteks)) {
            tanggalPilihan.setTanggal(dariKonteks);
            dipilih = dariKonteks;
        }
        tanggalKonteksTerakhir = dariKonteks;
        if (dipilih != null) {
            tanggalTerakhir = dipilih;
        }
        LocalDate acuan = tanggalTerakhir;

        if (!adaBarang) {
            model.setRowCount(0);
            labelInfo.setText("Tambahkan barang melalui halaman Barang "
                    + "untuk mulai memantau kedaluwarsa.");
            pesanValidasi.setText(" ");
            tombolPeriksa.setEnabled(false);
            tampilkanHasil("kosongBarang");
            return;
        }

        tombolPeriksa.setEnabled(true);
        labelInfo.setText("Pemeriksaan terakhir untuk tanggal "
                + PemilihTanggal.format(acuan)
                + ". Hanya batch bersisa yang ditampilkan.");

        String satuan = barang.getSatuan();
        model.setRowCount(0);
        for (BatchStok batch : barang.getDaftarBatch()) {
            if (batch.getSisaJumlah() <= 0) {
                continue;
            }
            StatusKedaluwarsa status = batch.statusPada(acuan);
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
