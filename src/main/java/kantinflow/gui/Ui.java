package kantinflow.gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.util.UIScale;

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

    static void pakaiTema(boolean gelap) {
        if (gelap) {
            LATAR = new Color(0x0F172A);
            KARTU = new Color(0x111827);
            GARIS = new Color(0x1F2937);
            GARIS_KUAT = new Color(0x334155);
            AKSEN = new Color(0x1D4ED8);
            AKSEN_GELAP = new Color(0x1E40AF);
            AKSEN_LEMBUT = new Color(0x1E3A8A);
            TEKS = new Color(0xF8FAFC);
            TEKS_LEMBUT = new Color(0x94A3B8);
            HIJAU = new Color(0x4ADE80);
            HIJAU_LEMBUT = new Color(0x052E16);
            MERAH = new Color(0xF87171);
            MERAH_LEMBUT = new Color(0x450A0A);
            KUNING = new Color(0xFBBF24);
            KUNING_LEMBUT = new Color(0x451A03);
            ORANYE = new Color(0xFB923C);
            ORANYE_LEMBUT = new Color(0x431407);
            HIJAU_BADGE = new Color(0x86EFAC);
            KUNING_BADGE = new Color(0xFDE68A);
            MERAH_BADGE = new Color(0xFCA5A5);
            ORANYE_BADGE = new Color(0xFDBA74);
            NETRAL_LEMBUT = new Color(0x1E293B);
            HOVER = new Color(0x1E293B);
        } else {
            LATAR = new Color(0xF4F6FA);
            KARTU = Color.WHITE;
            GARIS = new Color(0xE2E8F0);
            GARIS_KUAT = new Color(0xCBD5E1);
            AKSEN = new Color(0x1D4ED8);
            AKSEN_GELAP = new Color(0x1E40AF);
            AKSEN_LEMBUT = new Color(0xDBEAFE);
            TEKS = new Color(0x0F172A);
            TEKS_LEMBUT = new Color(0x64748B);
            HIJAU = new Color(0x16A34A);
            HIJAU_LEMBUT = new Color(0xDCFCE7);
            MERAH = new Color(0xDC2626);
            MERAH_LEMBUT = new Color(0xFEE2E2);
            KUNING = new Color(0xD97706);
            KUNING_LEMBUT = new Color(0xFEF3C7);
            ORANYE = new Color(0xEA580C);
            ORANYE_LEMBUT = new Color(0xFFEDD5);
            HIJAU_BADGE = new Color(0x15803D);
            KUNING_BADGE = new Color(0xB45309);
            MERAH_BADGE = new Color(0xB91C1C);
            ORANYE_BADGE = new Color(0xC2410C);
            NETRAL_LEMBUT = new Color(0xF1F5F9);
            HOVER = new Color(0xEEF2FF);
        }
    }

    static int skala(int nilai) {
        return UIScale.scale(nilai);
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
        JTextField field = new JTextField(kolom);
        field.setFont(font(Font.PLAIN, FONT_BODY));
        field.putClientProperty("JTextField.placeholderText", "");
        return field;
    }

    static JButton utama(String teks) {
        JButton tombol = new JButton(teks);
        tombol.setFont(font(Font.BOLD, FONT_BUTTON));
        tombol.setBackground(AKSEN);
        tombol.setForeground(Color.WHITE);
        tombol.setBorder(new EmptyBorder(skala(9), skala(16),
                skala(9), skala(16)));
        tombol.setFocusPainted(true);
        tombol.setOpaque(true);
        tombol.putClientProperty("JButton.buttonType", "roundRect");
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
        JButton tombol = new JButton(teks);
        tombol.setFont(font(Font.PLAIN, FONT_BUTTON));
        tombol.setBackground(KARTU);
        tombol.setForeground(TEKS);
        tombol.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GARIS_KUAT),
                new EmptyBorder(skala(8), skala(14), skala(8), skala(14))));
        tombol.setFocusPainted(true);
        tombol.setOpaque(true);
        tombol.putClientProperty("JButton.buttonType", "roundRect");
        return tombol;
    }

    static JButton bahaya(String teks) {
        JButton tombol = new JButton(teks);
        tombol.setFont(font(Font.BOLD, FONT_BUTTON));
        tombol.setBackground(MERAH);
        tombol.setForeground(Color.WHITE);
        tombol.setBorder(new EmptyBorder(skala(9), skala(16),
                skala(9), skala(16)));
        tombol.setFocusPainted(true);
        tombol.setOpaque(true);
        tombol.putClientProperty("JButton.buttonType", "roundRect");
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
        JComboBox<T> combo = new JComboBox<>();
        combo.setFont(font(Font.PLAIN, FONT_BODY));
        return combo;
    }

    static JPanel field(String label, Component input) {
        return field(label, input, null);
    }

    static JPanel field(String label, Component input, JLabel pesan) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        panel.add(labelField(label), BorderLayout.NORTH);
        panel.add(input, BorderLayout.CENTER);
        if (pesan != null) {
            panel.add(pesan, BorderLayout.SOUTH);
        }
        return panel;
    }

    static JScrollPane scrollTabel(JTable tabel) {
        return scrollTabel(tabel, TINGGI_TABEL_DEFAULT);
    }

    static JScrollPane scrollTabel(JTable tabel, int tinggi) {
        JScrollPane scroll = new JScrollPane(tabel);
        scroll.setBorder(BorderFactory.createLineBorder(GARIS));
        scroll.getViewport().setBackground(KARTU);
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
            isi.setBounds(0, 0, lebar, getHeight());
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

    static DefaultTableModel model(String... kolom) {
        return new DefaultTableModel(kolom, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
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
        tabel.getTableHeader().setFont(font(Font.BOLD, FONT_LABEL));
        tabel.getTableHeader().setForeground(TEKS_LEMBUT);
        tabel.getTableHeader().setBackground(LATAR);
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
    }
}
