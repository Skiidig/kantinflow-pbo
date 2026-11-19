package kantinflow.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;

import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.XChartPanel;

import kantinflow.Barang;
import kantinflow.BatchStok;
import kantinflow.StatusKedaluwarsa;

/**
 * Dashboard ringkasan stok: statistik, peringatan kedaluwarsa, tabel barang,
 * dan grafik stok fisik.
 */
public class PanelRingkasan extends PanelKantin {
    private final JTextField fieldTanggal = Ui.field(11);
    private final JButton tombolPeriksa = Ui.utama("Periksa");
    private final JLabel pesanValidasi = Ui.pesan();

    private final StatCard kartuFisik = new StatCard(
            "Stok fisik", "boxes", Ui.AKSEN);
    private final StatCard kartuLayak = new StatCard(
            "Stok layak", "circle-check", Ui.HIJAU);
    private final StatCard kartuMinimum = new StatCard(
            "Stok minimum", "wallet", Ui.KUNING);
    private final StatCard kartuStatus = new StatCard(
            "Status kebutuhan", "alert-triangle", Ui.MERAH);

    private final DefaultTabel model = new DefaultTabel(
            "Barang", "Satuan", "Stok fisik", "Stok minimum", "Status");
    private final JTable tabel = Ui.tabel(model);
    private final JPanel daftarPeringatan = new JPanel();
    private final JPanel grafik = new JPanel(new BorderLayout());
    private final EmptyState kosongBarang = new EmptyState(
            "boxes", "Belum ada barang",
            "Tambahkan barang terlebih dahulu melalui halaman Barang.");
    private final EmptyState kosongPeringatan = new EmptyState(
            "circle-check", "Tidak ada peringatan",
            "Semua batch bersisa masih aman pada tanggal acuan.");
    private final JLabel ringkasanPeringatan = Ui.keterangan(" ");

    public PanelRingkasan(KonteksKantin konteks) {
        super(konteks, "Ringkasan",
                "Pantau stok, peringatan kedaluwarsa, dan kebutuhan restok.");
        fieldTanggal.setName("tanggalRingkasan");
        tombolPeriksa.setName("periksaRingkasan");
        tombolPeriksa.setIcon(Ui.ikonAksi("monitor", 16, java.awt.Color.WHITE));
        tombolPeriksa.setIconTextGap(7);
        kartuFisik.getNilai().setName("ringkasanFisik");
        kartuLayak.getNilai().setName("ringkasanLayak");
        kartuMinimum.getNilai().setName("ringkasanMinimum");
        kartuStatus.getNilai().setName("ringkasanStatus");
        tabel.setName("tabelRingkasan");
        tombolPeriksa.addActionListener(event -> periksa());
        Ui.rataKanan(tabel, 2, 3);
        tabel.getColumnModel().getColumn(4)
                .setCellRenderer(new PenyajiStatus());
        Ui.aturKolom(tabel, 230, 85, 110, 105, 110);
        bangunIsi();
    }

    @Override
    public int lebarMaksimum() {
        return Ui.LEBAR_KONTEN_RINGKASAN;
    }

    private void bangunIsi() {
        JPanel isi = new JPanel(new BorderLayout(0, 16));
        isi.setOpaque(false);

        JPanel kontrol = Ui.kartu("Tanggal acuan stok", "monitor");
        JPanel baris = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        baris.setOpaque(false);
        fieldTanggal.setColumns(10);
        baris.add(fieldTanggal);
        baris.add(tombolPeriksa);
        JPanel kontrolIsi = new JPanel(new BorderLayout(0, 6));
        kontrolIsi.setOpaque(false);
        kontrolIsi.add(baris, BorderLayout.NORTH);
        JLabel bantuan = Ui.keterangan(
                "Stok layak menghitung batch bersisa yang belum kedaluwarsa "
                + "pada tanggal ini.");
        kontrolIsi.add(bantuan, BorderLayout.CENTER);
        kontrolIsi.add(pesanValidasi, BorderLayout.SOUTH);
        kontrol.add(kontrolIsi, BorderLayout.CENTER);

        JPanel nilai = new JPanel(new java.awt.GridLayout(2, 2, 16, 16));
        nilai.setOpaque(false);
        nilai.add(kartuFisik);
        nilai.add(kartuLayak);
        nilai.add(kartuMinimum);
        nilai.add(kartuStatus);

        JPanel peringatan = Ui.kartu("Peringatan kedaluwarsa", "alert-triangle");
        JPanel peringatanIsi = new JPanel(new BorderLayout(0, 8));
        peringatanIsi.setOpaque(false);
        peringatanIsi.add(ringkasanPeringatan, BorderLayout.NORTH);
        daftarPeringatan.setOpaque(false);
        daftarPeringatan.setLayout(new BoxLayout(
                daftarPeringatan, BoxLayout.Y_AXIS));
        peringatanIsi.add(daftarPeringatan, BorderLayout.CENTER);
        peringatan.add(peringatanIsi, BorderLayout.CENTER);

        JPanel grafikKartu = Ui.kartu("Stok fisik per barang", "chart-column");
        grafik.setOpaque(false);
        grafikKartu.add(grafik, BorderLayout.CENTER);

        JPanel tengah = Ui.gridResponsif(2, 1050, peringatan, grafikKartu);

        JPanel semua = Ui.kartu("Semua barang", "package");
        JPanel semuaIsi = new JPanel(new BorderLayout(0, 6));
        semuaIsi.setOpaque(false);
        semuaIsi.add(Ui.scrollTabel(tabel, 260), BorderLayout.CENTER);
        semuaIsi.add(kosongBarang, BorderLayout.SOUTH);
        semua.add(semuaIsi, BorderLayout.CENTER);

        JPanel atas = new JPanel(new BorderLayout(0, 12));
        atas.setOpaque(false);
        atas.add(nilai, BorderLayout.NORTH);
        atas.add(tengah, BorderLayout.CENTER);
        isi.add(kontrol, BorderLayout.NORTH);
        isi.add(atas, BorderLayout.CENTER);
        isi.add(semua, BorderLayout.SOUTH);
        add(isi, BorderLayout.CENTER);
    }

    private void periksa() {
        try {
            LocalDate tanggal = LocalDate.parse(fieldTanggal.getText().trim());
            pesanValidasi.setText(" ");
            konteks.setTanggalPemantauan(tanggal);
            konteks.tampilkanPesan("Ringkasan diperiksa untuk tanggal "
                    + tanggal + ".", true);
        } catch (DateTimeParseException exception) {
            pesanValidasi.setText("Format tanggal harus yyyy-MM-dd.");
            fieldTanggal.requestFocusInWindow();
            konteks.tampilkanPesan("Format tanggal tidak valid. Gunakan yyyy-MM-dd.",
                    false);
        }
    }

    @Override
    public void perbarui() {
        Barang aktif = konteks.getBarangAktif();
        if (aktif == null) {
            return;
        }
        LocalDate tanggal = konteks.getTanggalAcuan();
        fieldTanggal.setText(tanggal.toString());

        int fisik = aktif.hitungSaldo();
        int layak = aktif.hitungStokLayak(tanggal);
        int minimum = aktif.getStokMinimum();
        String satuan = aktif.getSatuan();
        boolean perluRestok = layak <= minimum;

        kartuFisik.setNilai(fisik + " " + satuan);
        kartuFisik.setKeterangan("Seluruh sisa batch aktif");
        kartuLayak.setNilai(layak + " " + satuan);
        kartuLayak.setKeterangan("Batch layak pada tanggal acuan");
        kartuMinimum.setNilai(minimum + " " + satuan);
        kartuMinimum.setKeterangan("Batas kebutuhan barang aktif");
        kartuStatus.setNilai(perluRestok ? "Perlu restok" : "Cukup");
        kartuStatus.setKeterangan(perluRestok
                ? "Stok layak sudah mencapai batas"
                : "Stok layak masih di atas batas");
        kartuStatus.setAksen(perluRestok ? Ui.MERAH : Ui.HIJAU);

        perbaruiTabel(tanggal);
        perbaruiPeringatan(tanggal);
        perbaruiGrafik();
    }

    private void perbaruiTabel(LocalDate tanggal) {
        model.setRowCount(0);
        for (Barang barang : konteks.getDaftarBarang()) {
            int layak = barang.hitungStokLayak(tanggal);
            boolean restok = layak <= barang.getStokMinimum();
            model.addRow(new Object[]{
                barang.getNama(),
                barang.getSatuan(),
                barang.hitungSaldo() + " " + barang.getSatuan(),
                barang.getStokMinimum() + " " + barang.getSatuan(),
                restok ? "Perlu restok" : "Cukup"
            });
        }
        boolean kosong = model.getRowCount() == 0;
        kosongBarang.setVisible(kosong);
        tabel.setVisible(!kosong);
    }

    private void perbaruiPeringatan(LocalDate tanggal) {
        daftarPeringatan.removeAll();
        int jumlah = 0;
        for (Barang barang : konteks.getDaftarBarang()) {
            for (BatchStok batch : barang.getDaftarBatch()) {
                if (batch.getSisaJumlah() <= 0) {
                    continue;
                }
                StatusKedaluwarsa status = batch.statusPada(tanggal);
                if (status == StatusKedaluwarsa.AMAN) {
                    continue;
                }
                jumlah++;
                daftarPeringatan.add(barisPeringatan(
                        barang, batch, status));
                daftarPeringatan.add(Box.createVerticalStrut(6));
            }
        }
        if (jumlah == 0) {
            daftarPeringatan.add(kosongPeringatan);
            ringkasanPeringatan.setText("Tidak ada batch yang perlu perhatian.");
        } else {
            ringkasanPeringatan.setText(jumlah
                    + " batch perlu perhatian pada tanggal acuan.");
        }
        daftarPeringatan.revalidate();
        daftarPeringatan.repaint();
    }

    private JPanel barisPeringatan(Barang barang, BatchStok batch,
            StatusKedaluwarsa status) {
        JPanel baris = new JPanel(new BorderLayout(8, 0));
        baris.setOpaque(true);
        baris.setBackground(Ui.NETRAL_LEMBUT);
        baris.setBorder(new EmptyBorder(Ui.skala(8), Ui.skala(10),
                Ui.skala(8), Ui.skala(10)));
        JLabel teks = new JLabel(barang.getNama() + " · " + batch.getKode()
                + " · sisa " + batch.getSisaJumlah() + " "
                + barang.getSatuan()
                + " · " + batch.getTanggalKedaluwarsa());
        teks.setFont(Ui.font(Font.PLAIN, Ui.FONT_KECIL));
        teks.setForeground(Ui.KARTU.getRed() < 100
                ? new Color(0xE2E8F0) : Ui.TEKS);
        JPanel kanan = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        kanan.setOpaque(false);
        kanan.add(Badge.status(status.getLabel()));
        baris.add(teks, BorderLayout.CENTER);
        baris.add(kanan, BorderLayout.EAST);
        return baris;
    }

    private void perbaruiGrafik() {
        grafik.removeAll();
        List<Barang> daftar = konteks.getDaftarBarang();
        if (daftar.isEmpty()) {
            grafik.add(new EmptyState("chart-column", "Belum ada data",
                    "Grafik muncul setelah barang ditambahkan."),
                    BorderLayout.CENTER);
        } else {
            List<String> nama = new ArrayList<>();
            List<Integer> nilai = new ArrayList<>();
            for (Barang barang : daftar) {
                nama.add(barang.getNama());
                nilai.add(barang.hitungSaldo());
            }
            CategoryChart chart = new CategoryChartBuilder()
                    .width(460)
                    .height(230)
                    .yAxisTitle("Stok fisik")
                    .build();
            chart.addSeries("Stok", nama, nilai);
            chart.getStyler().setLegendVisible(false);
            chart.getStyler().setChartBackgroundColor(Ui.KARTU);
            chart.getStyler().setPlotBackgroundColor(Ui.KARTU);
            chart.getStyler().setPlotBorderVisible(false);
            chart.getStyler().setPlotGridVerticalLinesVisible(false);
            chart.getStyler().setPlotGridLinesColor(Ui.GARIS);
            chart.getStyler().setAxisTickLabelsColor(Ui.TEKS_LEMBUT);
            chart.getStyler().setAxisTickLabelsFont(
                    Ui.font(Font.PLAIN, Ui.FONT_KECIL));
            chart.getStyler().setAxisTitleFont(
                    Ui.font(Font.PLAIN, Ui.FONT_KECIL));
            chart.getStyler().setChartFontColor(Ui.TEKS_LEMBUT);
            chart.getStyler().setAvailableSpaceFill(0.55);
            chart.getStyler().setOverlapped(false);
            chart.getStyler().setSeriesColors(new Color[]{
                Ui.AKSEN, Ui.HIJAU, Ui.KUNING, Ui.ORANYE
            });
            grafik.add(new XChartPanel<>(chart), BorderLayout.CENTER);
        }
        grafik.revalidate();
        grafik.repaint();
    }

    private static final class PenyajiStatus extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tabel,
                Object nilai, boolean dipilih, boolean fokus, int baris,
                int kolom) {
            super.getTableCellRendererComponent(
                    tabel, nilai, dipilih, fokus, baris, kolom);
            setOpaque(true);
            setBorder(new EmptyBorder(0, Ui.skala(Ui.PADDING_SEL_TABEL),
                    0, Ui.skala(Ui.PADDING_SEL_TABEL)));
            setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
            setVerticalAlignment(javax.swing.SwingConstants.CENTER);
            setFont(tabel.getFont().deriveFont(Font.BOLD));
            int hover = tabel instanceof TabelKantin
                    ? ((TabelKantin) tabel).getBarisHover() : -1;
            if (dipilih) {
                setForeground(tabel.getSelectionForeground());
                setBackground(tabel.getSelectionBackground());
            } else {
                setForeground("Perlu restok".equals(String.valueOf(nilai))
                        ? Ui.MERAH : Ui.HIJAU);
                setBackground(baris == hover ? Ui.HOVER : Ui.KARTU);
            }
            return this;
        }
    }

    private static final class DefaultTabel
            extends javax.swing.table.DefaultTableModel {
        private DefaultTabel(String... kolom) {
            super(kolom, 0);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }
}
