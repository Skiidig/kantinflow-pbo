package kantinflow.gui;

import java.awt.BorderLayout;
import java.awt.Component;
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
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.AbstractAction;
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
import javax.swing.KeyStroke;
import javax.swing.RowFilter;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import kantinflow.Barang;

/**
 * Halaman Barang: pencarian, daftar, aksi barang terpilih, dan dialog tambah/minimum.
 */
public class PanelBarang extends PanelKantin {
    private static final String SEMUA = "Semua";
    private static final String LAINNYA = "Lainnya";
    private static final String[] SATUAN = {
        "buah", "kotak", "bungkus", "botol", "kaleng", LAINNYA
    };

    private final DefaultTableModel model = Ui.model(
            "Nama", "Satuan", "Stok fisik", "Stok minimum");
    private final JTable tabel = Ui.tabel(model);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
    private final List<Barang> urutanBarang = new ArrayList<>();
    private final JTextField fieldCari = Ui.field(18);
    private final JButton tombolTambah = Ui.utama("Tambah Barang");
    private final JButton tombolAktif = Ui.utama("Jadikan Barang Aktif");
    private final JButton tombolMinimum = Ui.sekunder("Atur Minimum");
    private final JLabel labelAktif = Ui.keterangan("Barang aktif: -");
    private final JLabel labelDipilih = Ui.keterangan("Belum ada barang dipilih.");
    private final EmptyState kosong = new EmptyState(
            "package", "Belum ada barang",
            "Tambahkan barang pertama melalui tombol Tambah Barang.");
    private boolean memperbaruiTabel;

    public PanelBarang(KonteksKantin konteks) {
        super(konteks, "Barang",
                "Kelola daftar barang, stok fisik, dan batas minimum.");
        tabel.setName("tabelBarang");
        fieldCari.setName("cariBarang");
        tombolTambah.setName("tombolTambahBarang");
        tombolTambah.setIcon(Ui.ikonAksi("plus", 16, java.awt.Color.WHITE));
        tombolTambah.setIconTextGap(7);
        tombolAktif.setName("jadikanBarangAktif");
        tombolAktif.setIcon(Ui.ikonAksi("check", 16, java.awt.Color.WHITE));
        tombolAktif.setIconTextGap(7);
        tombolMinimum.setName("aturMinimum");
        tombolMinimum.setIcon(Ui.ikonAksi("wallet", 16, Ui.TEKS_LEMBUT));
        tombolMinimum.setIconTextGap(7);
        labelAktif.setName("labelBarangAktif");
        labelDipilih.setName("labelBarangDipilih");
        kosong.setName("kosongBarang");
        tombolAktif.setEnabled(false);
        tombolMinimum.setEnabled(false);
        tabel.setRowSorter(sorter);
        for (int i = 0; i < tabel.getColumnModel().getColumnCount(); i++) {
            tabel.getColumnModel().getColumn(i).setCellRenderer(new PenyajiBarang());
        }
        Ui.rataKananHeader(tabel, 2, 3);
        Ui.aturKolom(tabel, 260, 100, 120, 130);
        tabel.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && !memperbaruiTabel) {
                perbaruiAksi();
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
        tombolAktif.addActionListener(event -> jadikanAktif());
        tombolMinimum.addActionListener(event -> {
            Barang dipilih = barangDipilih();
            if (dipilih != null) {
                bukaDialogMinimum(dipilih);
            }
        });
        bangunIsi();
    }

    private void bangunIsi() {
        JPanel isi = new JPanel(new BorderLayout(0, 14));
        isi.setOpaque(false);

        JPanel atas = Ui.kartu();
        JPanel atasIsi = new JPanel();
        atasIsi.setOpaque(false);
        atasIsi.setLayout(new BoxLayout(atasIsi, BoxLayout.Y_AXIS));
        JLabel petunjuk = Ui.keterangan(
                "Pilih barang untuk melihat tindakannya. "
                + "Barang aktif digunakan pada halaman transaksi.");
        petunjuk.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel cariBaris = new JPanel(new BorderLayout(12, 0));
        cariBaris.setOpaque(false);
        cariBaris.setAlignmentX(Component.LEFT_ALIGNMENT);
        cariBaris.add(Ui.field("Cari nama barang", fieldCari),
                BorderLayout.CENTER);
        cariBaris.add(Ui.field(" ", tombolTambah), BorderLayout.EAST);
        atasIsi.add(petunjuk);
        atasIsi.add(Box.createVerticalStrut(10));
        atasIsi.add(cariBaris);
        atas.add(atasIsi, BorderLayout.CENTER);

        JPanel daftar = Ui.kartu("Daftar Barang", "package");
        JPanel daftarIsi = new JPanel(new BorderLayout(0, 6));
        daftarIsi.setOpaque(false);
        daftarIsi.add(Ui.scrollTabel(tabel, 150), BorderLayout.CENTER);
        daftarIsi.add(kosong, BorderLayout.SOUTH);
        daftar.add(daftarIsi, BorderLayout.CENTER);

        JPanel aksi = Ui.kartu("Aksi Barang", "check");
        JPanel aksiIsi = new JPanel(new BorderLayout(12, 6));
        aksiIsi.setOpaque(false);
        JPanel informasi = new JPanel();
        informasi.setOpaque(false);
        informasi.setLayout(new BoxLayout(informasi, BoxLayout.Y_AXIS));
        labelAktif.setFont(Ui.font(Font.BOLD, Ui.FONT_CATATAN));
        labelAktif.setForeground(Ui.AKSEN);
        labelAktif.setAlignmentX(Component.LEFT_ALIGNMENT);
        labelDipilih.setAlignmentX(Component.LEFT_ALIGNMENT);
        informasi.add(labelAktif);
        informasi.add(Box.createVerticalStrut(3));
        informasi.add(labelDipilih);
        JPanel tombolAksi = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        tombolAksi.setOpaque(false);
        tombolAksi.add(tombolAktif);
        tombolAksi.add(tombolMinimum);
        aksiIsi.add(informasi, BorderLayout.CENTER);
        aksiIsi.add(tombolAksi, BorderLayout.EAST);
        aksi.add(aksiIsi, BorderLayout.CENTER);

        isi.add(atas, BorderLayout.NORTH);
        isi.add(daftar, BorderLayout.CENTER);
        isi.add(aksi, BorderLayout.SOUTH);
        add(isi, BorderLayout.CENTER);
    }

    private void perbaruiSaringan() {
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
        kosong.setVisible(tanpaTampilan);
        perbaruiAksi();
    }

    private void perbaruiAksi() {
        Barang dipilih = barangDipilih();
        Barang aktif = konteks.getBarangAktif();
        labelDipilih.setText(dipilih == null
                ? "Belum ada barang dipilih."
                : "Dipilih: " + dipilih.getNama()
                + " (" + dipilih.getSatuan() + ")");
        tombolAktif.setEnabled(dipilih != null
                && (aktif == null || dipilih.getId() != aktif.getId()));
        tombolMinimum.setEnabled(dipilih != null);
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

    private void jadikanAktif() {
        Barang dipilih = barangDipilih();
        if (dipilih == null) {
            return;
        }
        Barang aktif = konteks.getBarangAktif();
        if (aktif != null && aktif.getId() == dipilih.getId()) {
            return;
        }
        konteks.gantiBarang(dipilih);
        perbaruiAksi();
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
        Integer idTerpilih = barangDipilih() == null
                ? null : barangDipilih().getId();
        memperbaruiTabel = true;
        try {
            urutanBarang.clear();
            urutanBarang.addAll(konteks.getDaftarBarang());
            model.setRowCount(0);
            Barang aktif = konteks.getBarangAktif();
            for (Barang barang : urutanBarang) {
                model.addRow(new Object[]{
                    barang.getNama(),
                    barang.getSatuan(),
                    barang.hitungSaldo() + " " + barang.getSatuan(),
                    barang.getStokMinimum() + " " + barang.getSatuan()
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
            labelAktif.setText(aktif == null
                    ? "Barang aktif: belum ada."
                    : "Barang aktif: " + aktif.getNama()
                    + " (" + aktif.getSatuan() + ")");
            perbaruiSaringan();
        } finally {
            memperbaruiTabel = false;
        }
        perbaruiAksi();
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
            setHorizontalAlignment(column >= 2
                    ? javax.swing.SwingConstants.RIGHT
                    : javax.swing.SwingConstants.LEFT);
            setVerticalAlignment(javax.swing.SwingConstants.CENTER);
            int modelRow = table.convertRowIndexToModel(row);
            Barang barang = modelRow >= 0 && modelRow < urutanBarang.size()
                    ? urutanBarang.get(modelRow) : null;
            Barang aktif = konteks.getBarangAktif();
            boolean iniAktif = barang != null && aktif != null
                    && barang.getId() == aktif.getId();
            if (iniAktif && !isSelected) {
                setBackground(Ui.AKSEN_LEMBUT);
                setForeground(Ui.KARTU.getRed() < 100
                        ? Ui.TEKS : Ui.AKSEN);
                setFont(Ui.font(Font.BOLD, Ui.FONT_BODY));
                setToolTipText("Barang aktif");
            } else {
                int hover = table instanceof TabelKantin
                        ? ((TabelKantin) table).getBarisHover() : -1;
                setBackground(isSelected
                        ? table.getSelectionBackground()
                        : (row == hover ? Ui.HOVER : Ui.KARTU));
                setForeground(Ui.TEKS);
                setFont(Ui.font(Font.PLAIN, Ui.FONT_BODY));
                setToolTipText(null);
            }
            return this;
        }
    }
}
