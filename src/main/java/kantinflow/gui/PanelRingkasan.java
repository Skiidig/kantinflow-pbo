package kantinflow.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Ringkasan stok aktif dan status restok pada tanggal yang dipilih.
 */
public class PanelRingkasan extends PanelKantin {
    private final JTextField fieldTanggal = Ui.field(11);
    private final JLabel labelFisik = new JLabel("-");
    private final JLabel labelLayak = new JLabel("-");
    private final JLabel labelMinimum = new JLabel("-");
    private final JLabel labelStatus = new JLabel("Belum diperiksa");
    private final JLabel pesanValidasi = Ui.pesan();
    private final JButton tombolPeriksa = Ui.sekunder("Periksa");

    public PanelRingkasan(KonteksKantin konteks) {
        super(konteks, "Ringkasan",
                "Kondisi stok dan kebutuhan restok barang aktif.");
        fieldTanggal.setName("tanggalRingkasan");
        tombolPeriksa.setName("periksaRingkasan");
        labelFisik.setName("ringkasanFisik");
        labelLayak.setName("ringkasanLayak");
        labelMinimum.setName("ringkasanMinimum");
        labelStatus.setName("ringkasanStatus");
        tombolPeriksa.addActionListener(event -> periksa());
        bangunIsi();
    }

    private void bangunIsi() {
        JPanel isi = new JPanel(new BorderLayout(0, 16));
        isi.setOpaque(false);

        JPanel kontrol = Ui.kartu("Tanggal acuan stok layak");
        JPanel baris = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        baris.setOpaque(false);
        fieldTanggal.setColumns(10);
        baris.add(fieldTanggal);
        baris.add(tombolPeriksa);
        JPanel kontrolIsi = new JPanel(new BorderLayout(0, 6));
        kontrolIsi.setOpaque(false);
        kontrolIsi.add(baris, BorderLayout.NORTH);
        JLabel bantuan = Ui.keterangan(
                "Stok layak menghitung batch bersisa yang belum kedaluwarsa pada tanggal ini.");
        bantuan.setBorder(new javax.swing.border.EmptyBorder(2, 0, 0, 0));
        kontrolIsi.add(bantuan, BorderLayout.CENTER);
        kontrolIsi.add(pesanValidasi, BorderLayout.SOUTH);
        kontrol.add(kontrolIsi, BorderLayout.CENTER);

        JPanel nilai = new JPanel(new java.awt.GridLayout(2, 2, 12, 12));
        nilai.setOpaque(false);
        nilai.add(Ui.kartuNilai("Stok fisik", labelFisik,
                Ui.keterangan("Seluruh sisa batch aktif")));
        nilai.add(Ui.kartuNilai("Stok layak", labelLayak,
                Ui.keterangan("Batch layak pada tanggal acuan")));
        nilai.add(Ui.kartuNilai("Stok minimum", labelMinimum,
                Ui.keterangan("Batas kebutuhan barang aktif")));
        nilai.add(Ui.kartuNilai("Status kebutuhan", labelStatus,
                Ui.keterangan("Perlu restok jika stok layak <= minimum")));

        isi.add(kontrol, BorderLayout.NORTH);
        isi.add(nilai, BorderLayout.CENTER);
        add(isi, BorderLayout.CENTER);
    }

    private void periksa() {
        try {
            LocalDate tanggal = LocalDate.parse(fieldTanggal.getText().trim());
            pesanValidasi.setText(" ");
            konteks.setTanggalPemantauan(tanggal);
            konteks.tampilkanPesan("Ringkasan diperiksa untuk tanggal "
                    + tanggal + ".", true);
        } catch (DateTimeParseException exception) {
            pesanValidasi.setText("Format tanggal harus yyyy-MM-dd.");
            fieldTanggal.requestFocusInWindow();
            konteks.tampilkanPesan("Format tanggal tidak valid. Gunakan yyyy-MM-dd.",
                    false);
        }
    }

    @Override
    public void perbarui() {
        LocalDate tanggal = konteks.getTanggalPemantauan();
        if (tanggal == null) {
            tanggal = LocalDate.now();
        }
        fieldTanggal.setText(tanggal.toString());

        int fisik = konteks.getBarangAktif().hitungSaldo();
        int layak = konteks.getBarangAktif().hitungStokLayak(tanggal);
        int minimum = konteks.getBarangAktif().getStokMinimum();
        String satuan = konteks.getBarangAktif().getSatuan();

        labelFisik.setText(fisik + " " + satuan);
        labelLayak.setText(layak + " " + satuan);
        labelMinimum.setText(minimum + " " + satuan);

        boolean perluRestok = layak <= minimum;
        labelStatus.setText(perluRestok ? "Perlu restok" : "Cukup");
        labelStatus.setForeground(perluRestok ? Ui.MERAH : Ui.HIJAU);
        labelStatus.setFont(Ui.font(Font.BOLD, Ui.FONT_NILAI));
    }
}
