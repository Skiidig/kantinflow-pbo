package kantinflow.gui;

import java.awt.Font;

import javax.swing.UIManager;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.fonts.inter.FlatInterFont;

/**
 * Inisialisasi tema terang KantinFlow.
 */
public final class Tema {
    private static boolean siap;

    private Tema() {
    }

    public static void pasang() {
        if (siap) {
            return;
        }
        pasangLaf();
        siap = true;
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
        FlatLightLaf.setup();
        UIManager.put("defaultFont",
                new Font(fontFamily(), Font.PLAIN, 13));
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
        UIManager.put("Component.arc", Ui.RADIUS_KONTROL);
        UIManager.put("Button.arc", Ui.RADIUS_KONTROL);
        UIManager.put("TextComponent.arc", Ui.RADIUS_KONTROL);
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
