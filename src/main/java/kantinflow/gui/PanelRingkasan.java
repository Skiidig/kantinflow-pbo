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
import java.util.List;

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

import kantinflow.Barang;
import kantinflow.BatchStok;
import kantinflow.StatusKedaluwarsa;

/**
 * Dashboard ringkasan stok: statistik, peringatan kedaluwarsa, dan tabel
 * barang.
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
            "Barang", "Satuan", "Stok layak", "Stok fisik", "Stok minimum",
            "Status");
    private final JTable tabel = Ui.tabel(model);
    private final JLabel jumlahBarang = Ui.keterangan(" ");
    private final DefaultTabel modelPeringatan = new DefaultTabel(
            "Barang / Batch", "Sisa", "Kedaluwarsa", "Status");
    private final JTable tabelPeringatan = Ui.tabel(modelPeringatan);
    private final JPanel wadahPeringatan = new JPanel(new CardLayout());
    private JScrollPane gulirPeringatan;
    private final EmptyState kosongBarang = new EmptyState(
            "boxes", "Belum ada barang",
            "Tambahkan barang terlebih dahulu melalui halaman Barang.");
    private final EmptyState kosongPeringatan = new EmptyState(
            "circle-check", "Tidak ada peringatan kedaluwarsa",
            "Semua batch bersisa dalam kondisi aman pada tanggal acuan ini.");
    private final JLabel ringkasanPeringatan = Ui.keterangan(" ");
    private final JPanel panelFilterRestok = new JPanel(
            new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
    private final JButton tombolResetRestok = Ui.sekunder("Tampilkan Semua");
    private final JButton tombolResetKosong = Ui.sekunder("Tampilkan Semua");
    private final JButton tombolBukaBarang = Ui.utama("Buka Halaman Barang");
    private boolean hanyaPerluRestok;

    public PanelRingkasan(KonteksKantin konteks) {
        super(konteks, "Ringkasan",
                "Lihat kondisi stok dan barang yang perlu ditindaklanjuti.");
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
        kartuFisik.getNilai().setName("ringkasanFisik");
        kartuLayak.getNilai().setName("ringkasanLayak");
        kartuMinimum.getNilai().setName("ringkasanMinimum");
        kartuStatus.getNilai().setName("ringkasanStatus");
        kartuBarang.getNilai().setName("ringkasanBarang");
        kartuPerluRestok.getNilai().setName("ringkasanPerluRestok");
        kartuSegera.getNilai().setName("ringkasanSegera");
        kartuKedaluwarsa.getNilai().setName("ringkasanKedaluwarsa");
        pasangPintasanKartu();
        tabel.setName("tabelRingkasan");
        tombolPeriksa.addActionListener(event -> periksa());
        tombolResetRestok.addActionListener(event -> aturFilterRestok(false));
        tombolResetKosong.addActionListener(event -> aturFilterRestok(false));
        tombolBukaBarang.addActionListener(
                event -> konteks.bukaHalaman("barang"));
        tabel.setRowHeight(Ui.skala(44));
        tabel.setFillsViewportHeight(false);
        tabel.getColumnModel().getColumn(0)
                .setCellRenderer(new PenyajiSelRingkasan(SwingConstants.LEFT));
        tabel.getColumnModel().getColumn(1)
                .setCellRenderer(new PenyajiSelRingkasan(SwingConstants.LEFT));
        tabel.getColumnModel().getColumn(2)
                .setCellRenderer(new PenyajiSelRingkasan(SwingConstants.RIGHT));
        tabel.getColumnModel().getColumn(3)
                .setCellRenderer(new PenyajiSelRingkasan(SwingConstants.RIGHT));
        tabel.getColumnModel().getColumn(4)
                .setCellRenderer(new PenyajiSelRingkasan(SwingConstants.RIGHT));
        Ui.pasangBadge(tabel, 5);
        Ui.rataKananHeader(tabel, 2, 3, 4);
        Ui.aturKolom(tabel, 210, 80, 110, 110, 115, 110);
        aturKolomStatus(tabel, 5);
        Ui.gulirHorizontalSaatSempit(tabel);
        pasangTabelPeringatan();
        bangunIsi();
    }

    private void pasangPintasanKartu() {
        kartuBarang.setName("kartuRingkasanBarang");
        kartuPerluRestok.setName("kartuRingkasanPerluRestok");
        kartuSegera.setName("kartuRingkasanSegera");
        kartuKedaluwarsa.setName("kartuRingkasanKedaluwarsa");
        kartuFisik.setName("kartuRingkasanFisik");
        kartuLayak.setName("kartuRingkasanLayak");
        kartuMinimum.setName("kartuRingkasanMinimum");
        kartuStatus.setName("kartuRingkasanStatus");

        kartuBarang.setAksiKlik(
                () -> konteks.bukaHalaman("barang"),
                "Klik untuk membuka halaman Barang");
        kartuPerluRestok.setAksiKlik(
                () -> aturFilterRestok(!hanyaPerluRestok),
                "Klik untuk memfilter barang berstatus Perlu restok");
        kartuSegera.setAksiKlik(
                () -> konteks.bukaPemantauanStatus(
                        StatusKedaluwarsa.SEGERA_KEDALUWARSA),
                "Klik untuk memantau batch segera kedaluwarsa");
        kartuKedaluwarsa.setAksiKlik(
                () -> konteks.bukaHalaman("pembuangan"),
                "Klik untuk membuka halaman Pembuangan");

        kartuFisik.setAksiKlik(
                () -> konteks.bukaHalaman("stok-masuk"),
                "Klik untuk melihat dan menambah batch di Stok Masuk");
        kartuLayak.setAksiKlik(
                () -> konteks.bukaHalaman("stok-keluar"),
                "Klik untuk mengeluarkan stok layak di Stok Keluar");
        kartuMinimum.setAksiKlik(
                () -> konteks.bukaHalaman("barang"),
                "Klik untuk mengatur stok minimum di halaman Barang");
        kartuStatus.setAksiKlik(
                () -> konteks.bukaPemantauanStatus(null),
                "Klik untuk memeriksa status kedaluwarsa di Pemantauan");
    }

    private void aturFilterRestok(boolean aktif) {
        hanyaPerluRestok = aktif;
        kartuPerluRestok.setAktif(aktif);
        panelFilterRestok.setVisible(aktif);
        perbaruiTabel(konteks.getTanggalAcuan());
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
        Ui.pasangBadge(tabelPeringatan, 3);
        Ui.rataKananHeader(tabelPeringatan, 1);
        int[] lebar = {320, 120, 230, 330};
        int[] minimum = {160, 70, 120, 170};
        tabelPeringatan.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        for (int i = 0; i < lebar.length; i++) {
            tabelPeringatan.getColumnModel().getColumn(i)
                    .setPreferredWidth(Ui.skala(lebar[i]));
            tabelPeringatan.getColumnModel().getColumn(i)
                    .setMinWidth(Ui.skala(minimum[i]));
        }
        aturKolomStatus(tabelPeringatan, 3);
    }

    private void aturKolomStatus(JTable tabel, int kolom) {
        int lebar = Ui.skala(210);
        tabel.getColumnModel().getColumn(kolom).setPreferredWidth(lebar);
        tabel.getColumnModel().getColumn(kolom).setMinWidth(lebar);
        tabel.getColumnModel().getColumn(kolom).setMaxWidth(lebar);
    }

    @Override
    public int lebarMaksimum() {
        return Ui.LEBAR_KONTEN_RINGKASAN;
    }

    private void bangunIsi() {
        JPanel isi = new JPanel(new BorderLayout(0, Ui.skala(Ui.GAP_KARTU)));
        isi.setOpaque(false);

        JPanel kontrol = kartuRingkasan("Filter barang", "search");
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

        JPanel peringatan = kartuRingkasan("Peringatan kedaluwarsa", "alert-triangle");
        JPanel peringatanIsi = new JPanel(new BorderLayout(0, 8));
        peringatanIsi.setOpaque(false);
        peringatanIsi.add(ringkasanPeringatan, BorderLayout.NORTH);
        gulirPeringatan = buatGulirTabel(tabelPeringatan, 120);
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

        panelFilterRestok.setOpaque(false);
        panelFilterRestok.setBorder(new EmptyBorder(0, 0, Ui.skala(6), 0));
        panelFilterRestok.add(Badge.status("Perlu restok"));
        panelFilterRestok.add(Ui.keterangan(
                "Menampilkan hanya barang dengan stok layak \u2264 minimum"));
        panelFilterRestok.add(tombolResetRestok);
        panelFilterRestok.setVisible(false);

        JPanel semua = kartuRingkasan("Semua barang", "package");
        JPanel semuaIsi = new JPanel(new BorderLayout(0, 6));
        semuaIsi.setOpaque(false);
        semuaIsi.add(panelFilterRestok, BorderLayout.NORTH);
        gulirTabel = buatGulirTabel(tabel, 280);
        semuaIsi.add(gulirTabel, BorderLayout.CENTER);
        JPanel kaki = new JPanel(new BorderLayout(0, Ui.skala(8)));
        kaki.setOpaque(false);
        jumlahBarang.setBorder(new EmptyBorder(Ui.skala(8), 0, 0, 0));
        kaki.add(jumlahBarang, BorderLayout.NORTH);
        kaki.add(kosongBarang, BorderLayout.CENTER);
        semuaIsi.add(kaki, BorderLayout.SOUTH);
        semua.add(semuaIsi, BorderLayout.CENTER);

        JPanel bawah = new JPanel(new BorderLayout(0, Ui.skala(Ui.GAP_KARTU)));
        bawah.setOpaque(false);
        bawah.add(semua, BorderLayout.NORTH);
        bawah.add(peringatan, BorderLayout.CENTER);

        JPanel atas = new JPanel(new BorderLayout(0, Ui.skala(Ui.GAP_KARTU)));
        atas.setOpaque(false);
        atas.add(kontrol, BorderLayout.NORTH);
        atas.add(nilai, BorderLayout.CENTER);
        isi.add(atas, BorderLayout.NORTH);
        isi.add(bawah, BorderLayout.CENTER);
        add(isi, BorderLayout.CENTER);
    }

    /**
     * Tabel Ringkasan memakai scrollpane standar tanpa bingkai luar. Kartu
     * pembungkus tetap menjadi satu-satunya bingkai putih membulat.
     */
    private JScrollPane buatGulirTabel(JTable tabel, int tinggi) {
        tabel.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        tabel.getTableHeader().setBackground(Ui.LATAR);
        tabel.getTableHeader().setPreferredSize(
                new Dimension(0, Ui.skala(Ui.TINGGI_HEADER_TABEL)));
        tabel.setFillsViewportHeight(false);
        JScrollPane gulir = new JScrollPane(tabel);
        gulir.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        gulir.setViewportBorder(null);
        gulir.setBackground(Ui.KARTU);
        gulir.setOpaque(true);
        gulir.getViewport().setOpaque(true);
        gulir.getViewport().setBackground(Ui.KARTU);
        if (gulir.getColumnHeader() != null) {
            gulir.getColumnHeader().setOpaque(true);
            gulir.getColumnHeader().setBackground(Ui.KARTU);
        }
        gulir.setPreferredSize(new Dimension(600, Ui.skala(tinggi)));
        gulir.getVerticalScrollBar().setUnitIncrement(16);
        gulir.setVerticalScrollBarPolicy(
                javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        gulir.setHorizontalScrollBarPolicy(
                javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        return gulir;
    }

    /** Gaya kartu dashboard seragam dengan CardPanel standar. */
    private JPanel kartuRingkasan(String judul, String ikon) {
        JPanel kartu = Ui.kartu(judul, ikon);
        JPanel kepala = (JPanel) ((BorderLayout) kartu.getLayout())
                .getLayoutComponent(BorderLayout.NORTH);
        kepala.setOpaque(false);
        kepala.setBackground(Ui.KARTU);
        kepala.setBorder(new EmptyBorder(0, 0, 0, 0));
        return kartu;
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
                + FormatTanggal.format(dipilih) + ".", true);
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
                perluRestok > 0 ? Ui.MERAH : Ui.HIJAU);
        kartuSegera.setNilai(segera + " batch");
        kartuSegera.setKeterangan("Sisa batch segera");
        kartuSegera.setAksen(segera > 0 ? Ui.KUNING : Ui.HIJAU);
        kartuKedaluwarsa.setNilai(kedaluwarsa + " batch");
        kartuKedaluwarsa.setKeterangan("Sisa batch lewat");
        kartuKedaluwarsa.setAksen(
                kedaluwarsa > 0 ? Ui.MERAH : Ui.HIJAU);
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
        kartuStatus.setAksen(perluRestok ? Ui.MERAH : Ui.HIJAU);

        perbaruiTabel(acuan);
        perbaruiPeringatan(acuan);
    }

    private void perbaruiTabel(LocalDate tanggal) {
        List<Barang> semuaBarang = konteks.getDaftarBarang();
        model.setRowCount(0);
        for (Barang item : semuaBarang) {
            int layak = item.hitungStokLayak(tanggal);
            int fisik = item.hitungSaldo();
            boolean restok = layak <= item.getStokMinimum();
            if (hanyaPerluRestok && !restok) {
                continue;
            }
            model.addRow(new Object[]{
                item.getNama(),
                item.getSatuan(),
                layak + " " + item.getSatuan(),
                fisik + " " + item.getSatuan(),
                item.getStokMinimum() + " " + item.getSatuan(),
                restok ? "Perlu restok" : "Cukup"
            });
        }
        jumlahBarang.setText(hanyaPerluRestok
                ? model.getRowCount() + " dari " + semuaBarang.size()
                        + " barang perlu restok"
                : model.getRowCount() + " barang ditampilkan");
        boolean kosong = model.getRowCount() == 0;
        if (kosong) {
            if (semuaBarang.isEmpty()) {
                kosongBarang.setIcon("boxes")
                        .setTitle("Belum ada barang")
                        .setDescription(
                                "Tambahkan barang terlebih dahulu melalui halaman Barang.")
                        .setAction(tombolBukaBarang);
            } else {
                kosongBarang.setIcon("circle-check")
                        .setTitle("Semua stok tercukupi")
                        .setDescription(
                                "Tidak ada barang berstatus Perlu restok pada tanggal acuan ini.")
                        .setAction(tombolResetKosong);
            }
        }
        kosongBarang.setVisible(kosong);
        jumlahBarang.setVisible(!kosong);
        if (gulirTabel != null) {
            gulirTabel.setVisible(!kosong);
        }
        tabel.setVisible(!kosong);
        sesuaikanTinggiTabel();
    }

    private void sesuaikanTinggiTabel() {
        if (gulirTabel == null) {
            return;
        }
        int baris = Math.min(model.getRowCount(), 10);
        int tinggi = tabel.getTableHeader().getPreferredSize().height
                + baris * tabel.getRowHeight() + 2;
        gulirTabel.setPreferredSize(new Dimension(0, tinggi));
        gulirTabel.revalidate();
    }

    private void perbaruiPeringatan(LocalDate tanggal) {
        List<Barang> daftar = konteks.getDaftarBarang();
        if (daftar.isEmpty()) {
            modelPeringatan.setRowCount(0);
            kosongPeringatan.setIcon("boxes")
                    .setTitle("Belum ada barang")
                    .setDescription("Tambahkan barang melalui halaman Barang untuk memantau kedaluwarsa.");
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
                baris.batch().getTanggalKedaluwarsa(),
                baris.status()
            });
        }

        if (hasil.isEmpty()) {
            kosongPeringatan.setIcon("circle-check")
                    .setTitle("Tidak ada peringatan kedaluwarsa")
                    .setDescription(semua
                            ? "Semua batch bersisa dalam kondisi aman pada tanggal acuan ini."
                            : "Seluruh batch " + barang.getNama()
                                    + " dalam kondisi aman pada tanggal acuan ini.");
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
        int baris = Math.min(modelPeringatan.getRowCount(), 10);
        int tinggi = tabelPeringatan.getTableHeader()
                .getPreferredSize().height
                + baris * tabelPeringatan.getRowHeight() + 2;
        gulirPeringatan.setPreferredSize(new Dimension(0, tinggi));
        gulirPeringatan.revalidate();
    }

    private record BarisPeringatan(Barang barang, BatchStok batch,
            StatusKedaluwarsa status) {
    }

    private static Color latarBaris(JTable tabel, boolean dipilih, int baris) {
        if (dipilih) {
            return tabel.getSelectionBackground();
        }
        int hover = tabel instanceof TabelKantin
                ? ((TabelKantin) tabel).getBarisHover() : -1;
        return baris == hover ? Ui.HOVER : Ui.KARTU;
    }

    private static final class PenyajiSelRingkasan
            extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        private final int perataan;

        private PenyajiSelRingkasan(int perataan) {
            this.perataan = perataan;
        }

        @Override
        public Component getTableCellRendererComponent(JTable tabel,
                Object nilai, boolean dipilih, boolean fokus, int baris,
                int kolom) {
            super.getTableCellRendererComponent(
                    tabel, nilai, dipilih, fokus, baris, kolom);
            setOpaque(true);
            setFont(Ui.font(Font.PLAIN, Ui.FONT_BODY));
            setBorder(new EmptyBorder(0, Ui.skala(Ui.PADDING_SEL_TABEL),
                    0, Ui.skala(Ui.PADDING_SEL_TABEL)));
            setHorizontalAlignment(perataan);
            setVerticalAlignment(SwingConstants.CENTER);
            setBackground(latarBaris(tabel, dipilih, baris));
            setForeground(dipilih ? tabel.getSelectionForeground() : Ui.TEKS);
            setToolTipText(null);
            return this;
        }
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
            nama.setFont(Ui.font(Font.PLAIN, Ui.FONT_BODY));
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
            nama.setFont(Ui.font(Font.PLAIN, Ui.FONT_BODY));
            batch.setFont(Ui.font(Font.PLAIN, Ui.FONT_CATATAN));
            nama.setForeground(dipilih
                    ? tabel.getSelectionForeground() : Ui.TEKS);
            batch.setForeground(dipilih
                    ? tabel.getSelectionForeground() : Ui.TEKS_LEMBUT);
            setBorder(new EmptyBorder(Ui.skala(6), Ui.skala(
                    Ui.PADDING_SEL_TABEL), Ui.skala(6), Ui.skala(
                    Ui.PADDING_SEL_TABEL)));
            setBackground(latarBaris(tabel, dipilih, baris));
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
            if (nilai instanceof LocalDate tanggal) {
                setText(FormatTanggal.format(tanggal));
            }
            setBorder(new EmptyBorder(0, Ui.skala(Ui.PADDING_SEL_TABEL),
                    0, Ui.skala(Ui.PADDING_SEL_TABEL)));
            setHorizontalAlignment(kanan
                    ? SwingConstants.RIGHT : SwingConstants.LEFT);
            setVerticalAlignment(SwingConstants.CENTER);
            setFont(Ui.font(Font.PLAIN, Ui.FONT_BODY));
            setForeground(dipilih
                    ? tabel.getSelectionForeground() : Ui.TEKS);
            setBackground(latarBaris(tabel, dipilih, baris));
            return this;
        }
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
