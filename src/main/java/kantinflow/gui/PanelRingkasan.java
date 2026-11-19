package kantinflow.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

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
    private final JComboBox<PilihanBarang> pilihanBarang = Ui.combo();
    private Barang barang;
    private int idBarang = -1;
    private boolean memperbaruiPilihan;
    private final PemilihTanggal tanggalPilihan = new PemilihTanggal();
    private final JButton tombolPeriksa = Ui.utama("Periksa");
    private final JLabel pesanTanggal = Ui.pesan();
    private LocalDate tanggalTerakhir;
    private LocalDate tanggalKonteksTerakhir;

    private static final String KARTU_SEMUA = "semua";
    private static final String KARTU_SATU = "satuan";

    private final StatCard kartuFisik = new StatCard(
            "Stok fisik", "boxes", Ui.AKSEN);
    private final StatCard kartuLayak = new StatCard(
            "Stok layak", "circle-check", Ui.TEKS_LEMBUT);
    private final StatCard kartuMinimum = new StatCard(
            "Stok minimum", "wallet", Ui.TEKS_LEMBUT);
    private final StatCard kartuStatus = new StatCard(
            "Status", "alert-triangle", Ui.TEKS_LEMBUT);
    private final StatCard kartuBarang = new StatCard(
            "Barang", "boxes", Ui.AKSEN);
    private final StatCard kartuPerluRestok = new StatCard(
            "Perlu restok", "wallet", Ui.TEKS_LEMBUT);
    private final StatCard kartuSegera = new StatCard(
            "Segera kedaluwarsa", "flame", Ui.TEKS_LEMBUT);
    private final StatCard kartuKedaluwarsa = new StatCard(
            "Kedaluwarsa", "alert-triangle", Ui.TEKS_LEMBUT);
    private final JPanel nilai = new JPanel(new CardLayout());
    private JPanel panelKartuSemua;
    private JPanel panelKartuSatuan;
    private JScrollPane gulirTabel;

    private final DefaultTabel model = new DefaultTabel(
            "Barang", "Satuan", "Stok fisik", "Stok minimum", "Status");
    private final JTable tabel = Ui.tabel(model);
    private final DefaultTabel modelPeringatan = new DefaultTabel(
            "Barang / Batch", "Sisa", "Kedaluwarsa", "Status");
    private final JTable tabelPeringatan = Ui.tabel(modelPeringatan);
    private final JPanel wadahPeringatan = new JPanel(new CardLayout());
    private JScrollPane gulirPeringatan;
    private final JPanel grafik = new JPanel(new BorderLayout());
    private final EmptyState kosongBarang = new EmptyState(
            "boxes", "Belum ada barang",
            "Tambahkan barang terlebih dahulu melalui halaman Barang.");
    private final JLabel kosongPeringatan = Ui.keterangan(" ");
    private final JLabel ringkasanPeringatan = Ui.keterangan(" ");
    private final JComboBox<String> comboSatuanGrafik = Ui.combo();
    private boolean memperbaruiSatuanGrafik;

    public PanelRingkasan(KonteksKantin konteks) {
        super(konteks, "Ringkasan",
                "Pantau stok, peringatan kedaluwarsa, dan kebutuhan restok.");
        pilihanBarang.setName("barangRingkasan");
        pilihanBarang.addItem(PilihanBarang.SEMUA);
        pilihanBarang.addActionListener(event -> pilihBarang());
        tanggalPilihan.komponenTeks().setName("tanggalRingkasan");
        pesanTanggal.setName("pesanTanggalRingkasan");
        tanggalTerakhir = konteks.getTanggalAcuan();
        tanggalKonteksTerakhir = tanggalTerakhir;
        tanggalPilihan.setTanggal(tanggalTerakhir);
        tombolPeriksa.setName("periksaRingkasan");
        tombolPeriksa.setIcon(Ui.ikonAksi("monitor", 16, java.awt.Color.WHITE));
        tombolPeriksa.setIconTextGap(7);
        comboSatuanGrafik.setName("satuanGrafik");
        comboSatuanGrafik.setToolTipText(
                "Pilih satuan yang dibandingkan pada grafik.");
        comboSatuanGrafik.setPreferredSize(new Dimension(
                Ui.skala(110), Ui.tinggiKontrol()));
        comboSatuanGrafik.addActionListener(event -> {
            if (!memperbaruiSatuanGrafik) {
                gambarGrafik();
            }
        });
        kartuFisik.getNilai().setName("ringkasanFisik");
        kartuLayak.getNilai().setName("ringkasanLayak");
        kartuMinimum.getNilai().setName("ringkasanMinimum");
        kartuStatus.getNilai().setName("ringkasanStatus");
        kartuBarang.getNilai().setName("ringkasanBarang");
        kartuPerluRestok.getNilai().setName("ringkasanPerluRestok");
        kartuSegera.getNilai().setName("ringkasanSegera");
        kartuKedaluwarsa.getNilai().setName("ringkasanKedaluwarsa");
        tabel.setName("tabelRingkasan");
        tombolPeriksa.addActionListener(event -> periksa());
        Ui.rataKanan(tabel, 2, 3);
        tabel.getColumnModel().getColumn(4)
                .setCellRenderer(new PenyajiStatus());
        Ui.aturKolom(tabel, 230, 85, 110, 105, 110);
        pasangTabelPeringatan();
        bangunIsi();
    }

    private void pasangTabelPeringatan() {
        tabelPeringatan.setName("tabelPeringatan");
        tabelPeringatan.setRowHeight(Ui.skala(52));
        tabelPeringatan.setFillsViewportHeight(false);
        tabelPeringatan.getColumnModel().getColumn(0)
                .setCellRenderer(new PenyajiBarangBatch());
        tabelPeringatan.getColumnModel().getColumn(1)
                .setCellRenderer(new PenyajiTeksPeringatan(true));
        tabelPeringatan.getColumnModel().getColumn(2)
                .setCellRenderer(new PenyajiTeksPeringatan(false));
        tabelPeringatan.getColumnModel().getColumn(3)
                .setCellRenderer(new PenyajiStatusPeringatan());
        Ui.rataKananHeader(tabelPeringatan, 1);
        int[] lebar = {420, 150, 160, 170};
        int[] minimum = {170, 70, 100, 135};
        tabelPeringatan.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        for (int i = 0; i < lebar.length; i++) {
            tabelPeringatan.getColumnModel().getColumn(i)
                    .setPreferredWidth(Ui.skala(lebar[i]));
            tabelPeringatan.getColumnModel().getColumn(i)
                    .setMinWidth(Ui.skala(minimum[i]));
        }
    }

    @Override
    public int lebarMaksimum() {
        return Ui.LEBAR_KONTEN_RINGKASAN;
    }

    private void bangunIsi() {
        JPanel isi = new JPanel(new BorderLayout(0, 16));
        isi.setOpaque(false);

        JPanel kontrol = Ui.kartu("Filter barang", "search");
        kontrol.setBorder(new EmptyBorder(Ui.skala(12), Ui.skala(14),
                Ui.skala(12), Ui.skala(14)));
        JLabel labelBarang = Ui.labelField("Barang");
        JLabel labelTanggal = Ui.labelField("Tanggal acuan");
        kontrol.add(buatBarisFilter(labelBarang, labelTanggal),
                BorderLayout.CENTER);

        panelKartuSemua = Ui.gridBertahap(4, 820, 480,
                kartuBarang, kartuPerluRestok, kartuSegera, kartuKedaluwarsa);
        panelKartuSatuan = Ui.gridBertahap(4, 820, 480,
                kartuFisik, kartuLayak, kartuMinimum, kartuStatus);
        nilai.setOpaque(false);
        nilai.add(panelKartuSemua, KARTU_SEMUA);
        nilai.add(panelKartuSatuan, KARTU_SATU);
        tampilkanKartu(true);

        JPanel peringatan = Ui.kartu("Peringatan kedaluwarsa", "alert-triangle");
        JPanel peringatanIsi = new JPanel(new BorderLayout(0, 8));
        peringatanIsi.setOpaque(false);
        peringatanIsi.add(ringkasanPeringatan, BorderLayout.NORTH);
        gulirPeringatan = Ui.scrollTabel(tabelPeringatan, 120);
        gulirPeringatan.setPreferredSize(null);
        gulirPeringatan.setMinimumSize(new Dimension(0, 0));
        gulirPeringatan.setVerticalScrollBarPolicy(
                javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        gulirPeringatan.setHorizontalScrollBarPolicy(
                javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        wadahPeringatan.setOpaque(false);
        wadahPeringatan.add(gulirPeringatan, "tabel");
        wadahPeringatan.add(kosongPeringatan, "kosong");
        peringatanIsi.add(wadahPeringatan, BorderLayout.CENTER);
        peringatan.add(peringatanIsi, BorderLayout.CENTER);
        kosongPeringatan.setBorder(new EmptyBorder(
                Ui.skala(12), 0, Ui.skala(12), 0));

        JPanel grafikKartu = Ui.kartu("Stok fisik per barang", "chart-column");
        JPanel kepalaGrafik = (JPanel) ((BorderLayout) grafikKartu.getLayout())
                .getLayoutComponent(BorderLayout.NORTH);
        if (kepalaGrafik != null) {
            kepalaGrafik.add(comboSatuanGrafik, BorderLayout.EAST);
        }
        grafik.setOpaque(false);
        grafikKartu.add(grafik, BorderLayout.CENTER);

        JPanel samping = new JPanel(new BorderLayout(0, 12));
        samping.setOpaque(false);
        samping.add(peringatan, BorderLayout.NORTH);
        samping.add(grafikKartu, BorderLayout.CENTER);

        JPanel semua = Ui.kartu("Semua barang", "package");
        JPanel semuaIsi = new JPanel(new BorderLayout(0, 6));
        semuaIsi.setOpaque(false);
        gulirTabel = Ui.scrollTabel(tabel, 280);
        semuaIsi.add(gulirTabel, BorderLayout.CENTER);
        semuaIsi.add(kosongBarang, BorderLayout.SOUTH);
        semua.add(semuaIsi, BorderLayout.CENTER);

        JPanel atas = new JPanel(new BorderLayout(0, 12));
        atas.setOpaque(false);
        atas.add(kontrol, BorderLayout.NORTH);
        atas.add(nilai, BorderLayout.CENTER);
        isi.add(atas, BorderLayout.NORTH);
        isi.add(buatAreaUtama(semua, samping), BorderLayout.CENTER);
        add(isi, BorderLayout.CENTER);
    }

    private JPanel buatBarisFilter(JLabel labelBarang, JLabel labelTanggal) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.addComponentListener(new ComponentAdapter() {
            private boolean sempit;

            @Override
            public void componentResized(ComponentEvent event) {
                boolean sekarang = panel.getWidth() > 0
                        && panel.getWidth() < Ui.skala(700);
                if (sekarang == sempit) {
                    return;
                }
                sempit = sekarang;
                aturFilter(panel, labelBarang, labelTanggal, sempit);
                panel.revalidate();
                panel.repaint();
            }
        });
        aturFilter(panel, labelBarang, labelTanggal, false);
        return panel;
    }

    /**
     * Grid dua baris pada lebar normal: label di atas, kontrol di bawah.
     * Pada lebar sempit, Barang memakai baris pertama dan tanggal+Periksa
     * berdampingan pada baris kedua.
     */
    private void aturFilter(JPanel panel, JLabel labelBarang,
            JLabel labelTanggal, boolean sempit) {
        panel.removeAll();
        GridBagConstraints c = new GridBagConstraints();
        int gap = Ui.skala(Ui.GAP_FORM);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        if (!sempit) {
            c.gridy = 0;
            c.gridx = 0;
            c.gridwidth = 1;
            c.weightx = 0;
            c.insets = new Insets(0, 0, 0, 0);
            panel.add(labelBarang, c);
            c.gridx = 1;
            c.insets = new Insets(0, gap, 0, 0);
            panel.add(labelTanggal, c);

            c.gridy = 1;
            c.gridx = 0;
            c.weightx = 1;
            c.fill = GridBagConstraints.BOTH;
            c.insets = new Insets(Ui.skala(4), 0, 0, 0);
            panel.add(pilihanBarang, c);
            c.gridx = 1;
            c.weightx = 0;
            c.insets = new Insets(Ui.skala(4), gap, 0, 0);
            panel.add(tanggalPilihan, c);
            c.gridx = 2;
            c.insets = new Insets(Ui.skala(4), gap, 0, 0);
            panel.add(tombolPeriksa, c);

            c.gridy = 2;
            c.gridx = 1;
            c.gridwidth = 2;
            c.weightx = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(Ui.skala(4), gap, 0, 0);
            panel.add(pesanTanggal, c);
        } else {
            c.gridy = 0;
            c.gridx = 0;
            c.gridwidth = 2;
            c.weightx = 0;
            c.insets = new Insets(0, 0, 0, 0);
            panel.add(labelBarang, c);

            c.gridy = 1;
            c.gridx = 0;
            c.weightx = 1;
            c.fill = GridBagConstraints.BOTH;
            c.insets = new Insets(Ui.skala(4), 0, 0, 0);
            panel.add(pilihanBarang, c);

            c.gridy = 2;
            c.gridx = 0;
            c.weightx = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(Ui.skala(10), 0, 0, 0);
            panel.add(labelTanggal, c);

            c.gridy = 3;
            c.gridx = 0;
            c.gridwidth = 1;
            c.weightx = 1;
            c.fill = GridBagConstraints.BOTH;
            c.insets = new Insets(Ui.skala(4), 0, 0, 0);
            panel.add(tanggalPilihan, c);
            c.gridx = 1;
            c.weightx = 0;
            c.insets = new Insets(Ui.skala(4), gap, 0, 0);
            panel.add(tombolPeriksa, c);

            c.gridy = 4;
            c.gridx = 0;
            c.gridwidth = 2;
            c.weightx = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(Ui.skala(4), 0, 0, 0);
            panel.add(pesanTanggal, c);
        }
    }

    /**
     * Tabel menjadi konten utama; peringatan dan grafik berada di samping
     * pada lebar besar, lalu ditumpuk di bawah tabel pada lebar sempit.
     */
    private JPanel buatAreaUtama(JPanel tabelKartu, JPanel samping) {
        JPanel area = new JPanel(new BorderLayout(16, 0));
        area.setOpaque(false);
        area.add(tabelKartu, BorderLayout.CENTER);
        area.add(samping, BorderLayout.EAST);
        samping.setPreferredSize(new Dimension(Ui.skala(560),
                samping.getPreferredSize().height));
        area.addComponentListener(new ComponentAdapter() {
            private boolean sempit;

            @Override
            public void componentResized(ComponentEvent event) {
                boolean sekarang = area.getWidth() > 0
                        && area.getWidth() < Ui.skala(1000);
                if (sekarang == sempit) {
                    return;
                }
                sempit = sekarang;
                area.removeAll();
                if (sempit) {
                    samping.setPreferredSize(null);
                    area.setLayout(new BorderLayout(0, 16));
                    area.add(tabelKartu, BorderLayout.NORTH);
                    area.add(samping, BorderLayout.CENTER);
                } else {
                    samping.setPreferredSize(new Dimension(Ui.skala(560),
                            samping.getPreferredSize().height));
                    area.setLayout(new BorderLayout(16, 0));
                    area.add(tabelKartu, BorderLayout.CENTER);
                    area.add(samping, BorderLayout.EAST);
                }
                area.revalidate();
                area.repaint();
            }
        });
        return area;
    }

    private void periksa() {
        if (tanggalPilihan.ambilTeksTidakValid()) {
            pesanTanggal.setText("Tanggal tidak valid. Pilih tanggal yang benar.");
            tanggalPilihan.komponenTeks().requestFocusInWindow();
            return;
        }
        LocalDate dipilih = tanggalPilihan.getTanggal();
        if (dipilih == null) {
            pesanTanggal.setText("Tanggal acuan wajib diisi.");
            tanggalPilihan.komponenTeks().requestFocusInWindow();
            return;
        }
        if (!tanggalPilihan.isTextFieldValid()) {
            pesanTanggal.setText("Tanggal tidak valid. Pilih tanggal yang benar.");
            tanggalPilihan.komponenTeks().requestFocusInWindow();
            return;
        }
        pesanTanggal.setText(" ");
        tanggalTerakhir = dipilih;
        tanggalKonteksTerakhir = dipilih;
        konteks.setTanggalPemantauan(dipilih);
        konteks.tampilkanPesan("Ringkasan diperiksa untuk tanggal "
                + PemilihTanggal.format(dipilih) + ".", true);
    }

    private void pilihBarang() {
        if (memperbaruiPilihan) {
            return;
        }
        PilihanBarang dipilih = (PilihanBarang) pilihanBarang
                .getSelectedItem();
        if (dipilih == null || dipilih.idBarang() == idBarang) {
            return;
        }
        barang = dipilih.barang();
        idBarang = dipilih.idBarang();
        perbarui();
    }

    private boolean perbaruiPilihanBarang() {
        List<Barang> daftar = konteks.getDaftarBarang();
        int idTarget = idBarang;
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
                pilihanBarang.setSelectedItem(PilihanBarang.SEMUA);
                pilihanBarang.setEnabled(false);
                pilihanBarang.setToolTipText(
                        "Belum ada barang. Tambahkan melalui halaman Barang.");
                return false;
            }

            pilihanBarang.setEnabled(true);
            PilihanBarang terpilih = PilihanBarang.SEMUA;
            if (idTarget >= 0) {
                for (int i = 1; i < pilihanBarang.getItemCount(); i++) {
                    PilihanBarang item = pilihanBarang.getItemAt(i);
                    if (item.idBarang() == idTarget) {
                        terpilih = item;
                        break;
                    }
                }
            }
            pilihanBarang.setSelectedItem(terpilih);
            barang = terpilih.barang();
            idBarang = terpilih.idBarang();
            pilihanBarang.setToolTipText(
                    terpilih == PilihanBarang.SEMUA
                            ? "Seluruh barang terdaftar"
                            : terpilih.barang().getNama() + " ("
                                    + terpilih.barang().getSatuan() + ")");
            return true;
        } finally {
            memperbaruiPilihan = false;
        }
    }

    private void tampilkanKartu(boolean semua) {
        ((CardLayout) nilai.getLayout()).show(
                nilai, semua ? KARTU_SEMUA : KARTU_SATU);
        nilai.revalidate();
        nilai.repaint();
    }

    private void perbaruiKartuSemua(LocalDate tanggal) {
        List<Barang> daftar = konteks.getDaftarBarang();
        int perluRestok = 0;
        int segera = 0;
        int kedaluwarsa = 0;
        for (Barang item : daftar) {
            if (item.hitungStokLayak(tanggal) <= item.getStokMinimum()) {
                perluRestok++;
            }
            for (BatchStok batch : item.getDaftarBatch()) {
                if (batch.getSisaJumlah() <= 0) {
                    continue;
                }
                StatusKedaluwarsa status = batch.statusPada(tanggal);
                if (status == StatusKedaluwarsa.SEGERA_KEDALUWARSA) {
                    segera++;
                } else if (status == StatusKedaluwarsa.KEDALUWARSA) {
                    kedaluwarsa++;
                }
            }
        }

        tampilkanKartu(true);
        kartuBarang.setNilai(daftar.size() + " barang");
        kartuBarang.setKeterangan("Dari seluruh barang");
        kartuBarang.setAksen(Ui.AKSEN);
        kartuPerluRestok.setNilai(perluRestok + " barang");
        kartuPerluRestok.setKeterangan("Layak \u2264 minimum");
        kartuPerluRestok.setAksen(
                perluRestok > 0 ? Ui.MERAH : Ui.TEKS_LEMBUT);
        kartuSegera.setNilai(segera + " batch");
        kartuSegera.setKeterangan("Sisa batch segera");
        kartuSegera.setAksen(segera > 0 ? Ui.KUNING : Ui.TEKS_LEMBUT);
        kartuKedaluwarsa.setNilai(kedaluwarsa + " batch");
        kartuKedaluwarsa.setKeterangan("Sisa batch lewat");
        kartuKedaluwarsa.setAksen(
                kedaluwarsa > 0 ? Ui.MERAH : Ui.TEKS_LEMBUT);
    }

    @Override
    public void perbarui() {
        perbaruiPilihanBarang();
        LocalDate dariKonteks = konteks.getTanggalAcuan();
        LocalDate dipilih = tanggalPilihan.getTanggal();
        if (dipilih != null && dipilih.equals(tanggalKonteksTerakhir)
                && !dipilih.equals(dariKonteks)) {
            tanggalPilihan.setTanggal(dariKonteks);
        }
        tanggalKonteksTerakhir = dariKonteks;
        tanggalTerakhir = dariKonteks;
        LocalDate acuan = dariKonteks;

        if (barang == null) {
            perbaruiKartuSemua(acuan);
            perbaruiTabel(acuan);
            perbaruiPeringatan(acuan);
            perbaruiGrafik();
            return;
        }

        tampilkanKartu(false);
        int fisik = barang.hitungSaldo();
        int layak = barang.hitungStokLayak(acuan);
        int minimum = barang.getStokMinimum();
        String satuan = barang.getSatuan();
        boolean perluRestok = layak <= minimum;

        kartuFisik.setNilai(fisik + " " + satuan);
        kartuFisik.setKeterangan("Seluruh sisa batch");
        kartuFisik.setAksen(Ui.AKSEN);
        kartuLayak.setNilai(layak + " " + satuan);
        kartuLayak.setKeterangan("Layak pada tanggal acuan");
        kartuLayak.setAksen(Ui.AKSEN);
        kartuMinimum.setNilai(minimum + " " + satuan);
        kartuMinimum.setKeterangan("Batas kebutuhan");
        kartuMinimum.setAksen(Ui.TEKS_LEMBUT);
        kartuStatus.setNilai(perluRestok ? "Perlu restok" : "Cukup");
        kartuStatus.setKeterangan(perluRestok
                ? "Pada atau di bawah batas"
                : "Di atas batas");
        kartuStatus.setAksen(perluRestok ? Ui.MERAH : Ui.AKSEN);

        perbaruiTabel(acuan);
        perbaruiPeringatan(acuan);
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
        if (gulirTabel != null) {
            gulirTabel.setVisible(!kosong);
        }
        tabel.setVisible(!kosong);
    }

    private void perbaruiPeringatan(LocalDate tanggal) {
        List<Barang> daftar = konteks.getDaftarBarang();
        if (daftar.isEmpty()) {
            modelPeringatan.setRowCount(0);
            kosongPeringatan.setText("Belum ada barang untuk dipantau.");
            ((CardLayout) wadahPeringatan.getLayout())
                    .show(wadahPeringatan, "kosong");
            ringkasanPeringatan.setText(" ");
            sesuaikanTinggiPeringatan();
            return;
        }

        boolean semua = barang == null;
        List<Barang> lingkup = semua ? daftar : List.of(barang);
        List<BarisPeringatan> hasil = new ArrayList<>();
        for (Barang item : lingkup) {
            for (BatchStok batch : item.getDaftarBatch()) {
                if (batch.getSisaJumlah() <= 0) {
                    continue;
                }
                StatusKedaluwarsa status = batch.statusPada(tanggal);
                if (status != StatusKedaluwarsa.AMAN) {
                    hasil.add(new BarisPeringatan(item, batch, status));
                }
            }
        }
        hasil.sort(Comparator
                .comparingInt((BarisPeringatan baris) ->
                        baris.status() == StatusKedaluwarsa.KEDALUWARSA
                                ? 0 : 1)
                .thenComparing(baris -> baris.batch()
                        .getTanggalKedaluwarsa())
                .thenComparing(baris -> baris.barang().getNama(),
                        String.CASE_INSENSITIVE_ORDER)
                .thenComparing(baris -> baris.batch().getKode(),
                        String.CASE_INSENSITIVE_ORDER));

        modelPeringatan.setRowCount(0);
        for (BarisPeringatan baris : hasil) {
            modelPeringatan.addRow(new Object[]{
                baris,
                baris.batch().getSisaJumlah() + " "
                        + baris.barang().getSatuan(),
                PemilihTanggal.format(
                        baris.batch().getTanggalKedaluwarsa()),
                baris.status()
            });
        }

        if (hasil.isEmpty()) {
            kosongPeringatan.setText(
                    "Tidak ada peringatan pada tanggal ini.");
            ((CardLayout) wadahPeringatan.getLayout())
                    .show(wadahPeringatan, "kosong");
            ringkasanPeringatan.setText(" ");
        } else {
            ((CardLayout) wadahPeringatan.getLayout())
                    .show(wadahPeringatan, "tabel");
            ringkasanPeringatan.setText(semua
                    ? hasil.size() + " batch dari seluruh barang perlu "
                            + "perhatian pada tanggal acuan."
                    : hasil.size() + " batch " + barang.getNama()
                            + " perlu perhatian pada tanggal acuan.");
        }
        sesuaikanTinggiPeringatan();
        wadahPeringatan.revalidate();
        wadahPeringatan.repaint();
    }

    private void sesuaikanTinggiPeringatan() {
        if (gulirPeringatan == null) {
            return;
        }
        int baris = Math.min(modelPeringatan.getRowCount(), 5);
        int tinggi = tabelPeringatan.getTableHeader()
                .getPreferredSize().height
                + baris * tabelPeringatan.getRowHeight() + 2;
        gulirPeringatan.setPreferredSize(new Dimension(0, tinggi));
        gulirPeringatan.revalidate();
    }

    private record BarisPeringatan(Barang barang, BatchStok batch,
            StatusKedaluwarsa status) {
    }

    private static final class PenyajiBarangBatch extends JPanel
            implements TableCellRenderer {
        private final JLabel nama = new JLabel();
        private final JLabel batch = new JLabel();

        private PenyajiBarangBatch() {
            super();
            setOpaque(true);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(new EmptyBorder(Ui.skala(6), Ui.skala(
                    Ui.PADDING_SEL_TABEL), Ui.skala(6), Ui.skala(
                    Ui.PADDING_SEL_TABEL)));
            nama.setFont(Ui.font(Font.BOLD, Ui.FONT_BODY));
            nama.setForeground(Ui.TEKS);
            batch.setFont(Ui.font(Font.PLAIN, Ui.FONT_CATATAN));
            batch.setForeground(Ui.TEKS_LEMBUT);
            add(nama);
            add(Box.createVerticalStrut(2));
            add(batch);
        }

        @Override
        public Component getTableCellRendererComponent(JTable tabel,
                Object nilai, boolean dipilih, boolean fokus, int baris,
                int kolom) {
            if (nilai instanceof BarisPeringatan data) {
                nama.setText(data.barang().getNama());
                batch.setText(data.batch().getKode());
            }
            int hover = tabel instanceof TabelKantin
                    ? ((TabelKantin) tabel).getBarisHover() : -1;
            setBackground(dipilih ? tabel.getSelectionBackground()
                    : (baris == hover ? Ui.HOVER : Ui.KARTU));
            return this;
        }
    }

    private static final class PenyajiTeksPeringatan
            extends DefaultTableCellRenderer {
        private final boolean kanan;

        private PenyajiTeksPeringatan(boolean kanan) {
            this.kanan = kanan;
        }

        @Override
        public Component getTableCellRendererComponent(JTable tabel,
                Object nilai, boolean dipilih, boolean fokus, int baris,
                int kolom) {
            super.getTableCellRendererComponent(
                    tabel, nilai, dipilih, fokus, baris, kolom);
            setBorder(new EmptyBorder(0, Ui.skala(Ui.PADDING_SEL_TABEL),
                    0, Ui.skala(Ui.PADDING_SEL_TABEL)));
            setHorizontalAlignment(kanan
                    ? SwingConstants.RIGHT : SwingConstants.LEFT);
            setVerticalAlignment(SwingConstants.CENTER);
            setFont(Ui.font(Font.PLAIN, Ui.FONT_BODY));
            int hover = tabel instanceof TabelKantin
                    ? ((TabelKantin) tabel).getBarisHover() : -1;
            setForeground(Ui.TEKS);
            setBackground(dipilih ? tabel.getSelectionBackground()
                    : (baris == hover ? Ui.HOVER : Ui.KARTU));
            return this;
        }
    }

    private static final class PenyajiStatusPeringatan
            extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tabel,
                Object nilai, boolean dipilih, boolean fokus, int baris,
                int kolom) {
            super.getTableCellRendererComponent(
                    tabel, nilai, dipilih, fokus, baris, kolom);
            StatusKedaluwarsa status = nilai instanceof StatusKedaluwarsa s
                    ? s : StatusKedaluwarsa.AMAN;
            setText(status.getLabel());
            setBorder(new EmptyBorder(0, Ui.skala(Ui.PADDING_SEL_TABEL),
                    0, Ui.skala(Ui.PADDING_SEL_TABEL)));
            setHorizontalAlignment(SwingConstants.LEFT);
            setVerticalAlignment(SwingConstants.CENTER);
            setFont(Ui.font(Font.BOLD, Ui.FONT_CATATAN));
            int hover = tabel instanceof TabelKantin
                    ? ((TabelKantin) tabel).getBarisHover() : -1;
            setForeground(dipilih ? tabel.getSelectionForeground()
                    : status == StatusKedaluwarsa.KEDALUWARSA
                            ? Ui.MERAH : Ui.KUNING_BADGE);
            setBackground(dipilih ? tabel.getSelectionBackground()
                    : (baris == hover ? Ui.HOVER : Ui.KARTU));
            return this;
        }
    }

    private void perbaruiGrafik() {
        perbaruiPilihanSatuanGrafik();
        gambarGrafik();
    }

    private void perbaruiPilihanSatuanGrafik() {
        String terpilih = (String) comboSatuanGrafik.getSelectedItem();
        Set<String> satuan = new LinkedHashSet<>();
        for (Barang barang : konteks.getDaftarBarang()) {
            satuan.add(barang.getSatuan());
        }
        memperbaruiSatuanGrafik = true;
        try {
            comboSatuanGrafik.removeAllItems();
            for (String item : satuan) {
                comboSatuanGrafik.addItem(item);
            }
            if (terpilih != null && satuan.contains(terpilih)) {
                comboSatuanGrafik.setSelectedItem(terpilih);
            } else if (barang != null && satuan.contains(barang.getSatuan())) {
                comboSatuanGrafik.setSelectedItem(barang.getSatuan());
            } else if (comboSatuanGrafik.getItemCount() > 0) {
                comboSatuanGrafik.setSelectedIndex(0);
            }
            comboSatuanGrafik.setEnabled(!satuan.isEmpty());
        } finally {
            memperbaruiSatuanGrafik = false;
        }
    }

    private void gambarGrafik() {
        grafik.removeAll();
        String satuan = (String) comboSatuanGrafik.getSelectedItem();
        List<String> nama = new ArrayList<>();
        List<Integer> nilai = new ArrayList<>();
        boolean adaPositif = false;
        for (Barang barang : konteks.getDaftarBarang()) {
            if (satuan == null || !satuan.equals(barang.getSatuan())) {
                continue;
            }
            int saldo = barang.hitungSaldo();
            nama.add(barang.getNama());
            nilai.add(saldo);
            adaPositif |= saldo > 0;
        }
        if (nama.isEmpty() || !adaPositif) {
            JLabel kosongGrafik = Ui.keterangan(
                    "Belum ada stok untuk ditampilkan");
            kosongGrafik.setHorizontalAlignment(
                    javax.swing.SwingConstants.CENTER);
            kosongGrafik.setBorder(new EmptyBorder(
                    Ui.skala(28), Ui.skala(8), Ui.skala(28), Ui.skala(8)));
            grafik.add(kosongGrafik, BorderLayout.CENTER);
        } else {
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

    private static final class PilihanBarang {
        private static final PilihanBarang SEMUA = new PilihanBarang(null);
        private final Barang barang;

        private PilihanBarang(Barang barang) {
            this.barang = barang;
        }

        private Barang barang() {
            return barang;
        }

        private int idBarang() {
            return barang == null ? -1 : barang.getId();
        }

        @Override
        public String toString() {
            return barang == null
                    ? "Semua Barang"
                    : barang.getNama() + " (" + barang.getSatuan() + ")";
        }
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
