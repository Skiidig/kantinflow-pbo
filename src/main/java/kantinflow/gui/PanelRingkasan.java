package kantinflow.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.time.LocalDate;
import java.util.ArrayList;
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

        JPanel kontrol = Ui.kartu();
        kontrol.setBorder(new EmptyBorder(Ui.skala(12), Ui.skala(14),
                Ui.skala(12), Ui.skala(14)));
        JLabel labelBarang = Ui.labelField("Barang *");
        JLabel labelTanggal = Ui.labelField("Tanggal acuan *");
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
        daftarPeringatan.setOpaque(false);
        daftarPeringatan.setLayout(new BoxLayout(
                daftarPeringatan, BoxLayout.Y_AXIS));
        peringatanIsi.add(daftarPeringatan, BorderLayout.CENTER);
        peringatan.add(peringatanIsi, BorderLayout.CENTER);

        JPanel grafikKartu = Ui.kartu("Stok fisik per barang", "chart-column");
        grafik.setOpaque(false);
        grafikKartu.add(grafik, BorderLayout.CENTER);

        JPanel samping = new JPanel();
        samping.setOpaque(false);
        samping.setLayout(new BoxLayout(samping, BoxLayout.Y_AXIS));
        peringatan.setAlignmentX(LEFT_ALIGNMENT);
        grafikKartu.setAlignmentX(LEFT_ALIGNMENT);
        samping.add(peringatan);
        samping.add(Box.createVerticalStrut(12));
        samping.add(grafikKartu);

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
        samping.setPreferredSize(new Dimension(Ui.skala(340),
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
                    area.setLayout(new BoxLayout(area, BoxLayout.Y_AXIS));
                    area.add(tabelKartu);
                    area.add(Box.createVerticalStrut(16));
                    area.add(samping);
                } else {
                    samping.setPreferredSize(new Dimension(Ui.skala(340),
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
        daftarPeringatan.removeAll();
        List<Barang> daftar = konteks.getDaftarBarang();
        if (daftar.isEmpty()) {
            kosongPeringatan.setTitle("Belum ada barang");
            kosongPeringatan.setDescription(
                    "Tambahkan barang melalui halaman Barang untuk melihat "
                    + "peringatan kedaluwarsa.");
            daftarPeringatan.add(kosongPeringatan);
            ringkasanPeringatan.setText(" ");
            daftarPeringatan.revalidate();
            daftarPeringatan.repaint();
            return;
        }

        boolean semua = barang == null;
        List<Barang> lingkup = semua ? daftar : List.of(barang);
        kosongPeringatan.setTitle("Tidak ada peringatan");
        kosongPeringatan.setDescription(semua
                ? "Semua batch bersisa dari seluruh barang masih aman "
                        + "pada tanggal acuan."
                : "Semua batch bersisa " + barang.getNama()
                        + " masih aman pada tanggal acuan.");
        int jumlah = 0;
        for (Barang item : lingkup) {
            for (BatchStok batch : item.getDaftarBatch()) {
                if (batch.getSisaJumlah() <= 0) {
                    continue;
                }
                StatusKedaluwarsa status = batch.statusPada(tanggal);
                if (status == StatusKedaluwarsa.AMAN) {
                    continue;
                }
                jumlah++;
                daftarPeringatan.add(barisPeringatan(item, batch, status));
                daftarPeringatan.add(Box.createVerticalStrut(6));
            }
        }
        if (jumlah == 0) {
            daftarPeringatan.add(kosongPeringatan);
            ringkasanPeringatan.setText(" ");
        } else {
            ringkasanPeringatan.setText(semua
                    ? jumlah + " batch dari seluruh barang perlu perhatian "
                            + "pada tanggal acuan."
                    : jumlah + " batch " + barang.getNama()
                            + " perlu perhatian pada tanggal acuan.");
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
        List<String> nama = new ArrayList<>();
        List<Integer> nilai = new ArrayList<>();
        boolean adaPositif = false;
        for (Barang barang : daftar) {
            int saldo = barang.hitungSaldo();
            nama.add(barang.getNama());
            nilai.add(saldo);
            adaPositif |= saldo > 0;
        }
        if (daftar.isEmpty() || !adaPositif) {
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
