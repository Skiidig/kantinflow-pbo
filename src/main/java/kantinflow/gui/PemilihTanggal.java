package kantinflow.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import com.formdev.flatlaf.ui.FlatLineBorder;
import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;
import com.github.lgooddatepicker.components.DatePickerSettings.DateArea;

/**
 * Pemilih tanggal bersama dengan locale Indonesia dan format tampilan
 * {@code dd MMM yyyy}. Nilai yang dipakai aplikasi tetap {@link LocalDate}.
 */
final class PemilihTanggal extends DatePicker {
    private boolean teksTidakValid;
    private boolean pertahankanTeksTidakValid;

    PemilihTanggal() {
        super(buatPengaturan());
        setOpaque(true);
        setBackground(Ui.KARTU);
        aturGaris(false);

        JTextField teks = getComponentDateTextField();
        teks.setMargin(new Insets(0, 0, 0, 0));
        teks.setBackground(Ui.KARTU);
        teks.setForeground(Ui.TEKS);
        teks.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent event) {
                aturGaris(true);
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent event) {
                aturGaris(false);
            }
        });

        javax.swing.JButton tombol = getComponentToggleCalendarButton();
        tombol.setText("\u25BE");
        tombol.setFont(Ui.font(Font.PLAIN, Ui.FONT_KECIL));
        tombol.setForeground(Ui.TEKS_LEMBUT);
        tombol.setBackground(Ui.KARTU);
        tombol.setContentAreaFilled(false);
        tombol.setBorder(new EmptyBorder(0, Ui.skala(4), 0, Ui.skala(8)));
        tombol.setCursor(java.awt.Cursor.getPredefinedCursor(
                java.awt.Cursor.HAND_CURSOR));
        tombol.setFocusable(false);
        addDateChangeListener(event -> teksTidakValid = false);
        setTanggalMinimum(null);
    }

    /**
     * Menandai teks manual yang tidak dapat diparse sebelum komponen
     * mengembalikannya ke tanggal valid terakhir.
     */
    @Override
    public void setTextFieldToValidStateIfNeeded() {
        String teks = getText();
        if (teks != null && !teks.trim().isEmpty() && !isTextValid(teks)) {
            teksTidakValid = true;
            if (pertahankanTeksTidakValid) {
                return;
            }
        }
        super.setTextFieldToValidStateIfNeeded();
    }

    void setPertahankanTeksTidakValid(boolean aktif) {
        pertahankanTeksTidakValid = aktif;
    }

    boolean ambilTeksTidakValid() {
        boolean hasil = teksTidakValid && !isTextFieldValid();
        teksTidakValid = false;
        return hasil;
    }

    private static DatePickerSettings buatPengaturan() {
        DatePickerSettings pengaturan =
                new DatePickerSettings(FormatTanggal.LOCALE_INDONESIA);
        pengaturan.setFormatForDatesCommonEra(FormatTanggal.FORMATTER);
        pengaturan.setAllowEmptyDates(true);
        pengaturan.setFontValidDate(Ui.font(Font.PLAIN, Ui.FONT_BODY));
        pengaturan.setFontInvalidDate(Ui.font(Font.PLAIN, Ui.FONT_BODY));
        pengaturan.setFontVetoedDate(Ui.font(Font.PLAIN, Ui.FONT_BODY));
        pengaturan.setFontCalendarDateLabels(
                Ui.font(Font.PLAIN, Ui.FONT_CATATAN));
        pengaturan.setFontCalendarWeekdayLabels(
                Ui.font(Font.BOLD, Ui.FONT_KECIL));
        pengaturan.setFontMonthAndYearMenuLabels(
                Ui.font(Font.BOLD, Ui.FONT_CATATAN));
        pengaturan.setFontTodayLabel(Ui.font(Font.PLAIN, Ui.FONT_CATATAN));
        pengaturan.setFontClearLabel(Ui.font(Font.PLAIN, Ui.FONT_CATATAN));
        pengaturan.setColor(DateArea.TextFieldBackgroundValidDate, Ui.KARTU);
        pengaturan.setColor(DateArea.DatePickerTextValidDate, Ui.TEKS);
        pengaturan.setColor(DateArea.TextFieldBackgroundInvalidDate, Ui.KARTU);
        pengaturan.setColor(DateArea.DatePickerTextInvalidDate, Ui.MERAH);
        pengaturan.setColor(DateArea.TextFieldBackgroundVetoedDate, Ui.KARTU);
        pengaturan.setColor(DateArea.DatePickerTextVetoedDate, Ui.TEKS_LEMBUT);
        pengaturan.getFormatsForParsing().add(DateTimeFormatter.ISO_LOCAL_DATE);
        return pengaturan;
    }

    private void aturGaris(boolean fokus) {
        Color warna = fokus ? Ui.AKSEN : Ui.GARIS;
        setBorder(new FlatLineBorder(
                new Insets(Ui.skala(1), Ui.skala(1),
                        Ui.skala(1), Ui.skala(1)),
                warna, fokus ? 1.25f : 1f, Ui.RADIUS_KONTROL));
    }

    static String format(LocalDate tanggal) {
        return FormatTanggal.format(tanggal);
    }

    LocalDate getTanggal() {
        return getDate();
    }

    void setTanggal(LocalDate tanggal) {
        setDate(tanggal);
    }

    /**
     * Membatasi tanggal yang boleh dipilih melalui kalender dan input manual.
     * Nilai {@code null} berarti tanpa batas minimum.
     */
    void setTanggalMinimum(LocalDate minimum) {
        getSettings().setVetoPolicy(minimum == null
                ? null : tanggal -> !tanggal.isBefore(minimum));
    }

    void tambahPerubahan(Runnable aksi) {
        addDateChangeListener(event -> aksi.run());
    }

    JTextField komponenTeks() {
        return getComponentDateTextField();
    }

    /**
     * Mengganti garis bawaan komponen dengan padding saja, agar bingkai
     * membulat pada panel pembungkus yang terlihat.
     */
    @Override
    public void zDrawTextFieldIndicators() {
        super.zDrawTextFieldIndicators();
        getComponentDateTextField().setBorder(new EmptyBorder(
                Ui.skala(1), Ui.skala(8), Ui.skala(1), Ui.skala(6)));
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension ukuran = super.getPreferredSize();
        return new Dimension(ukuran.width, Ui.tinggiKontrol());
    }

    @Override
    public Dimension getMinimumSize() {
        Dimension ukuran = super.getMinimumSize();
        return new Dimension(ukuran.width, Ui.tinggiKontrol());
    }
}
