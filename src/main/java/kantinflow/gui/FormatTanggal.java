package kantinflow.gui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Utilitas pemformatan tanggal seragam di seluruh antarmuka KantinFlow
 * dengan pola {@code dd MMM yyyy} dan Locale Indonesia.
 */
public final class FormatTanggal {
    public static final Locale LOCALE_INDONESIA = Locale.of("id", "ID");
    public static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy", LOCALE_INDONESIA);

    private FormatTanggal() {
    }

    public static String format(LocalDate tanggal) {
        return tanggal == null ? "" : FORMATTER.format(tanggal);
    }
}
