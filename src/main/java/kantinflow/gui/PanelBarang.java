package kantinflow.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.RowFilter;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableRowSorter;

import com.formdev.flatlaf.ui.FlatLineBorder;

import kantinflow.Barang;

/**
 * Halaman Barang: pencarian, daftar, dan dialog tambah/minimum.
 */
public class PanelBarang extends PanelKantin {
    private static final String LAINNYA = "Lainnya";
    private static final String[] SATUAN = {
        "buah", "kotak", "bungkus", "botol", "kaleng", LAINNYA
    };

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Nama", "Satuan", "Stok fisik", "Stok minimum", "Aksi"},
            0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 4;
        }
    };
    private final JTable tabel = Ui.tabel(model);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
    private final List<Barang> urutanBarang = new ArrayList<>();
    private final JTextField fieldCari = Ui.field(18);
    private final JButton tombolTambah = Ui.utama("Tambah Barang");
    private final EmptyState kosong = new EmptyState(
            "package", "Belum ada barang",
            "Tambahkan barang pertama melalui tombol Tambah Barang.");
    private final JPanel wadahTabel = new JPanel(new CardLayout());
    private JPanel kartuDaftar;
    private JPanel kepalaDaftar;
    private JScrollPane gulirTabel;
    private boolean memperbaruiTabel;

    public PanelBarang(KonteksKantin konteks) {
        super(konteks, "Barang",
                "Kelola daftar barang, stok fisik, dan batas minimum.");
        tabel.setName("tabelBarang");
        fieldCari.setName("cariBarang");
        fieldCari.putClientProperty("JTextField.placeholderText",
                "Cari nama barang");
        tombolTambah.setName("tombolTambahBarang");
        tombolTambah.setIcon(Ui.ikonAksi("plus", 16, java.awt.Color.WHITE));
        tombolTambah.setIconTextGap(7);
        kosong.setName("kosongBarang");
        tabel.setRowSorter(sorter);
        sorter.addRowSorterListener(event -> hentikanEditAksi());
        pasangRendererTabel();
        tabel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                int kolom = tabel.columnAtPoint(event.getPoint());
                tabel.setCursor(Cursor.getPredefinedCursor(
                        kolom == 4 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
        });
        tabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent event) {
                tabel.setCursor(Cursor.getDefaultCursor());
            }
        });
        fieldCari.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                perbaruiSaringan();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                perbaruiSaringan();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                perbaruiSaringan();
            }
        });
        tombolTambah.addActionListener(event -> bukaDialogTambah());
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                sesuaikanTinggiTabel();
            }

            @Override
            public void componentShown(ComponentEvent event) {
                sesuaikanTinggiTabel();
            }
        });
        bangunIsi();
    }

    private void bangunIsi() {
        JPanel isi = new JPanel(new BorderLayout(0, 14));
        isi.setOpaque(false);

        kartuDaftar = Ui.kartu("Daftar Barang", "package");
        kepalaDaftar = new JPanel(new BorderLayout(12, 0));
        kepalaDaftar.setOpaque(false);
        kepalaDaftar.add(fieldCari, BorderLayout.CENTER);
        kepalaDaftar.add(tombolTambah, BorderLayout.EAST);
        JPanel daftarIsi = new JPanel(new BorderLayout(0, 6));
        daftarIsi.setOpaque(false);
        daftarIsi.add(kepalaDaftar, BorderLayout.NORTH);
        gulirTabel = Ui.scrollTabel(tabel, 250);
        gulirTabel.setMinimumSize(new Dimension(0, 0));
        gulirTabel.setVerticalScrollBarPolicy(
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        gulirTabel.setHorizontalScrollBarPolicy(
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        wadahTabel.setOpaque(false);
        wadahTabel.add(gulirTabel, "tabel");
        wadahTabel.add(kosong, "kosong");
        daftarIsi.add(wadahTabel, BorderLayout.CENTER);
        kartuDaftar.add(daftarIsi, BorderLayout.CENTER);
        isi.add(kartuDaftar, BorderLayout.NORTH);
        add(isi, BorderLayout.CENTER);
        sesuaikanTinggiTabel();
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return true;
    }

    private void perbaruiSaringan() {
        hentikanEditAksi();
        String kueri = fieldCari.getText() == null
                ? "" : fieldCari.getText().trim();
        if (kueri.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            String cari = kueri.toLowerCase(Locale.ROOT);
            sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
                @Override
                public boolean include(Entry<? extends DefaultTableModel,
                        ? extends Integer> entry) {
                    String nama = String.valueOf(entry.getValue(0));
                    return nama.toLowerCase(Locale.ROOT).contains(cari);
                }
            });
        }
        perbaruiKeadaan();
    }

    private void pasangRendererTabel() {
        for (int i = 0; i < 4; i++) {
            tabel.getColumnModel().getColumn(i)
                    .setCellRenderer(new PenyajiBarang());
        }
        tabel.getColumnModel().getColumn(4)
                .setCellRenderer(new PenyajiAksi());
        tabel.getColumnModel().getColumn(4)
                .setCellEditor(new EditorAksi());
        Ui.rataKananHeader(tabel, 2, 3);
        aturProporsiKolom();
        Ui.gulirHorizontalSaatSempit(tabel);
    }

    /**
     * Proporsi awal 30/15/17/20/18 dengan lebar minimum agar header dan
     * tombol Atur Minimum tetap terbaca.
     */
    private void aturProporsiKolom() {
        int[] bobot = {30, 15, 17, 20, 18};
        int[] minimum = {170, 80, 85, 105, 56};
        tabel.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        for (int i = 0; i < bobot.length; i++) {
            tabel.getColumnModel().getColumn(i).setPreferredWidth(
                    Ui.skala(bobot[i] * 10));
            tabel.getColumnModel().getColumn(i).setMinWidth(
                    Ui.skala(minimum[i]));
        }
        int lebarAksi = Ui.skala(56);
        tabel.getColumnModel().getColumn(4).setPreferredWidth(lebarAksi);
        tabel.getColumnModel().getColumn(4).setMinWidth(lebarAksi);
        tabel.getColumnModel().getColumn(4).setMaxWidth(lebarAksi);
    }

    /**
     * Tinggi tabel mengikuti seluruh baris selama muat. Jika melebihi ruang
     * viewport, tinggi dibatasi agar scroll hanya terjadi di dalam tabel.
     */
    private void sesuaikanTinggiTabel() {
        if (gulirTabel == null || kartuDaftar == null
                || kepalaDaftar == null) {
            return;
        }
        JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(
                JViewport.class, this);
        int tinggiTersedia = viewport != null
                ? viewport.getExtentSize().height : getHeight();
        if (tinggiTersedia <= 0) {
            return;
        }
        int tinggiKonten = tinggiTersedia
                - getInsets().top - getInsets().bottom;
        Insets kartu = kartuDaftar.getInsets();
        Component judul = ((BorderLayout) kartuDaftar.getLayout())
                .getLayoutComponent(BorderLayout.NORTH);
        int tinggiTetap = kartu.top + kartu.bottom
                + (judul == null ? 0 : judul.getPreferredSize().height)
                + Ui.skala(Ui.GAP_KARTU)
                + kepalaDaftar.getPreferredSize().height + 6;
        int tinggiMaksimum = Math.max(
                tabel.getTableHeader().getPreferredSize().height
                        + tabel.getRowHeight(),
                tinggiKonten - tinggiTetap - Ui.skala(2));
        int tinggiAlami = tabel.getTableHeader().getPreferredSize().height
                + tabel.getRowCount() * tabel.getRowHeight() + 2;
        int tinggi = Math.min(tinggiAlami, tinggiMaksimum);
        gulirTabel.setPreferredSize(new Dimension(0, tinggi));
        kartuDaftar.revalidate();
    }

    private void hentikanEditAksi() {
        if (tabel.isEditing()) {
            TableCellEditor editor = tabel.getCellEditor();
            if (editor != null) {
                editor.stopCellEditing();
            }
        }
    }

    private void perbaruiKeadaan() {
        boolean tanpaData = urutanBarang.isEmpty();
        boolean tanpaTampilan = tabel.getRowCount() == 0;
        if (tanpaTampilan) {
            kosong.setTitle(tanpaData
                    ? "Belum ada barang" : "Tidak ada hasil");
            kosong.setDescription(tanpaData
                    ? "Tambahkan barang pertama melalui tombol Tambah Barang."
                    : "Tidak ada barang yang cocok dengan pencarian.");
        }
        ((CardLayout) wadahTabel.getLayout()).show(
                wadahTabel, tanpaTampilan ? "kosong" : "tabel");
        sesuaikanTinggiTabel();
    }

    private Barang barangDipilih() {
        int barisTampilan = tabel.getSelectedRow();
        if (barisTampilan < 0) {
            return null;
        }
        int barisModel = tabel.convertRowIndexToModel(barisTampilan);
        if (barisModel < 0 || barisModel >= urutanBarang.size()) {
            return null;
        }
        return urutanBarang.get(barisModel);
    }

    private void bukaDialogTambah() {
        JDialog dialog = dialogBaru("Tambah Barang");
        JPanel form = Ui.kartu("Barang Baru");
        JPanel isian = new JPanel(new GridBagLayout());
        isian.setOpaque(false);
        int baris = 0;

        JTextField nama = Ui.field(24);
        nama.setName("namaBarangBaru");
        JLabel pesanNama = Ui.pesan();
        pesanNama.setName("pesanNamaBarang");
        JLabel petunjukNama = Ui.keterangan("Contoh: Susu Kotak atau Roti");
        JPanel kolomNama = Ui.field("Nama barang", nama, pesanNama);
        tambahBaris(isian, kolomNama, baris++);
        tambahBaris(isian, petunjukNama, baris++);

        JComboBox<String> combo = Ui.combo();
        combo.setName("satuanBarangBaru");
        combo.setModel(new javax.swing.DefaultComboBoxModel<>(SATUAN));
        combo.setSelectedItem("buah");
        JLabel pesanSatuan = Ui.pesan();
        pesanSatuan.setName("pesanSatuanBarang");
        tambahBaris(isian, Ui.field("Satuan", combo, pesanSatuan), baris++);
        JTextField satuanLain = Ui.field(20);
        satuanLain.setName("satuanKhususBarang");
        JLabel pesanSatuanLain = Ui.pesan();
        pesanSatuanLain.setName("pesanSatuanLain");
        JPanel panelSatuanLain = Ui.field(
                "Satuan khusus", satuanLain, pesanSatuanLain);
        panelSatuanLain.setVisible(false);
        tambahBaris(isian, panelSatuanLain, baris++);
        JLabel petunjukSatuan = Ui.keterangan(
                "Satuan adalah jenis hitungan, bukan jumlah stok. "
                + "Contoh: buah atau kotak.");
        tambahBaris(isian, petunjukSatuan, baris++);

        JLabel infoStok = Ui.keterangan(
                "Barang baru belum memiliki stok. Catat jumlah awal melalui Stok Masuk.");
        infoStok.setName("infoStokBarangBaru");
        tambahBaris(isian, infoStok, baris++);
        JLabel pesanUmum = Ui.pesan();
        pesanUmum.setName("pesanTambahBarang");
        tambahBaris(isian, pesanUmum, baris++);
        form.add(isian, BorderLayout.CENTER);

        combo.addActionListener(event -> {
            boolean lain = LAINNYA.equals(combo.getSelectedItem());
            panelSatuanLain.setVisible(lain);
            satuanLain.setEnabled(lain);
            form.revalidate();
            form.repaint();
            dialog.pack();
            sesuaikanUkuran(dialog);
        });
        bersihkanSaatBerubah(nama, pesanNama);
        bersihkanSaatBerubah(satuanLain, pesanSatuanLain);
        bersihkanSaatBerubah(nama, pesanUmum);

        JButton batal = Ui.sekunder("Batal");
        batal.setName("batalTambahBarang");
        JButton simpan = Ui.utama("Tambah Barang");
        simpan.setName("simpanTambahBarang");
        batal.addActionListener(event -> dialog.dispose());
        simpan.addActionListener(event -> {
            pesanNama.setText(" ");
            pesanSatuan.setText(" ");
            pesanSatuanLain.setText(" ");
            pesanUmum.setText(" ");
            String namaBaru = nama.getText() == null
                    ? "" : nama.getText().trim();
            if (namaBaru.isEmpty()) {
                pesanNama.setText("Nama barang wajib diisi.");
                nama.requestFocusInWindow();
                return;
            }
            String satuan = LAINNYA.equals(combo.getSelectedItem())
                    ? (satuanLain.getText() == null
                            ? "" : satuanLain.getText().trim())
                    : String.valueOf(combo.getSelectedItem());
            String masalahSatuan = validasiSatuan(satuan);
            if (masalahSatuan != null) {
                if (LAINNYA.equals(combo.getSelectedItem())) {
                    pesanSatuanLain.setText(masalahSatuan);
                    satuanLain.requestFocusInWindow();
                } else {
                    pesanSatuan.setText(masalahSatuan);
                    combo.requestFocusInWindow();
                }
                return;
            }
            if (!konteks.tambahBarang(namaBaru, satuan)) {
                pesanUmum.setText(
                        "Barang gagal ditambahkan. Periksa nama barang atau data terkait.");
            } else {
                dialog.dispose();
            }
        });

        tampilkanDialog(dialog, form, batal, simpan, nama);
    }

    private String validasiSatuan(String satuan) {
        String nilai = satuan == null ? "" : satuan.trim();
        if (nilai.isEmpty()) {
            return "Satuan wajib diisi.";
        }
        if (nilai.matches("\\d+")) {
            return "Satuan harus berupa jenis hitungan, misalnya buah atau kotak.";
        }
        return null;
    }

    private void bukaDialogMinimum(Barang barang) {
        JDialog dialog = dialogBaru("Atur Minimum");
        JPanel form = Ui.kartu("Stok Minimum");
        JPanel isian = new JPanel(new GridBagLayout());
        isian.setOpaque(false);
        int baris = 0;

        JLabel namaBarang = Ui.labelField(
                barang.getNama() + " (" + barang.getSatuan() + ")");
        namaBarang.setName("barangMinimum");
        tambahBaris(isian, namaBarang, baris++);
        JTextField field = Ui.field(10);
        field.setName("minimumBarang");
        field.setText(String.valueOf(barang.getStokMinimum()));
        JLabel pesan = Ui.pesan();
        pesan.setName("pesanMinimumBarang");
        tambahBaris(isian, Ui.field("Stok minimum", field, pesan), baris++);
        JLabel petunjuk = Ui.keterangan(
                "Peringatan restok muncul jika stok layak tersisa sejumlah ini "
                + "atau kurang.");
        tambahBaris(isian, petunjuk, baris++);
        form.add(isian, BorderLayout.CENTER);

        JButton batal = Ui.sekunder("Batal");
        batal.setName("batalMinimumBarang");
        JButton simpan = Ui.utama("Simpan Minimum");
        simpan.setName("simpanMinimumBarang");
        batal.addActionListener(event -> dialog.dispose());
        field.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void berubah() {
                if (!" ".equals(pesan.getText())
                        && validasiMinimum(field.getText()) == null) {
                    pesan.setText(" ");
                }
            }
        });
        simpan.addActionListener(event -> {
            pesan.setText(" ");
            String masalah = validasiMinimum(field.getText());
            if (masalah != null) {
                pesan.setText(masalah);
                field.requestFocusInWindow();
                return;
            }
            int nilai = Integer.parseInt(field.getText().trim());
            if (!konteks.simpanStokMinimum(barang, nilai)) {
                pesan.setText("Minimum gagal disimpan. Lihat status di bawah.");
            } else {
                dialog.dispose();
            }
        });

        tampilkanDialog(dialog, form, batal, simpan, field);
    }

    /**
     * Mengembalikan pesan kesalahan, atau null bila nilai dapat dipakai.
     */
    private String validasiMinimum(String teks) {
        String nilai = teks == null ? "" : teks.trim();
        if (nilai.isEmpty()) {
            return "Stok minimum wajib diisi.";
        }
        if (nilai.startsWith("-")) {
            return "Stok minimum tidak boleh negatif.";
        }
        if (nilai.contains(".") || nilai.contains(",")) {
            return "Stok minimum harus berupa bilangan bulat tanpa pecahan.";
        }
        try {
            int angka = Integer.parseInt(nilai);
            if (angka < 0) {
                return "Stok minimum tidak boleh negatif.";
            }
            return null;
        } catch (NumberFormatException exception) {
            return "Stok minimum harus berupa bilangan bulat 0 atau lebih.";
        }
    }

    private static void tambahBaris(JPanel panel, Component komponen,
            int baris) {
        GridBagConstraints batas = new GridBagConstraints();
        batas.gridx = 0;
        batas.gridy = baris;
        batas.weightx = 1;
        batas.fill = GridBagConstraints.HORIZONTAL;
        batas.anchor = GridBagConstraints.WEST;
        batas.insets = new Insets(0, 0, 4, 0);
        panel.add(komponen, batas);
    }

    private JDialog dialogBaru(String judul) {
        JDialog dialog = new JDialog(
                SwingUtilities.getWindowAncestor(this), judul,
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setName("dialog" + judul.replace(" ", ""));
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        return dialog;
    }

    private void tampilkanDialog(JDialog dialog, JPanel form,
            JButton batal, JButton simpan, JTextField fokus) {
        Notifikasi.bersihkan();
        JPanel konten = new JPanel(new BorderLayout(0, 12));
        konten.setOpaque(true);
        konten.setBackground(Ui.LATAR);
        konten.setBorder(new EmptyBorder(16, 16, 16, 16));
        JScrollPane scroll = new JScrollPane(form,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Ui.LATAR);
        JPanel aksi = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        aksi.setOpaque(false);
        aksi.add(batal);
        aksi.add(simpan);
        konten.add(scroll, BorderLayout.CENTER);
        konten.add(aksi, BorderLayout.SOUTH);
        dialog.setContentPane(konten);
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent event) {
                dialog.toFront();
                fokus.requestFocusInWindow();
                fokus.requestFocus();
            }
        });
        dialog.setAutoRequestFocus(true);
        dialog.getRootPane().setDefaultButton(simpan);
        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "tutup");
        dialog.getRootPane().getActionMap().put("tutup", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dialog.dispose();
            }
        });
        dialog.pack();
        sesuaikanUkuran(dialog);
        dialog.setLocationRelativeTo(this);
        SwingUtilities.invokeLater(() -> {
            fokus.requestFocusInWindow();
            fokus.selectAll();
        });
        javax.swing.Timer fokusTimer = new javax.swing.Timer(80, event -> {
            dialog.toFront();
            fokus.requestFocusInWindow();
            fokus.requestFocus();
        });
        fokusTimer.setRepeats(false);
        fokusTimer.start();
        dialog.setVisible(true);
    }

    private static void sesuaikanUkuran(JDialog dialog) {
        Rectangle batas = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getMaximumWindowBounds();
        int lebar = Math.min(dialog.getWidth(),
                Math.max(360, batas.width - 48));
        int tinggi = Math.min(dialog.getHeight(),
                Math.max(300, batas.height - 64));
        dialog.setSize(new Dimension(lebar, tinggi));
    }

    private static void bersihkanSaatBerubah(JTextField field, JLabel pesan) {
        field.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void berubah() {
                if (!" ".equals(pesan.getText())) {
                    pesan.setText(" ");
                }
            }
        });
    }

    @Override
    public void perbarui() {
        hentikanEditAksi();
        Integer idTerpilih = barangDipilih() == null
                ? null : barangDipilih().getId();
        memperbaruiTabel = true;
        try {
            urutanBarang.clear();
            urutanBarang.addAll(konteks.getDaftarBarang());
            model.setRowCount(0);
            for (Barang barang : urutanBarang) {
                model.addRow(new Object[]{
                    barang.getNama(),
                    barang.getSatuan(),
                    barang.hitungSaldo(),
                    barang.getStokMinimum(),
                    "Atur Minimum"
                });
            }
            tabel.clearSelection();
            if (idTerpilih != null) {
                for (int i = 0; i < urutanBarang.size(); i++) {
                    if (urutanBarang.get(i).getId() == idTerpilih) {
                        int tampilan = tabel.convertRowIndexToView(i);
                        if (tampilan >= 0) {
                            tabel.setRowSelectionInterval(tampilan, tampilan);
                        }
                        break;
                    }
                }
            }
            perbaruiSaringan();
        } finally {
            memperbaruiTabel = false;
        }
    }

    private abstract static class SimpleDocumentListener
            implements DocumentListener {
        protected abstract void berubah();

        @Override
        public void insertUpdate(DocumentEvent event) {
            berubah();
        }

        @Override
        public void removeUpdate(DocumentEvent event) {
            berubah();
        }

        @Override
        public void changedUpdate(DocumentEvent event) {
            berubah();
        }
    }

    private final class PenyajiBarang extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            setBorder(new EmptyBorder(0, Ui.skala(Ui.PADDING_SEL_TABEL),
                    0, Ui.skala(Ui.PADDING_SEL_TABEL)));
            setHorizontalAlignment(column >= 2 && column < 4
                    ? javax.swing.SwingConstants.RIGHT
                    : javax.swing.SwingConstants.LEFT);
            setVerticalAlignment(javax.swing.SwingConstants.CENTER);
            int hover = table instanceof TabelKantin
                    ? ((TabelKantin) table).getBarisHover() : -1;
            setBackground(isSelected
                    ? table.getSelectionBackground()
                    : (row == hover ? Ui.HOVER : Ui.KARTU));
            setForeground(Ui.TEKS);
            setFont(Ui.font(Font.PLAIN, Ui.FONT_BODY));
            setToolTipText(null);
            return this;
        }
    }

    private String namaAksi(int viewRow) {
        int modelRow = tabel.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= urutanBarang.size()) {
            return "";
        }
        return urutanBarang.get(modelRow).getNama();
    }

    private void aturTombolAksi(JButton tombol, int row, String nama,
            boolean hover) {
        String label = "Atur stok minimum " + nama;
        tombol.setToolTipText(label);
        tombol.getAccessibleContext().setAccessibleName(label);
        tombol.setIcon(Ui.ikonAksi("pencil", 16,
                hover ? Ui.AKSEN_GELAP : Ui.AKSEN));
        tombol.setContentAreaFilled(hover);
        tombol.setOpaque(hover);
        tombol.setBackground(hover ? Ui.HOVER : Ui.KARTU);
        tombol.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private JButton buatTombolAksi() {
        JButton tombol = new JButton();
        tombol.setText("");
        tombol.setPreferredSize(new Dimension(
                Ui.skala(32), Ui.skala(32)));
        tombol.setMinimumSize(tombol.getPreferredSize());
        tombol.setMaximumSize(tombol.getPreferredSize());
        tombol.setBorder(new EmptyBorder(0,
                Ui.skala(Ui.PADDING_SEL_TABEL), 0, 0));
        tombol.setContentAreaFilled(false);
        tombol.setBorderPainted(true);
        tombol.setFocusPainted(true);
        tombol.setOpaque(false);
        tombol.setHorizontalAlignment(SwingConstants.LEFT);
        tombol.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                tombol.setBorder(new CompoundBorder(
                        new FlatLineBorder(new Insets(
                                Ui.skala(1), Ui.skala(1),
                                Ui.skala(1), Ui.skala(1)),
                                Ui.AKSEN, 1.5f, Ui.RADIUS_KONTROL),
                        new EmptyBorder(0, Ui.skala(
                                Ui.PADDING_SEL_TABEL - 1), 0, 0)));
            }

            @Override
            public void focusLost(FocusEvent event) {
                tombol.setBorder(new EmptyBorder(0,
                        Ui.skala(Ui.PADDING_SEL_TABEL), 0, 0));
            }
        });
        return tombol;
    }

    private final class PenyajiAksi extends DefaultTableCellRenderer {
        private final JButton tombol = buatTombolAksi();

        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            String nama = namaAksi(row);
            boolean hover = !isSelected && table instanceof TabelKantin
                    && row == ((TabelKantin) table).getBarisHover();
            aturTombolAksi(tombol, row, nama, hover);
            return tombol;
        }
    }

    private final class EditorAksi extends AbstractCellEditor
            implements TableCellEditor {
        private final JButton tombol = buatTombolAksi();

        private EditorAksi() {
            tombol.addActionListener(event -> {
                int viewRow = tabel.getEditingRow();
                stopCellEditing();
                if (viewRow >= 0) {
                    int modelRow = tabel.convertRowIndexToModel(viewRow);
                    if (modelRow >= 0 && modelRow < urutanBarang.size()) {
                        bukaDialogMinimum(urutanBarang.get(modelRow));
                    }
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table,
                Object value, boolean isSelected, int row, int column) {
            aturTombolAksi(tombol, row, namaAksi(row), false);
            return tombol;
        }

        @Override
        public Object getCellEditorValue() {
            return "Atur Minimum";
        }
    }
}
