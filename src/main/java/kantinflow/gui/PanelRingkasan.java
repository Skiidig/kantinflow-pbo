package kantinflow.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
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
    private final JTextField fieldTanggal = Ui.field(10);
    private final JLabel labelFisik = new JLabel("-");
    private final JLabel labelLayak = new JLabel("-");
    private final JLabel labelMinimum = new JLabel("-");
    private final JLabel labelStatus = new JLabel("Belum diperiksa");
    private final JButton tombolPeriksa = Ui.utama("Periksa");

    public PanelRingkasan(KonteksKantin konteks) {
        super(konteks, "Ringkasan",
                "Kondisi stok barang aktif dan kebutuhan restok.");
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

        JPanel kontrol = Ui.kartu();
        JPanel baris = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        baris.setOpaque(false);
        baris.add(Ui.labelField("Tanggal pengecekan (yyyy-MM-dd):"));
        baris.add(fieldTanggal);
        baris.add(tombolPeriksa);
        kontrol.add(baris, BorderLayout.WEST);

        JPanel nilai = new JPanel(new java.awt.GridLayout(2, 2, 12, 12));
        nilai.setOpaque(false);
        nilai.add(Ui.kartuNilai("Stok fisik", labelFisik,
                Ui.keterangan("Seluruh sisa batch")));
        nilai.add(Ui.kartuNilai("Stok layak", labelLayak,
                Ui.keterangan("Aturan FEFO pada tanggal pengecekan")));
        nilai.add(Ui.kartuNilai("Stok minimum", labelMinimum,
                Ui.keterangan("Batas kebutuhan barang aktif")));
        nilai.add(Ui.kartuNilai("Status", labelStatus,
                Ui.keterangan("Perlu restok jika layak <= minimum")));

        JLabel catatan = Ui.keterangan(
                "Stok layak menghitung batch bersisa yang belum kedaluwarsa, "
                + "termasuk batch yang segera kedaluwarsa.");
        catatan.setBorder(new javax.swing.border.EmptyBorder(2, 2, 2, 2));

        isi.add(kontrol, BorderLayout.NORTH);
        isi.add(nilai, BorderLayout.CENTER);
        isi.add(catatan, BorderLayout.SOUTH);
        add(isi, BorderLayout.CENTER);
    }

    private void periksa() {
        try {
            LocalDate tanggal = LocalDate.parse(fieldTanggal.getText().trim());
            konteks.setTanggalPemantauan(tanggal);
            konteks.tampilkanPesan("Ringkasan diperiksa untuk tanggal "
                    + tanggal + ".", true);
        } catch (DateTimeParseException exception) {
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
        labelStatus.setFont(Ui.font(java.awt.Font.BOLD, 20f));
    }
}
