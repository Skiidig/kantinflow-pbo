package kantinflow.gui;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
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
    private final ButtonGroup grupNavigasi = new ButtonGroup();
    private final Map<String, JToggleButton> tombolNavigasi = new LinkedHashMap<>();
    private final Map<String, JScrollPane> gulirHalaman = new LinkedHashMap<>();
    private final Set<String> panelPerluSegar = new HashSet<>();
    private String halamanAktif;
    private LocalDate tanggalPemantauan;
    private boolean sudahDitutup;
    private boolean sedangMemproses;
    private final FadeOverlay fade = new FadeOverlay();

    public JendelaKantinFlow(PenyimpananSQLite penyimpanan) {
        super("KantinFlow");
        if (penyimpanan == null) {
            throw new IllegalArgumentException("Penyimpanan wajib diisi");
        }
        Tema.pasang();
        Ui.pasangTitleBar(getRootPane());
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
        Notifikasi.pasang(this);
        bangunTampilan();
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
        panelPerluSegar.addAll(panel.keySet());
        tampilkanHalaman(daftarBarang.isEmpty() ? "barang" : "ringkasan");
    }

    private JPanel buatSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(Ui.KARTU);
        sidebar.setPreferredSize(new Dimension(Ui.skala(Ui.LEBAR_SIDEBAR), 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Ui.GARIS));

        // Header logo dipisahkan garis hairline #E4E4E7
        JPanel merekBaris = new JPanel();
        merekBaris.setOpaque(false);
        merekBaris.setLayout(new BoxLayout(merekBaris, BoxLayout.X_AXIS));
        merekBaris.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Ui.GARIS),
                new EmptyBorder(Ui.skala(18), Ui.skala(18),
                        Ui.skala(16), Ui.skala(18))));
        JLabel merekMark = new JLabel("KF") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(Ui.AKSEN);
                    int arc = Ui.skala(8);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                } finally {
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        merekMark.setOpaque(false);
        merekMark.setForeground(Color.WHITE);
        merekMark.setFont(Ui.font(Font.BOLD, Ui.FONT_KECIL));
        merekMark.setHorizontalAlignment(SwingConstants.CENTER);
        Dimension ukuranMark = new Dimension(Ui.skala(30), Ui.skala(30));
        merekMark.setPreferredSize(ukuranMark);
        merekMark.setMinimumSize(ukuranMark);
        merekMark.setMaximumSize(ukuranMark);
        merekBaris.add(merekMark);
        merekBaris.add(Box.createHorizontalStrut(Ui.skala(10)));

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

        // Daftar navigasi dengan indikator aktif rounded pill lembut
        JPanel isi = new SidebarIsi();
        isi.setLayout(new BoxLayout(isi, BoxLayout.Y_AXIS));
        isi.setOpaque(false);
        isi.setBorder(new EmptyBorder(Ui.skala(14), Ui.skala(12),
                Ui.skala(14), Ui.skala(12)));

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
            TombolNavigasi tombol = new TombolNavigasi(item[1], item[0]);
            tombol.setName("nav-" + item[0]);
            tombol.addActionListener(event -> tampilkanHalaman(item[0]));
            tombol.addItemListener(event -> aturGayaNav(tombol));
            grupNavigasi.add(tombol);
            tombolNavigasi.put(item[0], tombol);
            isi.add(tombol);
            isi.add(Box.createVerticalStrut(Ui.skala(4)));
        }

        JScrollPane scroll = new JScrollPane(isi,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(true);
        scroll.getViewport().setBackground(Ui.KARTU);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        // Footer versi dipisahkan garis hairline #E4E4E7
        JLabel footer = new JLabel("KantinFlow · v1.0");
        footer.setFont(Ui.font(Font.PLAIN, Ui.FONT_FOOTER));
        footer.setForeground(Ui.TEKS_LEMBUT);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Ui.GARIS),
                new EmptyBorder(Ui.skala(13), Ui.skala(18),
                        Ui.skala(13), Ui.skala(18))));

        sidebar.add(merekBaris, BorderLayout.NORTH);
        sidebar.add(scroll, BorderLayout.CENTER);
        sidebar.add(footer, BorderLayout.SOUTH);
        return sidebar;
    }

    private void aturGayaNav(JToggleButton tombol) {
        String kunci = (String) tombol.getClientProperty("navIcon");
        if (tombol.isSelected()) {
            tombol.setForeground(Ui.TEKS);
            tombol.setFont(Ui.fontSemiBold(Ui.FONT_NAV));
            tombol.setIcon(Ui.ikonMenu(kunci, 17, Ui.TEKS));
        } else {
            tombol.setForeground(Ui.TEKS_LEMBUT);
            tombol.setFont(Ui.font(Font.PLAIN, Ui.FONT_NAV));
            tombol.setIcon(Ui.ikonMenu(kunci, 17, Ui.TEKS_LEMBUT));
        }
        tombol.repaint();
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
                    header);
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
        JPanel header = new JPanel(new BorderLayout(Ui.skala(16), 0));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(
                Ui.skala(24), Ui.skala(24), 0, Ui.skala(24)));

        JPanel kiri = new JPanel();
        kiri.setOpaque(false);
        kiri.setLayout(new BoxLayout(kiri, BoxLayout.Y_AXIS));
        JLabel judul = new JLabel(halaman.getJudul());
        judul.setFont(Ui.font(Font.BOLD, Ui.FONT_JUDUL));
        judul.setForeground(Ui.TEKS);
        judul.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel keterangan = new JLabel(halaman.getKeterangan());
        keterangan.setFont(Ui.font(Font.PLAIN, Ui.FONT_SUBJUDUL));
        keterangan.setForeground(Ui.TEKS_LEMBUT);
        keterangan.setAlignmentX(Component.LEFT_ALIGNMENT);
        kiri.add(judul);
        kiri.add(Box.createVerticalStrut(Ui.skala(4)));
        kiri.add(keterangan);
        header.add(kiri, BorderLayout.CENTER);

        JComponent aksiHeader = halaman.getAksiHeader();
        if (aksiHeader != null) {
            JPanel kanan = new JPanel(new GridBagLayout());
            kanan.setOpaque(false);
            GridBagConstraints c = new GridBagConstraints();
            c.anchor = GridBagConstraints.EAST;
            kanan.add(aksiHeader, c);
            header.add(kanan, BorderLayout.EAST);
        }
        return header;
    }

    private void tampilkanHalaman(String kunci) {
        if (kunci == null || !panel.containsKey(kunci)) {
            return;
        }
        halamanAktif = kunci;
        segarkanBilaPerlu(kunci);
        cardLayout.show(kartu, kunci);
        JScrollPane gulir = gulirHalaman.get(kunci);
        if (gulir != null) {
            gulir.getViewport().setViewPosition(new java.awt.Point(0, 0));
        }
        JToggleButton tombol = tombolNavigasi.get(kunci);
        if (tombol != null && !tombol.isSelected()) {
            tombol.setSelected(true);
        }
        fade.mulai();
    }

    public void tampilkan() {
        setVisible(true);
    }

    public JFrame getFrame() {
        return this;
    }

    public void tutup() {
        if (sudahDitutup) {
            return;
        }
        sudahDitutup = true;
        fade.hentikan();
        Notifikasi.bersihkan();
        Notifikasi.pasang(null);
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
        if (target == null || sudahDitutup) {
            return List.of();
        }
        try {
            Barang acuan = cariBarangTerbaru(target);
            return penyimpanan.muatRiwayat(acuan);
        } catch (IllegalArgumentException | PenyimpananException exception) {
            tampilkanPesan("Gagal memuat riwayat: " + exception.getMessage(),
                    false);
            return List.of();
        }
    }

    private Barang cariBarangTerbaru(Barang target) {
        int id = target.getId();
        if (id > 0) {
            for (Barang item : daftarBarang) {
                if (item.getId() == id) {
                    return item;
                }
            }
            return penyimpanan.muatBarang(id);
        }
        return target;
    }

    @Override
    public LocalDate getTanggalPemantauan() {
        return tanggalPemantauan;
    }

    @Override
    public void setTanggalPemantauan(LocalDate tanggal) {
        this.tanggalPemantauan = tanggal;
        tandaiPerluSegar("ringkasan", "stok-masuk", "stok-keluar",
                "pemantauan", "pembuangan");
        segarkanBilaPerlu(halamanAktif);
    }

    @Override
    public void bukaHalaman(String kunci) {
        if (kunci != null && panel.containsKey(kunci)) {
            tampilkanHalaman(kunci);
        }
    }

    @Override
    public void bukaPemantauanStatus(StatusKedaluwarsa status) {
        PanelKantin halaman = panel.get("pemantauan");
        if (halaman instanceof PanelPemantauan pemantauan) {
            panelPerluSegar.remove("pemantauan");
            pemantauan.terapkanFilterStatus(status);
        }
        tampilkanHalaman("pemantauan");
    }

    @Override
    public boolean bukaPembuanganDariPemantauan(
            int idBarangTarget, String kodeBatch, LocalDate tanggal) {
        return jalankanOperasiTulis(() -> bukaPembuanganInternal(
                idBarangTarget, kodeBatch, tanggal));
    }

    private boolean bukaPembuanganInternal(
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
        segarkanBilaPerlu("pembuangan");
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
        if (target == null || sudahDitutup) {
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
        return jalankanOperasiTulis(() -> {
            if (target == null) {
                tampilkanPesan("Pilih barang terlebih dahulu.", false);
                return false;
            }
            try {
                penyimpanan.simpanTransaksi(target, transaksi);
            } catch (PenyimpananException exception) {
                boolean dimuat = muatUlangInternal();
                tampilkanPesan("Penyimpanan gagal: " + exception.getMessage()
                        + (dimuat ? ". Tampilan dimuat ulang dari database." : "."),
                        false);
                return false;
            }
            if (!muatUlangInternal()) {
                return false;
            }
            tampilkanPesan(pesanSukses, true);
            return true;
        });
    }

    @Override
    public boolean tambahBarang(String nama, String satuan) {
        return jalankanOperasiTulis(() -> {
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
        });
    }

    @Override
    public boolean simpanStokMinimum(Barang target, int minimum) {
        return jalankanOperasiTulis(() -> {
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
                if (!muatUlangInternal()) {
                    return false;
                }
                tampilkanPesan("Stok minimum " + target.getNama() + " disimpan: "
                        + minimum + " " + target.getSatuan() + ".", true);
                return true;
            } catch (IllegalArgumentException | PenyimpananException exception) {
                tampilkanPesan("Gagal menyimpan stok minimum: "
                        + exception.getMessage(), false);
                return false;
            }
        });
    }

    @Override
    public boolean muatUlang() {
        return jalankanOperasiTulis(this::muatUlangInternal);
    }

    private boolean muatUlangInternal() {
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

    private boolean jalankanOperasiTulis(BooleanSupplier aksi) {
        if (sudahDitutup || sedangMemproses) {
            return false;
        }
        sedangMemproses = true;
        Cursor kursorSebelum = getCursor();
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            return aksi.getAsBoolean();
        } finally {
            setCursor(kursorSebelum);
            sedangMemproses = false;
        }
    }

    @Override
    public void tampilkanPesan(String pesan, boolean sukses) {
        if (sudahDitutup) {
            return;
        }
        if (sukses) {
            Notifikasi.sukses(pesan);
        } else {
            Notifikasi.gagal(pesan);
        }
    }

    private void perbaruiSemua() {
        panelPerluSegar.addAll(panel.keySet());
        segarkanBilaPerlu(halamanAktif);
    }

    private void tandaiPerluSegar(String... daftarKunci) {
        for (String kunci : daftarKunci) {
            if (kunci != null && panel.containsKey(kunci)) {
                panelPerluSegar.add(kunci);
            }
        }
    }

    private void segarkanBilaPerlu(String kunci) {
        if (kunci == null) {
            return;
        }
        if (panelPerluSegar.remove(kunci)) {
            PanelKantin halaman = panel.get(kunci);
            if (halaman != null) {
                halaman.perbarui();
            }
        }
    }

    private static final class HalamanScroll extends JPanel
            implements Scrollable {
        private final JPanel kolom;

        private HalamanScroll(PanelKantin halaman, JPanel header) {
            super(null);
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
            int lebar = getWidth();
            int tinggi = Math.max(getHeight(),
                    kolom.getPreferredSize().height);
            kolom.setBounds(0, 0, lebar, tinggi);
        }

        @Override
        public Dimension getPreferredSize() {
            return kolom.getPreferredSize();
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
            if (sudahDitutup || !isDisplayable()) {
                return;
            }
            alpha = 1f;
            setVisible(true);
            timer.restart();
        }

        private void hentikan() {
            timer.stop();
            alpha = 0f;
            setVisible(false);
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

    private static final class TombolNavigasi extends JToggleButton {
        private static final long serialVersionUID = 1L;
        private boolean hover;

        private TombolNavigasi(String label, String kunci) {
            super(label);
            putClientProperty("navIcon", kunci);
            setIcon(Ui.ikonMenu(kunci, 17, Ui.TEKS_LEMBUT));
            setIconTextGap(Ui.skala(10));
            setFont(Ui.font(Font.PLAIN, Ui.FONT_NAV));
            setForeground(Ui.TEKS_LEMBUT);
            setHorizontalAlignment(SwingConstants.LEFT);
            int tinggi = Ui.skala(38);
            setPreferredSize(new Dimension(0, tinggi));
            setMinimumSize(new Dimension(0, tinggi));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, tinggi));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setOpaque(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorder(new EmptyBorder(Ui.skala(8), Ui.skala(12),
                    Ui.skala(8), Ui.skala(12)));
            setCursor(java.awt.Cursor.getPredefinedCursor(
                    java.awt.Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent event) {
                    hover = true;
                    if (!isSelected()) {
                        setForeground(Ui.TEKS);
                        setIcon(Ui.ikonMenu(kunci, 17, Ui.TEKS));
                    }
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    hover = false;
                    if (!isSelected()) {
                        setForeground(Ui.TEKS_LEMBUT);
                        setIcon(Ui.ikonMenu(kunci, 17, Ui.TEKS_LEMBUT));
                    }
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            if (isSelected() || hover) {
                Graphics2D g2 = (Graphics2D) graphics.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    int arc = Ui.skala(8);
                    g2.setColor(isSelected() ? Ui.NAV_AKTIF_BG : Ui.HOVER);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                } finally {
                    g2.dispose();
                }
            }
            super.paintComponent(graphics);
        }
    }
}
