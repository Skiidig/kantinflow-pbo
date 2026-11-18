package kantinflow.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import kantinflow.Barang;
import kantinflow.Transaksi;
import kantinflow.penyimpanan.PenyimpananException;
import kantinflow.penyimpanan.PenyimpananSQLite;

/**
 * Shell aplikasi: sidebar, pemilih barang aktif, CardLayout, dan status bersama.
 * Panel halaman bertanggung jawab atas form masing-masing.
 */
public class JendelaKantinFlow extends JFrame implements KonteksKantin {
    private static final String KUNCI_PREFERENSI = "barangAktif";
    private final PenyimpananSQLite penyimpanan;
    private final List<Barang> daftarBarang;
    private final List<Transaksi> riwayat = new ArrayList<>();
    private final Map<String, PanelKantin> panel = new LinkedHashMap<>();
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel kartu = new JPanel(cardLayout);
    private final JComboBox<Barang> comboBarang = new JComboBox<>();
    private final JTextArea status = new JTextArea(2, 40);
    private final ButtonGroup grupNavigasi = new ButtonGroup();
    private final Map<String, JToggleButton> tombolNavigasi = new LinkedHashMap<>();
    private JLabel judulHalaman;
    private JLabel keteranganHalaman;
    private JLabel labelBarangKosong;
    private Barang barang;
    private LocalDate tanggalPemantauan;
    private boolean menggantiBarang;
    private boolean sudahDitutup;

    public JendelaKantinFlow(PenyimpananSQLite penyimpanan) {
        super("KantinFlow");
        if (penyimpanan == null) {
            throw new IllegalArgumentException("Penyimpanan wajib diisi");
        }
        this.penyimpanan = penyimpanan;
        this.daftarBarang = new ArrayList<>(penyimpanan.muatSemuaBarang());
        this.barang = pilihBarangAwal();
        if (barang != null) {
            this.riwayat.addAll(penyimpanan.muatRiwayat(barang));
        }

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

        bangunTampilan();
        perbaruiComboBarang();
        perbaruiSemua();
        if (barang == null) {
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
        tampilkanHalaman(barang == null ? "barang" : "ringkasan");
    }

    private JPanel buatSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(Ui.KARTU);
        sidebar.setPreferredSize(new Dimension(Ui.LEBAR_SIDEBAR, 0));
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
        merekMark.setPreferredSize(new Dimension(32, 32));
        merekMark.setMaximumSize(new Dimension(32, 32));
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
        isi.add(Box.createVerticalStrut(18));

        isi.add(buatPemilihBarang());
        isi.add(Box.createVerticalStrut(14));

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
        isi.add(nav);

        JScrollPane scroll = new JScrollPane(isi,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(true);
        scroll.getViewport().setBackground(Ui.KARTU);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JLabel footer = new JLabel("SQLite lokal · FlatLaf 3.8");
        footer.setFont(Ui.font(Font.PLAIN, Ui.FONT_FOOTER));
        footer.setForeground(Ui.TEKS_LEMBUT);
        footer.setBorder(new EmptyBorder(10, 16, 14, 16));

        sidebar.add(scroll, BorderLayout.CENTER);
        sidebar.add(footer, BorderLayout.SOUTH);
        return sidebar;
    }

    private JPanel buatPemilihBarang() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel label = Ui.labelField("Barang aktif");
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        comboBarang.setName("barangAktif");
        comboBarang.setFont(Ui.font(Font.PLAIN, Ui.FONT_BODY));
        comboBarang.setMaximumSize(new Dimension(Integer.MAX_VALUE, Ui.TINGGI_INPUT));
        comboBarang.setPreferredSize(new Dimension(Ui.LEBAR_SIDEBAR - 32,
                Ui.TINGGI_INPUT));
        comboBarang.setMinimumSize(new Dimension(80, Ui.TINGGI_INPUT));
        comboBarang.setAlignmentX(Component.LEFT_ALIGNMENT);
        comboBarang.addActionListener(event -> {
            if (!menggantiBarang) {
                Barang dipilih = (Barang) comboBarang.getSelectedItem();
                if (dipilih != null && (barang == null
                        || dipilih.getId() != barang.getId())) {
                    gantiBarang(dipilih);
                }
            }
        });
        comboBarang.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                if (value instanceof Barang item) {
                    setText(item.getNama() + " (" + item.getSatuan() + ")");
                }
                return this;
            }
        });
        labelBarangKosong = Ui.kosong(
                "Belum ada barang. Buka halaman Barang untuk menambahkan.");
        labelBarangKosong.setName("barangKosongSidebar");
        labelBarangKosong.setVisible(false);
        panel.add(label);
        panel.add(Box.createVerticalStrut(5));
        panel.add(comboBarang);
        panel.add(Box.createVerticalStrut(4));
        panel.add(labelBarangKosong);
        return panel;
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
            JScrollPane scroll = new JScrollPane(entry.getValue(),
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            scroll.setBorder(null);
            scroll.getViewport().setBackground(Ui.LATAR);
            scroll.getVerticalScrollBar().setUnitIncrement(16);
            kartu.add(scroll, entry.getKey());
        }

        status.setName("status");
        status.setEditable(false);
        status.setLineWrap(true);
        status.setWrapStyleWord(true);
        status.setFont(Ui.font(Font.PLAIN, Ui.FONT_STATUS));
        status.setBorder(new EmptyBorder(9, 26, 9, 26));
        status.setBackground(Ui.KARTU);
        status.setForeground(Ui.TEKS);
        status.setRows(2);

        JPanel konten = new JPanel(new BorderLayout());
        konten.setBackground(Ui.LATAR);
        konten.add(buatHeader(), BorderLayout.NORTH);
        konten.add(kartu, BorderLayout.CENTER);
        konten.add(status, BorderLayout.SOUTH);
        return konten;
    }

    private JPanel buatHeader() {
        JPanel header = new JPanel(new BorderLayout(24, 0));
        header.setBackground(Ui.KARTU);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Ui.GARIS),
                new EmptyBorder(16, 26, 15, 26)));

        JPanel kiri = new JPanel();
        kiri.setOpaque(false);
        kiri.setLayout(new BoxLayout(kiri, BoxLayout.Y_AXIS));
        JLabel eyebrow = new JLabel("KANTINFLOW · MANAJEMEN PERSEDIAAN");
        eyebrow.setFont(Ui.font(Font.BOLD, Ui.FONT_EYEBROW));
        eyebrow.setForeground(Ui.AKSEN);
        judulHalaman = new JLabel("Ringkasan");
        judulHalaman.setFont(Ui.font(Font.BOLD, Ui.FONT_JUDUL));
        judulHalaman.setForeground(Ui.TEKS);
        keteranganHalaman = new JLabel(" ");
        keteranganHalaman.setFont(Ui.font(Font.PLAIN, Ui.FONT_CATATAN));
        keteranganHalaman.setForeground(Ui.TEKS_LEMBUT);
        kiri.add(eyebrow);
        kiri.add(Box.createVerticalStrut(2));
        kiri.add(judulHalaman);
        kiri.add(Box.createVerticalStrut(2));
        kiri.add(keteranganHalaman);
        header.add(kiri, BorderLayout.CENTER);
        return header;
    }

    private void tampilkanHalaman(String kunci) {
        if (barang == null && !"barang".equals(kunci)) {
            kunci = "barang";
        }
        PanelKantin halaman = panel.get(kunci);
        if (halaman != null) {
            if (judulHalaman != null) {
                judulHalaman.setText(halaman.getJudul());
            }
            if (keteranganHalaman != null) {
                keteranganHalaman.setText(halaman.getKeterangan());
            }
        }
        cardLayout.show(kartu, kunci);
        JToggleButton tombol = tombolNavigasi.get(kunci);
        if (tombol != null) {
            tombol.setSelected(true);
        }
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
    public Barang getBarangAktif() {
        return barang;
    }

    @Override
    public List<Barang> getDaftarBarang() {
        return Collections.unmodifiableList(daftarBarang);
    }

    @Override
    public List<Transaksi> getRiwayat() {
        return Collections.unmodifiableList(riwayat);
    }

    @Override
    public LocalDate getTanggalPemantauan() {
        return tanggalPemantauan;
    }

    @Override
    public void setTanggalPemantauan(LocalDate tanggal) {
        this.tanggalPemantauan = tanggal;
        perbaruiPanel("ringkasan");
        perbaruiPanel("pemantauan");
    }

    @Override
    public boolean simpanTransaksi(Transaksi transaksi, String pesanSukses) {
        try {
            penyimpanan.simpanTransaksi(barang, transaksi);
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
            this.barang = baru;
            this.riwayat.clear();
            this.riwayat.addAll(penyimpanan.muatRiwayat(baru));
            this.tanggalPemantauan = null;
            perbaruiComboBarang();
            perbaruiSemua();
            simpanPreferensiBarang();
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
    public boolean simpanStokMinimum(int minimum) {
        if (barang == null) {
            tampilkanPesan("Belum ada barang aktif. Tambahkan barang terlebih dahulu.", false);
            return false;
        }
        if (minimum < 0) {
            tampilkanPesan("Stok minimum tidak boleh negatif.", false);
            return false;
        }
        try {
            penyimpanan.simpanStokMinimum(barang, minimum);
            barang.setStokMinimum(minimum);
            perbaruiSemua();
            tampilkanPesan("Stok minimum " + barang.getNama() + " disimpan: "
                    + minimum + " " + barang.getSatuan() + ".", true);
            return true;
        } catch (IllegalArgumentException | PenyimpananException exception) {
            tampilkanPesan("Gagal menyimpan stok minimum: "
                    + exception.getMessage(), false);
            return false;
        }
    }

    @Override
    public void gantiBarang(Barang dipilih) {
        if (dipilih == null || (barang != null && dipilih.getId() == barang.getId())) {
            return;
        }
        if (barang != null && adaIsianTransaksiBelumDisimpan()
                && !konfirmasiGantiBarang(dipilih)) {
            kembalikanPilihanBarang();
            tampilkanPesanNetral(
                    "Perpindahan barang dibatalkan. Isian formulir dipertahankan.");
            return;
        }
        try {
            Barang terbaru = penyimpanan.muatBarang(dipilih.getId());
            gantiReferensiBarang(terbaru);
            this.barang = terbaru;
            this.riwayat.clear();
            this.riwayat.addAll(penyimpanan.muatRiwayat(terbaru));
            this.tanggalPemantauan = null;
            perbaruiComboBarang();
            perbaruiSemua();
            simpanPreferensiBarang();
            tampilkanPesan("Barang aktif: " + terbaru.getNama()
                    + " (" + terbaru.getSatuan() + ").", true);
        } catch (PenyimpananException exception) {
            tampilkanPesan("Gagal mengganti barang: "
                    + exception.getMessage(), false);
            perbaruiComboBarang();
        }
    }

    @Override
    public boolean muatUlang() {
        try {
            Barang terbaru = penyimpanan.muatBarang(barang.getId());
            gantiReferensiBarang(terbaru);
            this.barang = terbaru;
            this.riwayat.clear();
            this.riwayat.addAll(penyimpanan.muatRiwayat(terbaru));
            perbaruiComboBarang();
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
    }

    private Barang pilihBarangAwal() {
        if (daftarBarang.isEmpty()) {
            return null;
        }
        try {
            int id = bacaPreferensiBarang();
            for (Barang item : daftarBarang) {
                if (item.getId() == id) {
                    return item;
                }
            }
        } catch (RuntimeException exception) {
            // Preferensi UI opsional; fallback ke barang pertama.
        }
        return daftarBarang.get(0);
    }

    private Path filePreferensiBarang() {
        String lokasi = penyimpanan.getLokasiDatabase();
        if (lokasi == null || lokasi.isBlank() || ":memory:".equals(lokasi)) {
            return null;
        }
        Path database = Path.of(lokasi).toAbsolutePath().normalize();
        return database.resolveSibling(database.getFileName() + ".ui");
    }

    private int bacaPreferensiBarang() {
        Path file = filePreferensiBarang();
        if (file == null || !Files.isRegularFile(file)) {
            return -1;
        }
        Properties properti = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properti.load(input);
            return Integer.parseInt(properti.getProperty(KUNCI_PREFERENSI, "-1"));
        } catch (IOException | NumberFormatException exception) {
            return -1;
        }
    }

    private void simpanPreferensiBarang() {
        Path file = filePreferensiBarang();
        if (file == null || barang == null) {
            return;
        }
        Properties properti = new Properties();
        properti.setProperty(KUNCI_PREFERENSI, String.valueOf(barang.getId()));
        try {
            Path induk = file.getParent();
            if (induk != null) {
                Files.createDirectories(induk);
            }
            try (OutputStream output = Files.newOutputStream(file)) {
                properti.store(output, "KantinFlow UI state");
            }
        } catch (IOException exception) {
            // Kegagalan preferensi tidak boleh mengganggu operasi aplikasi.
        }
    }

    private void perbaruiSemua() {
        if (barang == null) {
            PanelKantin halamanBarang = panel.get("barang");
            if (halamanBarang != null) {
                halamanBarang.perbarui();
            }
            return;
        }
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

    private void perbaruiComboBarang() {
        menggantiBarang = true;
        try {
            comboBarang.removeAllItems();
            boolean kosong = daftarBarang.isEmpty();
            comboBarang.setEnabled(!kosong);
            if (labelBarangKosong != null) {
                labelBarangKosong.setVisible(kosong);
            }
            if (kosong) {
                comboBarang.setToolTipText(
                        "Belum ada barang. Buka halaman Barang untuk menambahkan.");
                return;
            }
            for (Barang item : daftarBarang) {
                comboBarang.addItem(item);
            }
            if (barang == null) {
                comboBarang.setSelectedIndex(-1);
                return;
            }
            for (int i = 0; i < comboBarang.getItemCount(); i++) {
                if (comboBarang.getItemAt(i).getId() == barang.getId()) {
                    comboBarang.setSelectedIndex(i);
                    break;
                }
            }
            comboBarang.setToolTipText(barang.getNama() + " ("
                    + barang.getSatuan() + ")");
        } finally {
            menggantiBarang = false;
        }
    }

    private boolean adaIsianTransaksiBelumDisimpan() {
        for (PanelKantin halaman : panel.values()) {
            if (halaman.adaIsianBelumDisimpan()) {
                return true;
            }
        }
        return false;
    }

    private boolean konfirmasiGantiBarang(Barang dipilih) {
        String pesan = "Formulir transaksi masih berisi isian. Ganti barang aktif "
                + "dari " + barang.getNama() + " ke " + dipilih.getNama()
                + "? Isian formulir akan dibuang.";
        int jawaban = JOptionPane.showConfirmDialog(
                this, pesan, "Ganti barang aktif",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return jawaban == JOptionPane.YES_OPTION;
    }

    private void kembalikanPilihanBarang() {
        menggantiBarang = true;
        try {
            comboBarang.setSelectedItem(barang);
            if (barang != null) {
                comboBarang.setToolTipText(barang.getNama() + " ("
                        + barang.getSatuan() + ")");
            }
        } finally {
            menggantiBarang = false;
        }
    }

    private void tampilkanPesanNetral(String pesan) {
        status.setText(pesan);
        status.setForeground(Ui.TEKS);
        status.setCaretPosition(0);
    }

    private void gantiReferensiBarang(Barang baru) {
        for (int i = 0; i < daftarBarang.size(); i++) {
            if (daftarBarang.get(i).getId() == baru.getId()) {
                daftarBarang.set(i, baru);
                return;
            }
        }
        daftarBarang.add(baru);
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
