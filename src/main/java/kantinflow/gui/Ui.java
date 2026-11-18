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
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

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

    private Ui() {
    }

    static Font font(int gaya, float ukuran) {
        Font dasar = javax.swing.UIManager.getFont("Label.font");
        String nama = dasar == null ? "Segoe UI" : dasar.getName();
        return new Font(nama, gaya, Math.max(10, Math.round(ukuran)));
    }

    static JLabel judul(String teks) {
        JLabel label = new JLabel(teks);
        label.setFont(font(Font.BOLD, 24f));
        label.setForeground(TEKS);
        return label;
    }

    static JLabel keterangan(String teks) {
        JLabel label = new JLabel(teks);
        label.setFont(font(Font.PLAIN, 12f));
        label.setForeground(TEKS_LEMBUT);
        return label;
    }

    static JLabel labelField(String teks) {
        JLabel label = new JLabel(teks);
        label.setFont(font(Font.BOLD, 11.5f));
        label.setForeground(TEKS_LEMBUT);
        return label;
    }

    static JLabel pesan() {
        JLabel label = new JLabel(" ");
        label.setFont(font(Font.PLAIN, 11f));
        label.setForeground(MERAH);
        return label;
    }

    static JLabel kosong(String teks) {
        JLabel label = new JLabel(teks);
        label.setFont(font(Font.PLAIN, 12f));
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
        KartuPanel panel = new KartuPanel(new BorderLayout(0, 10));
        panel.setBorder(new EmptyBorder(16, 18, 16, 18));
        return panel;
    }

    static JPanel kartu(String judul) {
        JPanel panel = kartu();
        JLabel label = new JLabel(judul);
        label.setFont(font(Font.BOLD, 16f));
        label.setForeground(TEKS);
        panel.add(label, BorderLayout.NORTH);
        return panel;
    }

    static JTextField field(int kolom) {
        JTextField field = new JTextField(kolom);
        field.setFont(font(Font.PLAIN, 13f));
        field.putClientProperty("JTextField.placeholderText", "");
        return field;
    }

    static JButton utama(String teks) {
        JButton tombol = new JButton(teks);
        tombol.setFont(font(Font.BOLD, 12f));
        tombol.setBackground(AKSEN);
        tombol.setForeground(Color.WHITE);
        tombol.setBorder(new EmptyBorder(9, 16, 9, 16));
        tombol.setFocusPainted(true);
        tombol.setOpaque(true);
        tombol.putClientProperty("JButton.buttonType", "roundRect");
        return tombol;
    }

    static JButton sekunder(String teks) {
        JButton tombol = new JButton(teks);
        tombol.setFont(font(Font.PLAIN, 12f));
        tombol.setBackground(KARTU);
        tombol.setForeground(TEKS);
        tombol.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GARIS_KUAT),
                new EmptyBorder(8, 14, 8, 14)));
        tombol.setFocusPainted(true);
        tombol.setOpaque(true);
        tombol.putClientProperty("JButton.buttonType", "roundRect");
        return tombol;
    }

    static JButton bahaya(String teks) {
        JButton tombol = new JButton(teks);
        tombol.setFont(font(Font.BOLD, 12f));
        tombol.setBackground(MERAH);
        tombol.setForeground(Color.WHITE);
        tombol.setBorder(new EmptyBorder(9, 16, 9, 16));
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
        JScrollPane scroll = new JScrollPane(tabel);
        scroll.setBorder(BorderFactory.createLineBorder(GARIS));
        scroll.getViewport().setBackground(KARTU);
        scroll.setPreferredSize(new Dimension(600, 230));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    static JPanel kartuNilai(String judul, JLabel nilai, JLabel detail) {
        JPanel panel = kartu();
        panel.setLayout(new BorderLayout(0, 5));
        JLabel label = new JLabel(judul);
        label.setFont(font(Font.PLAIN, 12f));
        label.setForeground(TEKS_LEMBUT);
        nilai.setFont(font(Font.BOLD, 24f));
        nilai.setForeground(TEKS);
        detail.setFont(font(Font.PLAIN, 11f));
        detail.setForeground(TEKS_LEMBUT);
        panel.add(label, BorderLayout.NORTH);
        panel.add(nilai, BorderLayout.CENTER);
        panel.add(detail, BorderLayout.SOUTH);
        return panel;
    }

    static JPanel duaKartu(Component kiri, Component kanan) {
        return gridResponsif(2, 760, kiri, kanan);
    }

    /**
     * Grid dua kolom pada lebar desktop dan satu kolom saat ruang menyempit.
     */
    static JPanel gridResponsif(int kolomLebar, int ambang, Component... komponen) {
        JPanel panel = new JPanel(new GridLayout(
                Math.max(1, (komponen.length + kolomLebar - 1) / kolomLebar),
                Math.min(kolomLebar, komponen.length), 16, 16));
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
                panel.setLayout(new GridLayout(baris, kolom, 16, 16));
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
        tabel.setFont(font(Font.PLAIN, 13f));
        tabel.setRowHeight(36);
        tabel.setFillsViewportHeight(true);
        tabel.setShowVerticalLines(false);
        tabel.setShowHorizontalLines(true);
        tabel.setIntercellSpacing(new Dimension(0, 1));
        tabel.setGridColor(GARIS);
        tabel.setBackground(KARTU);
        tabel.setSelectionBackground(AKSEN_LEMBUT);
        tabel.setSelectionForeground(TEKS);
        tabel.getTableHeader().setFont(font(Font.BOLD, 11.5f));
        tabel.getTableHeader().setForeground(TEKS_LEMBUT);
        tabel.getTableHeader().setBackground(LATAR);
        tabel.getTableHeader().setPreferredSize(new Dimension(0, 38));
        DefaultTableCellRenderer cell = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                setBorder(new EmptyBorder(0, 12, 0, 12));
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
                setBorder(new EmptyBorder(0, 12, 0, 12));
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
        private static final int ARC = 12;

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
