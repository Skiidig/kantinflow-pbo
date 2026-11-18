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

/**
 * Komponen dan token visual bersama untuk tujuh halaman Swing/FlatLaf.
 */
final class Ui {
    static final Color LATAR = new Color(0xF5F6F8);
    static final Color KARTU = Color.WHITE;
    static final Color GARIS = new Color(0xE2E5EA);
    static final Color GARIS_KUAT = new Color(0xCFD5DE);
    static final Color AKSEN = new Color(0x014BAA);
    static final Color AKSEN_GELAP = new Color(0x013E8C);
    static final Color AKSEN_LEMBUT = new Color(0xE9EFF8);
    static final Color TEKS = new Color(0x20242C);
    static final Color TEKS_LEMBUT = new Color(0x606978);
    static final Color HIJAU = new Color(0x0F7A4D);
    static final Color HIJAU_LEMBUT = new Color(0xEAF5EF);
    static final Color MERAH = new Color(0xB42318);
    static final Color MERAH_LEMBUT = new Color(0xFDECEA);
    static final Color KUNING = new Color(0x8A5A00);
    static final Color KUNING_LEMBUT = new Color(0xFFF4D6);
    static final Color HOVER = new Color(0xF8F3F0);

    static final int RADIUS_KARTU = 12;
    static final int RADIUS_KONTROL = 8;
    static final int TINGGI_INPUT = 34;
    static final int TINGGI_TOMBOL = 36;
    static final int TINGGI_BARIS_TABEL = 36;
    static final int TINGGI_HEADER_TABEL = 38;
    static final int PADDING_SEL_TABEL = 12;
    static final int LEBAR_SIDEBAR = 240;
    static final int LEBAR_JENDELA = 1180;
    static final int TINGGI_JENDELA = 720;
    static final int LEBAR_MINIMUM = 960;
    static final int TINGGI_MINIMUM = 640;
    static final int AMBANG_FORM_SEMPIT = 760;
    static final int LEBAR_FORM_KELUAR = 600;
    static final int LEBAR_KOMBO_BARANG = 230;
    static final int TINGGI_FORM_MANUAL = 260;
    static final int TINGGI_FORM_FEFO = 300;
    static final int TINGGI_TABEL_BARANG = 175;
    static final int TINGGI_TABEL_STOK_MASUK = 260;
    static final int TINGGI_TABEL_PEMANTAUAN = 240;
    static final int TINGGI_TABEL_RIWAYAT = 300;
    static final int TINGGI_TABEL_DEFAULT = 230;
    static final Insets PADDING_HALAMAN = new Insets(22, 24, 22, 24);
    static final Insets PADDING_KARTU = new Insets(16, 18, 16, 18);
    static final int GAP_KARTU = 10;
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

    static Font font(int gaya, float ukuran) {
        Font dasar = javax.swing.UIManager.getFont("Label.font");
        String nama = dasar == null ? "Segoe UI" : dasar.getName();
        return new Font(nama, gaya, Math.max(10, Math.round(ukuran)));
    }

    static Icon ikonMenu(String kunci, int ukuran, Color warna) {
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
        return label;
    }

    static JLabel labelField(String teks) {
        JLabel label = new JLabel(teks);
        label.setFont(font(Font.BOLD, FONT_LABEL));
        label.setForeground(TEKS_LEMBUT);
        return label;
    }

    static JLabel pesan() {
        JLabel label = new JLabel(" ");
        label.setFont(font(Font.PLAIN, FONT_KECIL));
        label.setForeground(MERAH);
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
        KartuPanel panel = new KartuPanel(new BorderLayout(0, GAP_KARTU));
        panel.setBorder(new EmptyBorder(PADDING_KARTU));
        return panel;
    }

    static JPanel kartu(String judul) {
        JPanel panel = kartu();
        JLabel label = new JLabel(judul);
        label.setFont(font(Font.BOLD, FONT_KARTU));
        label.setForeground(TEKS);
        panel.add(label, BorderLayout.NORTH);
        return panel;
    }

    static JTextField field(int kolom) {
        JTextField field = new JTextField(kolom);
        field.setFont(font(Font.PLAIN, FONT_BODY));
        field.setPreferredSize(new Dimension(field.getPreferredSize().width,
                TINGGI_INPUT));
        field.putClientProperty("JTextField.placeholderText", "");
        return field;
    }

    static JButton utama(String teks) {
        JButton tombol = new JButton(teks);
        tombol.setFont(font(Font.BOLD, FONT_BUTTON));
        tombol.setBackground(AKSEN);
        tombol.setForeground(Color.WHITE);
        tombol.setBorder(new EmptyBorder(0, 16, 0, 16));
        tombol.setPreferredSize(new Dimension(tombol.getPreferredSize().width,
                TINGGI_TOMBOL));
        tombol.setFocusPainted(true);
        tombol.setOpaque(true);
        tombol.putClientProperty("JButton.buttonType", "roundRect");
        return tombol;
    }

    static JButton sekunder(String teks) {
        JButton tombol = new JButton(teks);
        tombol.setFont(font(Font.PLAIN, FONT_BUTTON));
        tombol.setBackground(KARTU);
        tombol.setForeground(TEKS);
        tombol.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GARIS_KUAT),
                new EmptyBorder(0, 14, 0, 14)));
        tombol.setPreferredSize(new Dimension(tombol.getPreferredSize().width,
                TINGGI_TOMBOL));
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
        tombol.setBorder(new EmptyBorder(0, 16, 0, 16));
        tombol.setPreferredSize(new Dimension(tombol.getPreferredSize().width,
                TINGGI_TOMBOL));
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
        combo.setPreferredSize(new Dimension(220, TINGGI_INPUT));
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
        scroll.setPreferredSize(new Dimension(600, tinggi));
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
                Math.min(kolomLebar, komponen.length), GAP_FORM, GAP_FORM));
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
                panel.setLayout(new GridLayout(baris, kolom, GAP_FORM, GAP_FORM));
                panel.revalidate();
                panel.repaint();
            }
        });
        return panel;
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
        JTable tabel = new JTable(model);
        tabel.setFont(font(Font.PLAIN, FONT_BODY));
        tabel.setRowHeight(TINGGI_BARIS_TABEL);
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
        tabel.getTableHeader().setPreferredSize(
                new Dimension(0, TINGGI_HEADER_TABEL));
        DefaultTableCellRenderer cell = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                setBorder(new EmptyBorder(0, PADDING_SEL_TABEL, 0, PADDING_SEL_TABEL));
                return this;
            }
        };
        tabel.setDefaultRenderer(Object.class, cell);
        return tabel;
    }

    static void rataKanan(JTable tabel, int... kolom) {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                setBorder(new EmptyBorder(0, PADDING_SEL_TABEL, 0, PADDING_SEL_TABEL));
                setHorizontalAlignment(SwingConstants.RIGHT);
                return this;
            }
        };
        for (int index : kolom) {
            if (index >= 0 && index < tabel.getColumnModel().getColumnCount()) {
                tabel.getColumnModel().getColumn(index).setCellRenderer(renderer);
            }
        }
    }

    private static final class KartuPanel extends JPanel {
        private static final int ARC = RADIUS_KARTU;

        private KartuPanel(LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(KARTU);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                        ARC, ARC);
                g2.setColor(GARIS);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                        ARC, ARC);
            } finally {
                g2.dispose();
            }
            super.paintComponent(graphics);
        }
    }
}
