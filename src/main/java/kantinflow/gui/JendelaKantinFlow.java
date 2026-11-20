package kantinflow.gui;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.JComponent;
import javax.swing.border.EmptyBorder;

import kantinflow.Barang;
import kantinflow.BatchStok;
import kantinflow.StatusKedaluwarsa;
import kantinflow.Transaksi;
import kantinflow.penyimpanan.PenyimpananException;
import kantinflow.penyimpanan.PenyimpananSQLite;

/**
 * Shell aplikasi: sidebar, CardLayout, dan status bersama. Setiap halaman
 * memiliki pilihan barangnya sendiri; shell hanya menyediakan daftar dan
 * operasi penyimpanan.
 */
public class JendelaKantinFlow extends JFrame implements KonteksKantin {
    private final PenyimpananSQLite penyimpanan;
    private final List<Barang> daftarBarang;
    private final Map<String, PanelKantin> panel = new LinkedHashMap<>();
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel kartu = new JPanel(cardLayout);
    private final JTextArea status = new JTextArea(2, 40);
    private final ButtonGroup grupNavigasi = new ButtonGroup();
    private final Map<String, JToggleButton> tombolNavigasi = new LinkedHashMap<>();
    private final Map<String, JScrollPane> gulirHalaman = new LinkedHashMap<>();
    private JPanel indikatorNav;
    private JPanel pembungkusNav;
    private String halamanAktif = "ringkasan";
    private LocalDate tanggalPemantauan;
    private boolean sudahDitutup;
    private final FadeOverlay fade = new FadeOverlay();

    public JendelaKantinFlow(PenyimpananSQLite penyimpanan) {
        super("KantinFlow");
        if (penyimpanan == null) {
            throw new IllegalArgumentException("Penyimpanan wajib diisi");
        }
        Tema.pasang();
        this.penyimpanan = penyimpanan;
        this.daftarBarang = new ArrayList<>(penyimpanan.muatSemuaBarang());

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                tutup();
            }
        });
        setMinimumSize(new Dimension(Ui.LEBAR_MINIMUM, Ui.TINGGI_MINIMUM));
        setSize(Ui.LEBAR_JENDELA, Ui.TINGGI_JENDELA);
        setLocationRelativeTo(null);

        setGlassPane(fade);
        fade.setVisible(false);
        bangunTampilan();
        Notifikasi.pasang(this);
        perbaruiSemua();
        if (daftarBarang.isEmpty()) {
            tampilkanPesan("Belum ada barang. Tambahkan barang melalui halaman Barang.", false);
        } else {
            tampilkanPesan("Data dan riwayat dimuat dari database.", true);
        }
    }

    private void bangunTampilan() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Ui.LATAR);
        root.add(buatSidebar(), BorderLayout.WEST);
        root.add(buatKonten(), BorderLayout.CENTER);
        setContentPane(root);
        tampilkanHalaman(daftarBarang.isEmpty() ? "barang" : "ringkasan");
    }

    private JPanel buatSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(Ui.KARTU);
        sidebar.setPreferredSize(new Dimension(Ui.skala(Ui.LEBAR_SIDEBAR), 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Ui.GARIS));

        JPanel isi = new SidebarIsi();
        isi.setLayout(new BoxLayout(isi, BoxLayout.Y_AXIS));
        isi.setOpaque(false);
        isi.setBorder(new EmptyBorder(20, 16, 8, 16));

        JPanel merekBaris = new JPanel();
        merekBaris.setOpaque(false);
        merekBaris.setLayout(new BoxLayout(merekBaris, BoxLayout.X_AXIS));
        JLabel merekMark = new JLabel("KF");
        merekMark.setOpaque(true);
        merekMark.setBackground(Ui.AKSEN);
        merekMark.setForeground(Color.WHITE);
        merekMark.setFont(Ui.font(Font.BOLD, Ui.FONT_KECIL));
        merekMark.setHorizontalAlignment(SwingConstants.CENTER);
        merekMark.setPreferredSize(new Dimension(Ui.skala(32), Ui.skala(32)));
        merekMark.setMaximumSize(new Dimension(Ui.skala(32), Ui.skala(32)));
        merekMark.setBorder(new EmptyBorder(2, 0, 2, 0));
        merekBaris.add(merekMark);
        merekBaris.add(Box.createHorizontalStrut(10));
        JPanel merekTeks = new JPanel();
        merekTeks.setOpaque(false);
        merekTeks.setLayout(new BoxLayout(merekTeks, BoxLayout.Y_AXIS));
        JLabel merek = new JLabel("KantinFlow");
        merek.setFont(Ui.font(Font.BOLD, Ui.FONT_BRAND));
        merek.setForeground(Ui.TEKS);
        JLabel sub = new JLabel("Manajemen Persediaan");
        sub.setFont(Ui.font(Font.PLAIN, Ui.FONT_FOOTER));
        sub.setForeground(Ui.TEKS_LEMBUT);
        merekTeks.add(merek);
        merekTeks.add(Box.createVerticalStrut(1));
        merekTeks.add(sub);
        merekBaris.add(merekTeks);
        merekBaris.setAlignmentX(Component.LEFT_ALIGNMENT);
        isi.add(merekBaris);
        isi.add(Box.createVerticalStrut(22));

        JPanel nav = new JPanel();
        nav.setOpaque(false);
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        String[][] halaman = {
            {"ringkasan", "Ringkasan"},
            {"barang", "Barang"},
            {"stok-masuk", "Stok Masuk"},
            {"stok-keluar", "Stok Keluar"},
            {"pemantauan", "Pemantauan"},
            {"pembuangan", "Pembuangan"},
            {"riwayat", "Riwayat"}
        };
        for (String[] item : halaman) {
            JToggleButton tombol = new JToggleButton(item[1]);
            tombol.setName("nav-" + item[0]);
            tombol.putClientProperty("navIcon", item[0]);
            tombol.setIcon(Ui.ikonMenu(item[0], 20, Ui.TEKS_LEMBUT));
            tombol.setIconTextGap(10);
            tombol.setFont(Ui.font(Font.PLAIN, Ui.FONT_NAV));
            tombol.setHorizontalAlignment(SwingConstants.LEFT);
            tombol.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
            tombol.setAlignmentX(Component.LEFT_ALIGNMENT);
            tombol.setFocusPainted(true);
            tombol.setOpaque(true);
            tombol.setContentAreaFilled(false);
            tombol.setBorder(aturBorderNav(false));
            tombol.addActionListener(event -> tampilkanHalaman(item[0]));
            tombol.addItemListener(event -> aturGayaNav(tombol));
            tombol.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent event) {
                    if (!tombol.isSelected()) {
                        tombol.setBackground(Ui.HOVER);
                    }
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    if (!tombol.isSelected()) {
                        tombol.setBackground(Ui.KARTU);
                    }
                }
            });
            grupNavigasi.add(tombol);
            tombolNavigasi.put(item[0], tombol);
            nav.add(tombol);
            nav.add(Box.createVerticalStrut(3));
        }
        nav.setAlignmentX(Component.LEFT_ALIGNMENT);
        pembungkusNav = new JPanel(null);
        pembungkusNav.setOpaque(false);
        pembungkusNav.setAlignmentX(Component.LEFT_ALIGNMENT);
        pembungkusNav.add(nav);
        indikatorNav = new JPanel();
        indikatorNav.setBackground(Ui.AKSEN);
        indikatorNav.setOpaque(true);
        pembungkusNav.add(indikatorNav);
        pembungkusNav.setPreferredSize(new Dimension(0,
                nav.getPreferredSize().height));
        pembungkusNav.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                nav.getPreferredSize().height));
        isi.add(pembungkusNav);

        JScrollPane scroll = new JScrollPane(isi,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(true);
        scroll.getViewport().setBackground(Ui.KARTU);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JLabel footer = new JLabel("KantinFlow · v1.0");
        footer.setFont(Ui.font(Font.PLAIN, Ui.FONT_FOOTER));
        footer.setForeground(Ui.TEKS_LEMBUT);
        footer.setBorder(new EmptyBorder(10, 16, 14, 16));

        pembungkusNav.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent event) {
                int lebar = pembungkusNav.getWidth();
                int tinggi = pembungkusNav.getHeight();
                nav.setBounds(0, 0, lebar, tinggi);
                if (indikatorNav.getWidth() <= 0) {
                    indikatorNav.setBounds(0, 0, Ui.skala(3), Ui.skala(30));
                }
                geserIndikator();
            }
        });
        isi.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent event) {
                geserIndikator();
            }
        });
        sidebar.add(scroll, BorderLayout.CENTER);
        sidebar.add(footer, BorderLayout.SOUTH);
        return sidebar;
    }

    private javax.swing.border.Border aturBorderNav(boolean aktif) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0,
                        aktif ? Ui.AKSEN : Ui.KARTU),
                new EmptyBorder(8, 9, 8, 10));
    }

    private void aturGayaNav(JToggleButton tombol) {
        String kunci = (String) tombol.getClientProperty("navIcon");
        if (tombol.isSelected()) {
            tombol.setBackground(Ui.AKSEN_LEMBUT);
            tombol.setForeground(Ui.AKSEN);
            tombol.setFont(Ui.font(Font.BOLD, Ui.FONT_NAV));
            tombol.setBorder(aturBorderNav(true));
            tombol.setIcon(Ui.ikonMenu(kunci, 20, Ui.AKSEN));
        } else {
            tombol.setBackground(Ui.KARTU);
            tombol.setForeground(Ui.TEKS_LEMBUT);
            tombol.setFont(Ui.font(Font.PLAIN, Ui.FONT_NAV));
            tombol.setBorder(aturBorderNav(false));
            tombol.setIcon(Ui.ikonMenu(kunci, 20, Ui.TEKS_LEMBUT));
        }
    }

    private JPanel buatKonten() {
        kartu.setOpaque(false);
        panel.put("ringkasan", new PanelRingkasan(this));
        panel.put("barang", new PanelBarang(this));
        panel.put("stok-masuk", new PanelStokMasuk(this));
        panel.put("stok-keluar", new PanelStokKeluar(this));
        panel.put("pemantauan", new PanelPemantauan(this));
        panel.put("pembuangan", new PanelPembuangan(this));
        panel.put("riwayat", new PanelRiwayat(this));

        for (Map.Entry<String, PanelKantin> entry : panel.entrySet()) {
            JPanel header = buatHeaderHalaman(
                    entry.getKey(), entry.getValue());
            HalamanScroll tampilan = new HalamanScroll(entry.getValue(),
                    header, entry.getValue().lebarMaksimum());
            JScrollPane scroll = new JScrollPane(tampilan,
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setBorder(null);
            scroll.getViewport().setBackground(Ui.LATAR);
            scroll.getVerticalScrollBar().setUnitIncrement(16);
            gulirHalaman.put(entry.getKey(), scroll);
            kartu.add(scroll, entry.getKey());
        }

        JPanel konten = new JPanel(new BorderLayout());
        konten.setBackground(Ui.LATAR);
        konten.add(kartu, BorderLayout.CENTER);
        return konten;
    }

    /**
     * Header bersama per halaman. Header berada di dalam area scroll dan
     * memakai layout manager, bukan posisi absolut.
     */
    private JPanel buatHeaderHalaman(String kunci, PanelKantin halaman) {
        JPanel header = new JPanel(new GridBagLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(
                Ui.skala(24), Ui.skala(24), 0, Ui.skala(24)));

        JLabel ikon = new JLabel(Ui.ikonMenu(kunci, 24, Ui.AKSEN));
        ikon.setVerticalAlignment(SwingConstants.CENTER);
        JLabel judul = new JLabel(halaman.getJudul());
        judul.setFont(Ui.font(Font.BOLD, Ui.FONT_JUDUL));
        judul.setForeground(Ui.TEKS);
        JLabel keterangan = new JLabel(halaman.getKeterangan());
        keterangan.setFont(Ui.font(Font.PLAIN, Ui.FONT_CATATAN));
        keterangan.setForeground(Ui.TEKS_LEMBUT);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 0, Ui.skala(12));
        header.add(ikon, c);

        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 0, 0);
        header.add(judul, c);

        c.gridy = 1;
        c.insets = new Insets(Ui.skala(6), 0, 0, 0);
        header.add(keterangan, c);
        return header;
    }

    private void tampilkanHalaman(String kunci) {
        halamanAktif = kunci;
        cardLayout.show(kartu, kunci);
        JScrollPane gulir = gulirHalaman.get(kunci);
        if (gulir != null) {
            gulir.getViewport().setViewPosition(new java.awt.Point(0, 0));
        }
        JToggleButton tombol = tombolNavigasi.get(kunci);
        if (tombol != null) {
            tombol.setSelected(true);
        }
        geserIndikator();
        fade.mulai();
    }

    private void geserIndikator() {
        if (indikatorNav == null || pembungkusNav == null) {
            return;
        }
        JToggleButton tombol = tombolNavigasi.get(halamanAktif);
        if (tombol == null) {
            return;
        }
        int target = tombol.getY() + Math.max(0,
                (tombol.getHeight() - Ui.skala(30)) / 2);
        int awal = indikatorNav.getY();
        indikatorNav.setBounds(0, awal, Ui.skala(3), Ui.skala(30));
        final int[] langkah = {0};
        final int jumlah = 8;
        Timer timer = new Timer(15, event -> {
            langkah[0]++;
            double t = Math.min(1.0, langkah[0] / (double) jumlah);
            int y = (int) Math.round(awal + (target - awal) * t);
            indikatorNav.setBounds(0, y, Ui.skala(3), Ui.skala(30));
            indikatorNav.repaint();
            if (langkah[0] >= jumlah) {
                ((Timer) event.getSource()).stop();
            }
        });
        timer.start();
    }

    public void tampilkan() {
        setVisible(true);
        SwingUtilities.invokeLater(this::geserIndikator);
    }

    public JFrame getFrame() {
        return this;
    }

    public void tutup() {
        if (sudahDitutup) {
            return;
        }
        sudahDitutup = true;
        try {
            penyimpanan.close();
        } catch (PenyimpananException exception) {
            System.err.println("Gagal menutup database: "
                    + exception.getMessage());
        } finally {
            dispose();
        }
    }

    @Override
    public List<Barang> getDaftarBarang() {
        return Collections.unmodifiableList(daftarBarang);
    }

    @Override
    public List<Transaksi> getRiwayat(Barang target) {
        if (target == null) {
            return List.of();
        }
        try {
            Barang terbaru = penyimpanan.muatBarang(target.getId());
            return penyimpanan.muatRiwayat(terbaru);
        } catch (IllegalArgumentException | PenyimpananException exception) {
            tampilkanPesan("Gagal memuat riwayat: " + exception.getMessage(),
                    false);
            return List.of();
        }
    }

    @Override
    public LocalDate getTanggalPemantauan() {
        return tanggalPemantauan;
    }

    @Override
    public void setTanggalPemantauan(LocalDate tanggal) {
        this.tanggalPemantauan = tanggal;
        perbaruiPanel("ringkasan");
        perbaruiPanel("stok-masuk");
        perbaruiPanel("stok-keluar");
        perbaruiPanel("pemantauan");
        perbaruiPanel("pembuangan");
    }

    @Override
    public boolean bukaPembuanganDariPemantauan(
            int idBarangTarget, String kodeBatch, LocalDate tanggal) {
        if (idBarangTarget < 0 || kodeBatch == null
                || kodeBatch.isBlank() || tanggal == null) {
            tampilkanPesan("Batch dari Pemantauan tidak valid. "
                    + "Pilih batch lain di Pemantauan.", false);
            return false;
        }

        PanelKantin halaman = panel.get("pembuangan");
        if (!(halaman instanceof PanelPembuangan pembuangan)) {
            tampilkanPesan("Halaman Pembuangan tidak tersedia.", false);
            return false;
        }

        List<Barang> terbaru;
        try {
            terbaru = penyimpanan.muatSemuaBarang();
        } catch (PenyimpananException exception) {
            tampilkanPesan("Gagal memuat ulang data untuk Pembuangan: "
                    + exception.getMessage(), false);
            return false;
        }

        Barang target = null;
        for (Barang item : terbaru) {
            if (item.getId() == idBarangTarget) {
                target = item;
                break;
            }
        }
        if (target == null) {
            tampilkanPesan("Barang untuk batch ini sudah tidak tersedia. "
                    + "Pilih batch lain di Pemantauan.", false);
            return false;
        }

        BatchStok batch = null;
        for (BatchStok item : target.getDaftarBatch()) {
            if (item.getKode().equals(kodeBatch)) {
                batch = item;
                break;
            }
        }
        if (batch == null || batch.getSisaJumlah() <= 0) {
            tampilkanPesan("Batch " + kodeBatch + " sudah tidak tersedia "
                    + "atau sisanya sudah habis. Pilih batch lain di Pemantauan.",
                    false);
            return false;
        }
        if (batch.statusPada(tanggal) != StatusKedaluwarsa.KEDALUWARSA) {
            tampilkanPesan("Batch " + kodeBatch + " tidak berstatus kedaluwarsa "
                    + "pada tanggal pemeriksaan. Periksa ulang tanggal.", false);
            return false;
        }

        if (pembuangan.adaIsianBelumDisimpan()
                && !DialogKonfirmasi.tampilkan(this, "Ganti isian Pembuangan",
                        "Formulir Pembuangan masih berisi isian yang belum "
                        + "disimpan. Ganti isian tersebut dengan batch "
                        + kodeBatch + "?",
                        "Buka Pembuangan")) {
            tampilkanPesan("Pembukaan Pembuangan dibatalkan. "
                    + "Isian formulir dipertahankan.", true);
            return false;
        }

        daftarBarang.clear();
        daftarBarang.addAll(terbaru);
        perbaruiSemua();
        if (!pembuangan.siapkanDariPemantauan(
                idBarangTarget, kodeBatch, tanggal)) {
            tampilkanPesan("Formulir Pembuangan tidak dapat diisi untuk batch "
                    + kodeBatch + ". Pilih batch lain di Pemantauan.", false);
            return false;
        }

        tampilkanHalaman("pembuangan");
        tampilkanPesan("Pembuangan siap untuk batch " + kodeBatch + " pada "
                + FormatTanggal.format(tanggal)
                + ". Isi jumlah (alasan opsional untuk batch kedaluwarsa), lalu tekan Buang Stok.", true);
        return true;
    }

    @Override
    public String kodeBatchOtomatis(Barang target, LocalDate tanggalMasuk) {
        if (target == null) {
            return null;
        }
        try {
            return penyimpanan.kodeBatchBerikutnya(target, tanggalMasuk);
        } catch (RuntimeException exception) {
            tampilkanPesan("Gagal membentuk kode batch: "
                    + exception.getMessage(), false);
            return null;
        }
    }

    @Override
    public boolean simpanTransaksi(Barang target, Transaksi transaksi,
            String pesanSukses) {
        if (target == null) {
            tampilkanPesan("Pilih barang terlebih dahulu.", false);
            return false;
        }
        try {
            penyimpanan.simpanTransaksi(target, transaksi);
        } catch (PenyimpananException exception) {
            boolean dimuat = muatUlang();
            tampilkanPesan("Penyimpanan gagal: " + exception.getMessage()
                    + (dimuat ? ". Tampilan dimuat ulang dari database." : "."),
                    false);
            return false;
        }
        if (!muatUlang()) {
            return false;
        }
        tampilkanPesan(pesanSukses, true);
        return true;
    }

    @Override
    public boolean tambahBarang(String nama, String satuan) {
        try {
            Barang baru = penyimpanan.simpanBarang(new Barang(nama, satuan));
            daftarBarang.add(baru);
            perbaruiSemua();
            tampilkanHalaman("barang");
            tampilkanPesan("Barang ditambahkan: " + baru.getNama()
                    + " (" + baru.getSatuan() + ").", true);
            return true;
        } catch (IllegalArgumentException | PenyimpananException exception) {
            tampilkanPesan("Tambah barang gagal: " + exception.getMessage(), false);
            return false;
        }
    }

    @Override
    public boolean simpanStokMinimum(Barang target, int minimum) {
        if (target == null) {
            tampilkanPesan("Pilih barang yang akan diatur minimumnya terlebih dahulu.", false);
            return false;
        }
        if (minimum < 0) {
            tampilkanPesan("Stok minimum tidak boleh negatif.", false);
            return false;
        }
        try {
            penyimpanan.simpanStokMinimum(target, minimum);
            muatUlang();
            tampilkanPesan("Stok minimum " + target.getNama() + " disimpan: "
                    + minimum + " " + target.getSatuan() + ".", true);
            return true;
        } catch (IllegalArgumentException | PenyimpananException exception) {
            tampilkanPesan("Gagal menyimpan stok minimum: "
                    + exception.getMessage(), false);
            return false;
        }
    }

    @Override
    public boolean muatUlang() {
        try {
            List<Barang> terbaru = penyimpanan.muatSemuaBarang();
            daftarBarang.clear();
            daftarBarang.addAll(terbaru);
            perbaruiSemua();
            return true;
        } catch (PenyimpananException exception) {
            tampilkanPesan("Gagal memuat ulang data: "
                    + exception.getMessage(), false);
            return false;
        }
    }

    @Override
    public void tampilkanPesan(String pesan, boolean sukses) {
        status.setText(pesan);
        status.setForeground(sukses ? Ui.HIJAU : Ui.MERAH);
        status.setCaretPosition(0);
        if (sukses) {
            Notifikasi.sukses(pesan);
        } else {
            Notifikasi.gagal(pesan);
        }
    }

    private void perbaruiSemua() {
        for (PanelKantin halaman : panel.values()) {
            halaman.perbarui();
        }
    }

    private void perbaruiPanel(String kunci) {
        PanelKantin halaman = panel.get(kunci);
        if (halaman != null) {
            halaman.perbarui();
        }
    }

    private static final class HalamanScroll extends JPanel
            implements Scrollable {
        private final JPanel kolom;
        private final int maksimum;

        private HalamanScroll(PanelKantin halaman, JPanel header,
                int maksimum) {
            super(null);
            this.maksimum = maksimum;
            setOpaque(false);
            kolom = new JPanel();
            kolom.setOpaque(false);
            kolom.setLayout(new BoxLayout(kolom, BoxLayout.Y_AXIS));
            header.setAlignmentX(Component.LEFT_ALIGNMENT);
            halaman.setAlignmentX(Component.LEFT_ALIGNMENT);
            kolom.add(header);
            kolom.add(halaman);
            add(kolom);
        }

        @Override
        public void doLayout() {
            int lebar = Math.min(getWidth(), Ui.skala(maksimum));
            int tinggi = Math.max(getHeight(),
                    kolom.getPreferredSize().height);
            kolom.setBounds(0, 0, lebar, tinggi);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension isi = kolom.getPreferredSize();
            return new Dimension(Math.min(isi.width, Ui.skala(maksimum)),
                    isi.height);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(
                Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(
                Rectangle visibleRect, int orientation, int direction) {
            return 96;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private final class FadeOverlay extends JPanel {
        private float alpha;
        private final Timer timer;

        private FadeOverlay() {
            setOpaque(false);
            setFocusable(false);
            timer = new Timer(15, event -> {
                alpha -= 0.1f;
                if (alpha <= 0f) {
                    alpha = 0f;
                    setVisible(false);
                    ((Timer) event.getSource()).stop();
                }
                repaint();
            });
        }

        private void mulai() {
            if (!isDisplayable()) {
                return;
            }
            alpha = 1f;
            setVisible(true);
            timer.restart();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            if (alpha <= 0f) {
                return;
            }
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                g2.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, Math.max(0f, alpha)));
                g2.setColor(Ui.LATAR);
                g2.fillRect(0, 0, getWidth(), getHeight());
            } finally {
                g2.dispose();
            }
        }
    }

    private static final class SidebarIsi extends JPanel implements Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(
                Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(
                Rectangle visibleRect, int orientation, int direction) {
            return 96;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
