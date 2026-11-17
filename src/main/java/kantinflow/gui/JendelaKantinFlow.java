package kantinflow.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
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
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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
        if (daftarBarang.isEmpty()) {
            throw new IllegalArgumentException("Database belum memiliki barang");
        }
        this.barang = daftarBarang.get(0);
        this.riwayat.addAll(penyimpanan.muatRiwayat(barang));

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                tutup();
            }
        });
        setMinimumSize(new Dimension(900, 600));
        setSize(1180, 700);
        setLocationRelativeTo(null);

        bangunTampilan();
        perbaruiComboBarang();
        perbaruiSemua();
        tampilkanPesan("Data dan riwayat dimuat dari database.", true);
    }

    private void bangunTampilan() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Ui.LATAR);
        root.add(buatSidebar(), BorderLayout.WEST);
        root.add(buatKonten(), BorderLayout.CENTER);
        setContentPane(root);
        tampilkanHalaman("ringkasan");
    }

    private JPanel buatSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(Color.WHITE);
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Ui.GARIS));

        JPanel atas = new JPanel();
        atas.setOpaque(false);
        atas.setLayout(new BoxLayout(atas, BoxLayout.Y_AXIS));
        atas.setBorder(new EmptyBorder(20, 18, 12, 18));

        JLabel merek = new JLabel("KantinFlow");
        merek.setFont(Ui.font(Font.BOLD, 22f));
        merek.setForeground(Ui.TEKS);
        merek.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel sub = new JLabel("PBO · stok kantin");
        sub.setFont(Ui.font(Font.PLAIN, 11f));
        sub.setForeground(Ui.TEKS_LEMBUT);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        atas.add(merek);
        atas.add(Box.createVerticalStrut(2));
        atas.add(sub);
        atas.add(Box.createVerticalStrut(18));

        JLabel labelAktif = new JLabel("Barang aktif");
        labelAktif.setFont(Ui.font(Font.BOLD, 11f));
        labelAktif.setForeground(Ui.TEKS_LEMBUT);
        labelAktif.setAlignmentX(Component.LEFT_ALIGNMENT);
        atas.add(labelAktif);
        atas.add(Box.createVerticalStrut(5));

        comboBarang.setName("barangAktif");
        comboBarang.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        comboBarang.setAlignmentX(Component.LEFT_ALIGNMENT);
        comboBarang.addActionListener(event -> {
            if (!menggantiBarang) {
                Barang dipilih = (Barang) comboBarang.getSelectedItem();
                if (dipilih != null && dipilih.getId() != barang.getId()) {
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
        atas.add(comboBarang);

        JPanel nav = new JPanel();
        nav.setOpaque(false);
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setBorder(new EmptyBorder(8, 10, 8, 10));
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
            tombol.setFont(Ui.font(Font.PLAIN, 12f));
            tombol.setHorizontalAlignment(SwingConstants.LEFT);
            tombol.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            tombol.setAlignmentX(Component.LEFT_ALIGNMENT);
            tombol.setFocusPainted(false);
            tombol.setOpaque(true);
            tombol.setBorder(new EmptyBorder(8, 12, 8, 12));
            tombol.addActionListener(event -> tampilkanHalaman(item[0]));
            tombol.addItemListener(event -> {
                if (tombol.isSelected()) {
                    tombol.setBackground(Ui.AKSEN);
                    tombol.setForeground(Color.WHITE);
                } else {
                    tombol.setBackground(Color.WHITE);
                    tombol.setForeground(Ui.TEKS);
                }
            });
            grupNavigasi.add(tombol);
            tombolNavigasi.put(item[0], tombol);
            nav.add(tombol);
            nav.add(Box.createVerticalStrut(3));
        }

        JLabel footer = new JLabel("SQLite lokal · FlatLaf 3.8");
        footer.setFont(Ui.font(Font.PLAIN, 10f));
        footer.setForeground(Ui.TEKS_LEMBUT);
        footer.setBorder(new EmptyBorder(10, 18, 16, 18));

        sidebar.add(atas, BorderLayout.NORTH);
        sidebar.add(nav, BorderLayout.CENTER);
        sidebar.add(footer, BorderLayout.SOUTH);
        return sidebar;
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
        status.setFont(Ui.font(Font.PLAIN, 12f));
        status.setBorder(new EmptyBorder(9, 18, 9, 18));
        status.setBackground(new Color(0xEEF2F7));
        status.setForeground(Ui.TEKS);
        status.setRows(2);

        JPanel konten = new JPanel(new BorderLayout());
        konten.setBackground(Ui.LATAR);
        konten.add(kartu, BorderLayout.CENTER);
        konten.add(status, BorderLayout.SOUTH);
        return konten;
    }

    private void tampilkanHalaman(String kunci) {
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
            this.tanggalPemantauan = null;
            perbaruiComboBarang();
            perbaruiSemua();
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
        if (dipilih == null || dipilih.getId() == barang.getId()) {
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

    private void perbaruiComboBarang() {
        menggantiBarang = true;
        try {
            comboBarang.removeAllItems();
            for (Barang item : daftarBarang) {
                comboBarang.addItem(item);
            }
            for (int i = 0; i < comboBarang.getItemCount(); i++) {
                if (comboBarang.getItemAt(i).getId() == barang.getId()) {
                    comboBarang.setSelectedIndex(i);
                    break;
                }
            }
        } finally {
            menggantiBarang = false;
        }
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
}
