package kantinflow.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

/**
 * Komponen dan warna bersama agar ketujuh halaman memakai ritme visual sama.
 */
final class Ui {
    static final Color LATAR = new Color(0xF4F6FA);
    static final Color KARTU = Color.WHITE;
    static final Color GARIS = new Color(0xE2E8F0);
    static final Color AKSEN = new Color(0x2563EB);
    static final Color AKSEN_GELAP = new Color(0x1D4ED8);
    static final Color TEKS = new Color(0x0F172A);
    static final Color TEKS_LEMBUT = new Color(0x64748B);
    static final Color HIJAU = new Color(0x067647);
    static final Color MERAH = new Color(0xB42318);
    static final Color KUNING = new Color(0xB54708);

    private Ui() {
    }

    static Font font(int gaya, float ukuran) {
        Font dasar = javax.swing.UIManager.getFont("Label.font");
        if (dasar == null) {
            dasar = new Font("Segoe UI", Font.PLAIN, 12);
        }
        return dasar.deriveFont(gaya, ukuran);
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
        label.setFont(font(Font.PLAIN, 12f));
        label.setForeground(TEKS);
        return label;
    }

    static JPanel header(String judul, String keterangan) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        panel.add(judul(judul), BorderLayout.NORTH);
        panel.add(Ui.keterangan(keterangan), BorderLayout.CENTER);
        return panel;
    }

    static JPanel kartu() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(KARTU);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GARIS),
                new EmptyBorder(16, 18, 16, 18)));
        return panel;
    }

    static JPanel kartu(String judul) {
        JPanel panel = kartu();
        JLabel label = new JLabel(judul);
        label.setFont(font(Font.BOLD, 14f));
        label.setForeground(TEKS);
        panel.add(label, BorderLayout.NORTH);
        return panel;
    }

    static JTextField field(int kolom) {
        JTextField field = new JTextField(kolom);
        field.setFont(font(Font.PLAIN, 12f));
        return field;
    }

    static JButton utama(String teks) {
        JButton tombol = new JButton(teks);
        tombol.setFont(font(Font.BOLD, 12f));
        tombol.setBackground(AKSEN);
        tombol.setForeground(Color.WHITE);
        tombol.setFocusPainted(false);
        tombol.putClientProperty("JButton.buttonType", "default");
        return tombol;
    }

    static JButton sekunder(String teks) {
        JButton tombol = new JButton(teks);
        tombol.setFont(font(Font.PLAIN, 12f));
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
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        panel.add(labelField(label), BorderLayout.NORTH);
        panel.add(input, BorderLayout.CENTER);
        return panel;
    }

    static JScrollPane scrollTabel(JTable tabel) {
        JScrollPane scroll = new JScrollPane(tabel);
        scroll.setBorder(BorderFactory.createLineBorder(GARIS));
        scroll.getViewport().setBackground(KARTU);
        scroll.setPreferredSize(new Dimension(600, 220));
        return scroll;
    }

    static JPanel kartuNilai(String judul, JLabel nilai, JLabel detail) {
        JPanel panel = kartu();
        panel.setLayout(new BorderLayout(0, 4));
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
        JPanel panel = new JPanel(new GridLayout(1, 2, 16, 0));
        panel.setOpaque(false);
        panel.add(kiri);
        panel.add(kanan);
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
        tabel.setFont(font(Font.PLAIN, 12f));
        tabel.setRowHeight(28);
        tabel.setFillsViewportHeight(true);
        tabel.setShowVerticalLines(false);
        tabel.setGridColor(GARIS);
        tabel.getTableHeader().setFont(font(Font.BOLD, 12f));
        return tabel;
    }
}
