package kantinflow.gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.ui.FlatLineBorder;
import com.formdev.flatlaf.ui.FlatTableHeaderUI;
import com.formdev.flatlaf.util.UIScale;

import kantinflow.Barang;

/**
 * Komponen dan token visual bersama untuk tujuh halaman Swing/FlatLaf.
 */
final class Ui {
    static Color LATAR = new Color(0xF4F6FA);
    static Color KARTU = Color.WHITE;
    static Color GARIS = new Color(0xE2E8F0);
    static Color GARIS_KUAT = new Color(0xCBD5E1);
    static Color AKSEN = new Color(0x1D4ED8);
    static Color AKSEN_GELAP = new Color(0x1E40AF);
    static Color AKSEN_LEMBUT = new Color(0xDBEAFE);
    static Color TEKS = new Color(0x0F172A);
    static Color TEKS_LEMBUT = new Color(0x64748B);
    static Color HIJAU = new Color(0x16A34A);
    static Color HIJAU_LEMBUT = new Color(0xDCFCE7);
    static Color MERAH = new Color(0xDC2626);
    static Color MERAH_LEMBUT = new Color(0xFEE2E2);
    static Color KUNING = new Color(0xD97706);
    static Color KUNING_LEMBUT = new Color(0xFEF3C7);
    static Color ORANYE = new Color(0xEA580C);
    static Color ORANYE_LEMBUT = new Color(0xFFEDD5);
    static Color HIJAU_BADGE = new Color(0x15803D);
    static Color KUNING_BADGE = new Color(0xB45309);
    static Color MERAH_BADGE = new Color(0xB91C1C);
    static Color ORANYE_BADGE = new Color(0xC2410C);
    static Color NETRAL_LEMBUT = new Color(0xF1F5F9);
    static Color HOVER = new Color(0xEEF2FF);

    static final int RADIUS_KARTU = 16;
    static final int RADIUS_KONTROL = 10;
    static final int TINGGI_INPUT = 34;
    static final int TINGGI_TOMBOL = 36;
    static final int TINGGI_BARIS_TABEL = 40;
    static final int TINGGI_HEADER_TABEL = 44;
    static final int PADDING_SEL_TABEL = 12;
    static final int LEBAR_SIDEBAR = 240;
    static final int LEBAR_JENDELA = 1180;
    static final int TINGGI_JENDELA = 720;
    static final int LEBAR_MINIMUM = 960;
    static final int TINGGI_MINIMUM = 640;
    static final int LEBAR_KONTEN = 1200;
    static final int LEBAR_KONTEN_RINGKASAN = 1400;
    static final int LEBAR_INPUT_MAKS = 460;
    static final int AMBANG_FORM_SEMPIT = 760;
    static final int AMBANG_FILTER_RIWAYAT = 600;
    static final int LEBAR_FORM_KELUAR = 460;
    static final int LEBAR_KOMBO_BARANG = 230;
    static final int TINGGI_FORM_MANUAL = 260;
    static final int TINGGI_FORM_FEFO = 300;
    static final int TINGGI_TABEL_BARANG = 175;
    static final int TINGGI_TABEL_STOK_MASUK = 260;
    static final int TINGGI_TABEL_PEMANTAUAN = 240;
    static final int TINGGI_TABEL_RIWAYAT = 300;
    static final int TINGGI_TABEL_DEFAULT = 230;
    static final Insets PADDING_HALAMAN = new Insets(24, 24, 24, 24);
    static final Insets PADDING_KARTU = new Insets(20, 20, 20, 20);
    static final int GAP_KARTU = 16;
    static final int GAP_FORM = 16;
    static final float FONT_JUDUL = 24f;
    static final float FONT_KARTU = 16f;
    static final float FONT_NILAI = 22f;
    static final float FONT_BRAND = 20f;
    static final float FONT_NAV = 13f;
    static final float FONT_EYEBROW = 9.5f;
    static final float FONT_STATUS = 12f;
    static final float FONT_FOOTER = 10f;
    static final float FONT_BODY = 13f;
    static final float FONT_LABEL = 11.5f;
    static final float FONT_CATATAN = 12f;
    static final float FONT_KECIL = 11f;
    static final float FONT_BUTTON = 12f;

    private Ui() {
    }

    static int skala(int nilai) {
        return UIScale.scale(nilai);
    }

    /**
     * Tinggi dasar kontrol mengikuti metrik font tema dan skala FlatLaf,
     * sehingga teks tidak terpotong saat font atau DPI diperbesar.
     */
    static int tinggiKontrol() {
        JLabel contoh = new JLabel();
        contoh.setFont(font(Font.PLAIN, FONT_BODY));
        int tinggiTeks = contoh.getFontMetrics(contoh.getFont()).getHeight();
        return Math.max(skala(TINGGI_INPUT), tinggiTeks + skala(18));
    }

    static Font font(int gaya, float ukuran) {
        Font dasar = javax.swing.UIManager.getFont("Label.font");
        String nama = dasar == null ? "Segoe UI" : dasar.getName();
        return new Font(nama, gaya, Math.max(10, Math.round(ukuran)));
    }

    static Icon ikonMenu(String kunci, int ukuran, Color warna) {
        ukuran = skala(ukuran);
        String nama = switch (kunci) {
            case "ringkasan" -> "dashboard";
            case "barang" -> "package";
            case "stok-masuk" -> "stock-in";
            case "stok-keluar" -> "stock-out";
            case "pemantauan" -> "monitor";
            case "pembuangan" -> "trash";
            case "riwayat" -> "history";
            default -> null;
        };
        return nama == null ? null : ikon(nama, ukuran, warna);
    }

    static Icon ikonAksi(String nama, int ukuran, Color warna) {
        return ikon(nama, ukuran, warna);
    }

    private static Icon ikon(String nama, int ukuran, Color warna) {
        try {
            URL url = Ui.class.getResource(
                    "/kantinflow/icons/" + nama + ".svg");
            if (url == null) {
                return null;
            }
            FlatSVGIcon svg = new FlatSVGIcon(url);
            if (!svg.hasFound()) {
                return null;
            }
            FlatSVGIcon hasil = svg.derive(ukuran, ukuran);
            if (warna != null) {
                hasil.setColorFilter(new FlatSVGIcon.ColorFilter(c -> new Color(
                        warna.getRed(), warna.getGreen(), warna.getBlue(),
                        c.getAlpha())));
            }
            return hasil;
        } catch (RuntimeException | LinkageError exception) {
            return null;
        }
    }

    static JLabel judul(String teks) {
        JLabel label = new JLabel(teks);
        label.setFont(font(Font.BOLD, FONT_JUDUL));
        label.setForeground(TEKS);
        return label;
    }

    static JLabel keterangan(String teks) {
        JLabel label = new JLabel(teks);
        label.setFont(font(Font.PLAIN, FONT_CATATAN));
        label.setForeground(TEKS_LEMBUT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setHorizontalAlignment(SwingConstants.LEFT);
        return label;
    }

    static JLabel labelField(String teks) {
        JLabel label = new JLabel(teks);
        label.setFont(font(Font.BOLD, FONT_LABEL));
        label.setForeground(TEKS_LEMBUT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setHorizontalAlignment(SwingConstants.LEFT);
        return label;
    }

    static String teksJumlah(Barang barang) {
        String satuan = barang == null ? null : barang.getSatuan();
        if (satuan == null || satuan.isBlank()) {
            return "Jumlah";
        }
        return "Jumlah (" + satuan + ")";
    }

    static JLabel pesan() {
        JLabel label = new JLabel(" ");
        label.setFont(font(Font.PLAIN, FONT_KECIL));
        label.setForeground(MERAH);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setHorizontalAlignment(SwingConstants.LEFT);
        return label;
    }

    static JLabel kosong(String teks) {
        JLabel label = new JLabel(teks);
        label.setFont(font(Font.PLAIN, FONT_CATATAN));
        label.setForeground(TEKS_LEMBUT);
        label.setBorder(new EmptyBorder(8, 2, 2, 2));
        return label;
    }

    static JPanel header(String judul, String keterangan) {
        JPanel panel = new JPanel(new BorderLayout(0, 3));
        panel.setOpaque(false);
        panel.add(judul(judul), BorderLayout.NORTH);
        panel.add(Ui.keterangan(keterangan), BorderLayout.CENTER);
        return panel;
    }

    static JPanel kartu() {
        return new CardPanel(new BorderLayout(0, skala(GAP_KARTU)));
    }

    static JPanel kartu(String judul) {
        return kartu(judul, null);
    }

    static JPanel kartu(String judul, String ikon) {
        JPanel panel = kartu();
        JPanel kepala = new JPanel(new BorderLayout(8, 0));
        kepala.setOpaque(false);
        if (ikon != null) {
            JLabel gambar = new JLabel(ikonAksi(ikon, 18, AKSEN));
            gambar.setBorder(new EmptyBorder(0, 0, 0, 2));
            kepala.add(gambar, BorderLayout.WEST);
        }
        JLabel label = new JLabel(judul);
        label.setFont(font(Font.BOLD, FONT_KARTU));
        label.setForeground(TEKS);
        kepala.add(label, BorderLayout.CENTER);
        panel.add(kepala, BorderLayout.NORTH);
        return panel;
    }

    static JTextField field(int kolom) {
        JTextField field = new MedanKantin(kolom);
        field.setFont(font(Font.PLAIN, FONT_BODY));
        field.putClientProperty("JTextField.placeholderText", "");
        return field;
    }

    static JButton utama(String teks) {
        JButton tombol = new TombolKantin(teks);
        tombol.setFont(font(Font.BOLD, FONT_BUTTON));
        tombol.setBackground(AKSEN);
        tombol.setForeground(Color.WHITE);
        tombol.setBorder(new FlatLineBorder(
                new Insets(skala(9), skala(16), skala(9), skala(16)),
                null, 0f, RADIUS_KONTROL));
        tombol.setFocusPainted(true);
        tombol.setOpaque(true);
        tombol.addPropertyChangeListener("enabled", event -> {
            if (tombol.isEnabled()) {
                tombol.setBackground(AKSEN);
                tombol.setForeground(Color.WHITE);
            } else {
                tombol.setBackground(GARIS_KUAT);
                tombol.setForeground(TEKS_LEMBUT);
            }
        });
        return tombol;
    }

    static JButton sekunder(String teks) {
        JButton tombol = new TombolKantin(teks);
        tombol.setFont(font(Font.PLAIN, FONT_BUTTON));
        tombol.setBackground(KARTU);
        tombol.setForeground(TEKS);
        tombol.setBorder(new FlatLineBorder(
                new Insets(skala(8), skala(14), skala(8), skala(14)),
                GARIS_KUAT, 1f, RADIUS_KONTROL));
        tombol.setFocusPainted(true);
        tombol.setOpaque(true);
        return tombol;
    }

    static JButton bahaya(String teks) {
        JButton tombol = new TombolKantin(teks);
        tombol.setFont(font(Font.BOLD, FONT_BUTTON));
        tombol.setBackground(MERAH);
        tombol.setForeground(Color.WHITE);
        tombol.setBorder(new FlatLineBorder(
                new Insets(skala(9), skala(16), skala(9), skala(16)),
                null, 0f, RADIUS_KONTROL));
        tombol.setFocusPainted(true);
        tombol.setOpaque(true);
        return tombol;
    }

    static JPanel baris(Component... komponen) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        panel.setOpaque(false);
        for (Component item : komponen) {
            panel.add(item);
        }
        return panel;
    }

    static <T> JComboBox<T> combo() {
        JComboBox<T> combo = new KomboKantin<>();
        combo.setFont(font(Font.PLAIN, FONT_BODY));
        return combo;
    }

    static JComboBox<Barang> comboBarang() {
        JComboBox<Barang> combo = combo();
        combo.setRenderer(new DefaultListCellRenderer() {
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
        return combo;
    }

    static JPanel field(String label, Component input) {
        return field(label, input, null);
    }

    static JPanel field(String label, Component input, JLabel pesan) {
        return field(labelField(label), input, pesan);
    }

    static JPanel field(JLabel label, Component input) {
        return field(label, input, null);
    }

    static JPanel field(JLabel label, Component input, JLabel pesan) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        panel.add(label, BorderLayout.NORTH);
        panel.add(input, BorderLayout.CENTER);
        if (pesan != null) {
            panel.add(pesan, BorderLayout.SOUTH);
        }
        return panel;
    }

    static JScrollPane scrollTabel(JTable tabel) {
        return scrollTabel(tabel, TINGGI_TABEL_DEFAULT);
    }

    static void sesuaikanTinggiTabel(JScrollPane gulir, JTable tabel,
            int maksBaris) {
        if (gulir == null) {
            return;
        }
        int baris = Math.min(tabel.getRowCount(), maksBaris);
        int tinggi = tabel.getTableHeader().getPreferredSize().height
                + baris * tabel.getRowHeight() + 2;
        gulir.setPreferredSize(new Dimension(0, tinggi));
        gulir.revalidate();
    }

    static JScrollPane scrollTabel(JTable tabel, int tinggi) {
        JScrollPane scroll = new JScrollPane(tabel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setViewportBorder(null);
        scroll.getViewport().setBackground(KARTU);
        if (scroll.getColumnHeader() != null) {
            scroll.getColumnHeader().setOpaque(true);
            scroll.getColumnHeader().setBackground(KARTU);
        }
        scroll.setPreferredSize(new Dimension(600, skala(tinggi)));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    static JPanel kartuNilai(String judul, JLabel nilai, JLabel detail) {
        JPanel panel = kartu();
        panel.setLayout(new BorderLayout(0, 5));
        JLabel label = new JLabel(judul);
        label.setFont(font(Font.PLAIN, 12f));
        label.setForeground(TEKS_LEMBUT);
        nilai.setFont(font(Font.BOLD, FONT_NILAI));
        nilai.setForeground(TEKS);
        detail.setFont(font(Font.PLAIN, FONT_KECIL));
        detail.setForeground(TEKS_LEMBUT);
        panel.add(label, BorderLayout.NORTH);
        panel.add(nilai, BorderLayout.CENTER);
        panel.add(detail, BorderLayout.SOUTH);
        return panel;
    }

    /**
     * Baris filter: label di atas kontrol, kontrol rata bawah, dan jarak
     * antarkontrol dari token bersama. Komponen pertama (dropdown barang)
     * mengambil sisa lebar; sisanya memakai lebar pilihan sendiri. Bila
     * lebar kartu tidak cukup, komponen selain yang pertama dipindah ke
     * baris kedua agar teks tidak terpotong.
     */
    static JPanel barisFilter(Component... bagian) {
        JPanel panel = new BarisFilter(bagian);
        panel.setOpaque(false);
        for (Component item : bagian) {
            panel.add(item);
        }
        return panel;
    }

    static JPanel duaKartu(Component kiri, Component kanan) {
        return gridResponsif(2, AMBANG_FORM_SEMPIT, kiri, kanan);
    }

    /**
     * Grid dua kolom pada lebar desktop dan satu kolom saat ruang menyempit.
     */
    static JPanel gridResponsif(int kolomLebar, int ambang, Component... komponen) {
        JPanel panel = new JPanel(new GridLayout(
                Math.max(1, (komponen.length + kolomLebar - 1) / kolomLebar),
                Math.min(kolomLebar, komponen.length),
                skala(GAP_FORM), skala(GAP_FORM)));
        panel.setOpaque(false);
        for (Component item : komponen) {
            panel.add(item);
        }
        panel.addComponentListener(new ComponentAdapter() {
            private boolean sempit;

            @Override
            public void componentResized(ComponentEvent event) {
                boolean sekarangSempit = panel.getWidth() > 0
                        && panel.getWidth() < ambang;
                if (sekarangSempit == sempit) {
                    return;
                }
                sempit = sekarangSempit;
                int kolom = sempit ? 1 : Math.min(kolomLebar, komponen.length);
                int baris = Math.max(1,
                        (komponen.length + kolom - 1) / kolom);
                panel.setLayout(new GridLayout(baris, kolom,
                        skala(GAP_FORM), skala(GAP_FORM)));
                panel.revalidate();
                panel.repaint();
            }
        });
        return panel;
    }

    /**
     * Grid bertahap: empat kolom pada layar lebar, dua kolom pada lebar sedang,
     * dan satu kolom saat ruang menyempit.
     */
    static JPanel gridBertahap(int kolomMaks, int ambangDua, int ambangSatu,
            Component... komponen) {
        int kolomAwal = Math.min(kolomMaks, komponen.length);
        JPanel panel = new JPanel(new GridLayout(
                Math.max(1, (komponen.length + kolomAwal - 1) / kolomAwal),
                Math.max(1, kolomAwal), skala(GAP_FORM), skala(GAP_FORM)));
        panel.setOpaque(false);
        for (Component item : komponen) {
            panel.add(item);
        }
        panel.addComponentListener(new ComponentAdapter() {
            private int kolomTerakhir = kolomAwal;

            @Override
            public void componentResized(ComponentEvent event) {
                int lebar = panel.getWidth();
                if (lebar <= 0) {
                    return;
                }
                int kolom = lebar < ambangSatu ? 1
                        : lebar < ambangDua ? Math.min(2, komponen.length)
                        : kolomAwal;
                if (kolom == kolomTerakhir) {
                    return;
                }
                kolomTerakhir = kolom;
                int baris = Math.max(1,
                        (komponen.length + kolom - 1) / kolom);
                panel.setLayout(new GridLayout(baris, kolom,
                        skala(GAP_FORM), skala(GAP_FORM)));
                panel.revalidate();
                panel.repaint();
            }
        });
        return panel;
    }

    /**
     * Form grid yang membatasi lebar tiap kolom input agar tidak melebar
     * mengikuti kartu pada layar besar.
     */
    static JPanel gridForm(int kolomLebar, int ambang, Component... komponen) {
        Component[] dibatasi = new Component[komponen.length];
        for (int i = 0; i < komponen.length; i++) {
            dibatasi[i] = lebarMaksimum(komponen[i], LEBAR_INPUT_MAKS);
        }
        return gridResponsif(kolomLebar, ambang, dibatasi);
    }

    static JPanel lebarMaksimum(Component isi, int maks) {
        return new LebarMaksimum(isi, maks);
    }

    /**
     * Mengatur lebar kolom tabel secara proporsional. Kolom angka tetap lebih
     * sempit dan rata kanan; kolom teks panjang mendapat porsi lebih besar.
     */
    static void gulirHorizontalSaatSempit(JTable tabel) {
        if (tabel instanceof TabelKantin khusus) {
            khusus.setHorizontalSaatSempit(true);
        }
    }

    static void aturKolom(JTable tabel, int... lebar) {
        tabel.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        int jumlah = Math.min(lebar.length,
                tabel.getColumnModel().getColumnCount());
        for (int i = 0; i < jumlah; i++) {
            int nilai = skala(lebar[i]);
            tabel.getColumnModel().getColumn(i).setPreferredWidth(nilai);
            tabel.getColumnModel().getColumn(i).setMinWidth(
                    skala(Math.min(lebar[i],
                            Math.max(84, Math.round(lebar[i] * 0.6f)))));
        }
    }

    private static final class MedanKantin extends JTextField {
        private MedanKantin(int kolom) {
            super(kolom);
        }

        @Override
        public Dimension getPreferredSize() {
            return tinggiSeragam(super.getPreferredSize());
        }

        @Override
        public Dimension getMinimumSize() {
            return tinggiSeragam(super.getMinimumSize());
        }

        private static Dimension tinggiSeragam(Dimension ukuran) {
            return new Dimension(ukuran.width,
                    Math.max(ukuran.height, tinggiKontrol()));
        }
    }

    private static final class KomboKantin<T> extends JComboBox<T> {
        @Override
        public Dimension getPreferredSize() {
            return MedanKantin.tinggiSeragam(super.getPreferredSize());
        }

        @Override
        public Dimension getMinimumSize() {
            return MedanKantin.tinggiSeragam(super.getMinimumSize());
        }
    }

    private static final class TombolKantin extends JButton {
        private TombolKantin(String teks) {
            super(teks);
        }

        @Override
        public Dimension getPreferredSize() {
            return MedanKantin.tinggiSeragam(super.getPreferredSize());
        }

        @Override
        public Dimension getMinimumSize() {
            return MedanKantin.tinggiSeragam(super.getMinimumSize());
        }
    }

    private static final class LebarMaksimum extends JPanel {
        private final Component isi;
        private final int maks;

        private LebarMaksimum(Component isi, int maks) {
            super(null);
            this.isi = isi;
            this.maks = maks;
            setOpaque(false);
            add(isi);
        }

        @Override
        public void doLayout() {
            int lebar = Math.min(getWidth(), skala(maks));
            int tinggi = Math.min(getHeight(), isi.getPreferredSize().height);
            isi.setBounds(0, 0, lebar, tinggi);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension ukuran = isi.getPreferredSize();
            return new Dimension(Math.min(ukuran.width, skala(maks)),
                    ukuran.height);
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(0, isi.getMinimumSize().height);
        }
    }

    /**
     * Menata baris filter tanpa tinggi tetap: satu baris saat cukup, dua
     * baris saat menyempit. Tinggi mengikuti isi agar kartu tidak menyisakan
     * ruang kosong.
     */
    private static final class BarisFilter extends JPanel {
        private final Component[] bagian;

        private BarisFilter(Component[] bagian) {
            super(new GridBagLayout());
            this.bagian = bagian;
            if (bagian.length > 0) {
                Dimension minimum = bagian[0].getMinimumSize();
                bagian[0].setMinimumSize(new Dimension(0, minimum.height));
            }
        }

        @Override
        public void doLayout() {
            int jarak = skala(GAP_FORM);
            int lebarTetap = 0;
            for (int i = 1; i < bagian.length; i++) {
                lebarTetap += bagian[i].getPreferredSize().width;
            }
            lebarTetap += jarak * Math.max(0, bagian.length - 1);
            int lebar = getWidth();
            boolean wrap = bagian.length > 1
                    && lebar < lebarTetap + skala(220);
            for (int i = 0; i < bagian.length; i++) {
                GridBagConstraints c = new GridBagConstraints();
                c.gridx = wrap && i > 0 ? i - 1 : i;
                c.gridy = wrap && i > 0 ? 1 : 0;
                c.gridwidth = 1;
                c.weightx = i == 0 && !wrap ? 1 : 0;
                c.weighty = 0;
                c.fill = GridBagConstraints.HORIZONTAL;
                c.anchor = GridBagConstraints.NORTH;
                c.insets = new Insets(0, i == 0 || (wrap && i == 1)
                        ? 0 : jarak, 0, 0);
                ((GridBagLayout) getLayout()).setConstraints(bagian[i], c);
            }
            super.doLayout();
        }
    }

    static DefaultTableModel model(String... kolom) {
        return new DefaultTableModel(kolom, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private static DefaultTableCellRenderer penyajiHeader(boolean rataKanan) {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                if (table != null && table.getTableHeader() != null) {
                    javax.swing.table.JTableHeader header =
                            table.getTableHeader();
                    setFont(header.getFont());
                    setForeground(header.getForeground());
                    setOpaque(false);
                }
                setHorizontalAlignment(rataKanan
                        ? SwingConstants.RIGHT : SwingConstants.LEFT);
                setVerticalAlignment(SwingConstants.CENTER);
                setBorder(new EmptyBorder(0, skala(PADDING_SEL_TABEL),
                        0, skala(PADDING_SEL_TABEL)));
                return this;
            }
        };
    }

    static JTable tabel(DefaultTableModel model) {
        JTable tabel = new TabelKantin(model);
        tabel.setFont(font(Font.PLAIN, FONT_BODY));
        int tinggiBaris = Math.max(skala(TINGGI_BARIS_TABEL),
                tabel.getFontMetrics(tabel.getFont()).getHeight()
                + skala(10));
        tabel.setRowHeight(tinggiBaris);
        tabel.setFillsViewportHeight(true);
        tabel.setShowVerticalLines(false);
        tabel.setShowHorizontalLines(true);
        tabel.setIntercellSpacing(new Dimension(0, 1));
        tabel.setGridColor(GARIS);
        tabel.setBackground(KARTU);
        tabel.setSelectionBackground(AKSEN_LEMBUT);
        tabel.setSelectionForeground(TEKS);
        tabel.getTableHeader().setUI(new KepalaMembulat());
        tabel.getTableHeader().setOpaque(false);
        tabel.getTableHeader().setBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, GARIS));
        tabel.getTableHeader().setFont(font(Font.BOLD, FONT_LABEL));
        tabel.getTableHeader().setForeground(TEKS_LEMBUT);
        tabel.getTableHeader().setBackground(NETRAL_LEMBUT);
        tabel.getTableHeader().setDefaultRenderer(penyajiHeader(false));
        int tinggiHeader = Math.max(skala(TINGGI_HEADER_TABEL),
                tabel.getFontMetrics(tabel.getFont()).getHeight()
                + skala(14));
        tabel.getTableHeader().setPreferredSize(
                new Dimension(0, tinggiHeader));
        DefaultTableCellRenderer cell = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.LEFT);
                setVerticalAlignment(SwingConstants.CENTER);
                setBorder(new EmptyBorder(0, skala(PADDING_SEL_TABEL),
                    0, skala(PADDING_SEL_TABEL)));
                int hover = table instanceof TabelKantin
                        ? ((TabelKantin) table).getBarisHover() : -1;
                if (!isSelected) {
                    setForeground(TEKS);
                    setBackground(row == hover ? HOVER : KARTU);
                } else {
                    setForeground(TEKS);
                    setBackground(table.getSelectionBackground());
                }
                return this;
            }
        };
        tabel.setDefaultRenderer(Object.class, cell);
        return tabel;
    }

    static void pasangBadge(JTable tabel, int... kolom) {
        BadgeRenderer renderer = new BadgeRenderer();
        for (int index : kolom) {
            if (index >= 0 && index < tabel.getColumnModel().getColumnCount()) {
                tabel.getColumnModel().getColumn(index)
                        .setCellRenderer(renderer);
            }
        }
    }

    /**
     * Menampilkan status/jenis sebagai teks berwarna tanpa pill, border, atau
     * latar kapsul. Warna mengikuti semantik {@link Badge#warnaUntuk(String)},
     * sedangkan baris terpilih memakai warna teks utama agar tetap kontras
     * dengan latar seleksi tema terang.
     */
    static void pasangTeksStatus(JTable tabel, int... kolom) {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                setBorder(new EmptyBorder(0, skala(PADDING_SEL_TABEL),
                        0, skala(PADDING_SEL_TABEL)));
                setHorizontalAlignment(SwingConstants.LEFT);
                setVerticalAlignment(SwingConstants.CENTER);
                int hover = table instanceof TabelKantin
                        ? ((TabelKantin) table).getBarisHover() : -1;
                if (isSelected) {
                    setForeground(TEKS);
                    setBackground(table.getSelectionBackground());
                } else {
                    setForeground(Badge.warnaUntuk(
                            value == null ? "" : value.toString())[0]);
                    setBackground(row == hover ? HOVER : KARTU);
                }
                return this;
            }
        };
        for (int index : kolom) {
            if (index >= 0 && index < tabel.getColumnModel().getColumnCount()) {
                tabel.getColumnModel().getColumn(index)
                        .setCellRenderer(renderer);
            }
        }
    }

    /**
     * Menyelaraskan header kolom angka ke kanan agar mengikuti isi kolom.
     * Dipakai juga oleh panel yang memasang renderer sel khusus.
     */
    static void rataKananHeader(JTable tabel, int... kolom) {
        for (int index : kolom) {
            if (index >= 0 && index < tabel.getColumnModel().getColumnCount()) {
                tabel.getColumnModel().getColumn(index)
                        .setHeaderRenderer(penyajiHeader(true));
            }
        }
    }

    static void rataKanan(JTable tabel, int... kolom) {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                setBorder(new EmptyBorder(0, skala(PADDING_SEL_TABEL),
                    0, skala(PADDING_SEL_TABEL)));
                setHorizontalAlignment(SwingConstants.RIGHT);
                setVerticalAlignment(SwingConstants.CENTER);
                int hover = table instanceof TabelKantin
                        ? ((TabelKantin) table).getBarisHover() : -1;
                if (!isSelected) {
                    setForeground(TEKS);
                    setBackground(row == hover ? HOVER : KARTU);
                } else {
                    setForeground(TEKS);
                    setBackground(table.getSelectionBackground());
                }
                return this;
            }
        };
        for (int index : kolom) {
            if (index >= 0 && index < tabel.getColumnModel().getColumnCount()) {
                tabel.getColumnModel().getColumn(index).setCellRenderer(renderer);
            }
        }
        rataKananHeader(tabel, kolom);
    }

    /**
     * Menambah jarak kiri pada sel dan header satu kolom agar batas dengan
     * kolom sebelumnya lebih jelas tanpa mengubah isi atau penyelarasan.
     */
    static void jarakKiriKolom(JTable tabel, int kolom, int jarak) {
        if (kolom < 0 || kolom >= tabel.getColumnModel().getColumnCount()) {
            return;
        }
        TableColumn kolomTabel = tabel.getColumnModel().getColumn(kolom);
        TableCellRenderer penyajiSel = kolomTabel.getCellRenderer() != null
                ? kolomTabel.getCellRenderer()
                : tabel.getDefaultRenderer(Object.class);
        kolomTabel.setCellRenderer((table, value, isSelected, hasFocus,
                row, column) -> jarakKiri(penyajiSel.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column), jarak));
        TableCellRenderer penyajiKepala = kolomTabel.getHeaderRenderer() != null
                ? kolomTabel.getHeaderRenderer()
                : tabel.getTableHeader().getDefaultRenderer();
        kolomTabel.setHeaderRenderer((table, value, isSelected, hasFocus,
                row, column) -> jarakKiri(
                        penyajiKepala.getTableCellRendererComponent(
                                table, value, isSelected, hasFocus, row, column),
                        jarak));
    }

    private static Component jarakKiri(Component komponen, int jarak) {
        if (komponen instanceof JComponent isi) {
            Border garis = isi.getBorder();
            Insets ukuran = garis instanceof EmptyBorder kosong
                    ? kosong.getBorderInsets() : isi.getInsets();
            isi.setBorder(new EmptyBorder(ukuran.top,
                    ukuran.left + skala(jarak), ukuran.bottom, ukuran.right));
        }
        return komponen;
    }

    private static final class KepalaMembulat extends FlatTableHeaderUI {
        private final int busur = skala(9);

        @Override
        public void paint(Graphics graphics, JComponent komponen) {
            if (komponen.getWidth() <= 0 || komponen.getHeight() <= 0) {
                return;
            }
            Graphics2D latar = (Graphics2D) graphics.create();
            try {
                latar.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                latar.setColor(NETRAL_LEMBUT);
                latar.fillRoundRect(0, 0, komponen.getWidth() - 1,
                        komponen.getHeight() - 1, busur, busur);
            } finally {
                latar.dispose();
            }

            Graphics2D isi = (Graphics2D) graphics.create();
            try {
                isi.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                isi.clip(new RoundRectangle2D.Float(
                        0, 0, komponen.getWidth(), komponen.getHeight(),
                        busur, busur));
                super.paint(isi, komponen);
            } finally {
                isi.dispose();
            }

            Graphics2D sudut = (Graphics2D) graphics.create();
            try {
                sudut.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                Area luar = new Area(new java.awt.Rectangle(
                        0, 0, komponen.getWidth(), komponen.getHeight()));
                luar.subtract(new Area(new RoundRectangle2D.Float(
                        0, 0, komponen.getWidth() - 1,
                        komponen.getHeight() - 1, busur, busur)));
                sudut.setColor(KARTU);
                sudut.fill(luar);
            } finally {
                sudut.dispose();
            }
        }
    }
}
