package kantinflow.gui;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import javax.swing.UIManager;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.fonts.inter.FlatInterFont;

/**
 * Tema terang/gelap KantinFlow dan preferensi tampilan pengguna.
 */
public final class Tema {
    private static final String KUNCI_GELAP = "temaGelap";
    private static final Path FILE = Path.of("data", "kantinflow-tema.properties");
    private static boolean siap;
    private static boolean gelap;

    private Tema() {
    }

    public static void pasang() {
        if (siap) {
            return;
        }
        gelap = bacaPreferensi();
        pasangLaf();
        siap = true;
    }

    public static boolean gelap() {
        return gelap;
    }

    public static void terapkan(boolean modeGelap) {
        gelap = modeGelap;
        pasangLaf();
        simpanPreferensi();
        siap = true;
    }

    public static void simpanPreferensi() {
        Properties properti = new Properties();
        properti.setProperty(KUNCI_GELAP, Boolean.toString(gelap));
        try {
            Path induk = FILE.toAbsolutePath().getParent();
            if (induk != null) {
                Files.createDirectories(induk);
            }
            try (OutputStream output = Files.newOutputStream(FILE)) {
                properti.store(output, "KantinFlow theme preference");
            }
        } catch (IOException | RuntimeException exception) {
            // Preferensi tidak boleh menggagalkan aplikasi.
        }
    }

    private static boolean bacaPreferensi() {
        if (!Files.isRegularFile(FILE)) {
            return false;
        }
        Properties properti = new Properties();
        try (InputStream input = Files.newInputStream(FILE)) {
            properti.load(input);
            return Boolean.parseBoolean(properti.getProperty(
                    KUNCI_GELAP, "false"));
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    private static void pasangLaf() {
        FlatLaf.registerCustomDefaultsSource("kantinflow");
        try {
            FlatInterFont.install();
        } catch (RuntimeException | LinkageError exception) {
            // Fallback ke font UI default bila paket font tidak tersedia.
        }
        UIManager.put("defaultFont",
                new Font(fontFamily(), Font.PLAIN, 13));
        if (gelap) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
        UIManager.put("defaultFont",
                new Font(fontFamily(), Font.PLAIN, 13));
        Ui.pakaiTema(gelap);
        UIManager.put("Panel.background", Ui.LATAR);
        UIManager.put("Viewport.background", Ui.LATAR);
        UIManager.put("ScrollPane.background", Ui.LATAR);
        UIManager.put("Label.foreground", Ui.TEKS);
        UIManager.put("Table.background", Ui.KARTU);
        UIManager.put("Table.foreground", Ui.TEKS);
        UIManager.put("Table.selectionBackground", Ui.AKSEN_LEMBUT);
        UIManager.put("Table.selectionForeground", Ui.TEKS);
        UIManager.put("TableHeader.background", Ui.LATAR);
        UIManager.put("TableHeader.foreground", Ui.TEKS_LEMBUT);
        UIManager.put("TextField.background", Ui.KARTU);
        UIManager.put("TextField.foreground", Ui.TEKS);
        UIManager.put("ComboBox.background", Ui.KARTU);
        UIManager.put("ComboBox.foreground", Ui.TEKS);
        UIManager.put("Component.arc", 10);
        UIManager.put("Button.arc", 10);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("ScrollBar.width", 8);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.rowHeight", 40);
        UIManager.put("TableHeader.height", 44);
        UIManager.put("Component.focusColor", Ui.AKSEN);
        UIManager.put("ToolTip.background", Ui.TEKS);
        UIManager.put("ToolTip.foreground", Ui.KARTU);
        UIManager.put("ToolTip.border",
                javax.swing.BorderFactory.createEmptyBorder(6, 10, 6, 10));
    }

    private static String fontFamily() {
        try {
            return FlatInterFont.FAMILY;
        } catch (RuntimeException | LinkageError exception) {
            return "Segoe UI";
        }
    }
}
