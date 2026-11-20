package kantinflow.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.fonts.inter.FlatInterFont;

/**
 * Inisialisasi tema Modern Minimalist (Zinc/Slate) untuk KantinFlow berbasis
 * FlatLaf sebelum jendela utama dibuat.
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
            // Fallback ke font UI sistem bila paket font tidak tersedia.
        }
        String keluargaFont = fontFamily();
        UIManager.put("defaultFont", new Font(keluargaFont, Font.PLAIN, 13));
        FlatLightLaf.setup();
        UIManager.put("defaultFont", new Font(keluargaFont, Font.PLAIN, 13));

        // Permukaan & teks Zinc/Slate
        UIManager.put("Panel.background", Ui.LATAR);
        UIManager.put("Viewport.background", Ui.LATAR);
        UIManager.put("ScrollPane.background", Ui.LATAR);
        UIManager.put("Label.foreground", Ui.TEKS);

        // Radius & focus ring halus seragam
        UIManager.put("Component.arc", Ui.RADIUS_KONTROL);
        UIManager.put("Button.arc", Ui.RADIUS_KONTROL);
        UIManager.put("TextComponent.arc", Ui.RADIUS_KONTROL);
        UIManager.put("CheckBox.arc", 6);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.innerFocusWidth", 0);
        UIManager.put("Component.focusColor", Ui.FOKUS_HALUS);
        UIManager.put("Component.borderColor", Ui.GARIS);
        UIManager.put("Component.focusedBorderColor", Ui.AKSEN);
        UIManager.put("Component.hoverBorderColor", Ui.GARIS_HOVER);

        // Input & ComboBox
        UIManager.put("TextField.background", Ui.KARTU);
        UIManager.put("TextField.foreground", Ui.TEKS);
        UIManager.put("TextField.placeholderForeground", Ui.PLACEHOLDER);
        UIManager.put("ComboBox.background", Ui.KARTU);
        UIManager.put("ComboBox.foreground", Ui.TEKS);
        UIManager.put("ComboBox.buttonBackground", Ui.KARTU);
        UIManager.put("ComboBox.buttonArrowColor", Ui.TEKS_LEMBUT);
        UIManager.put("ComboBox.buttonHoverArrowColor", Ui.TEKS);
        UIManager.put("ComboBox.selectionBackground", Ui.NAV_AKTIF_BG);
        UIManager.put("ComboBox.selectionForeground", Ui.TEKS);

        // Scrollbar tipis minimalis tanpa tombol panah
        UIManager.put("ScrollBar.width", 8);
        UIManager.put("ScrollBar.showButtons", false);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.trackArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("ScrollBar.track", new Color(0, 0, 0, 0));
        UIManager.put("ScrollBar.thumb", new Color(0x18, 0x18, 0x1B, 42));
        UIManager.put("ScrollBar.hoverThumbColor",
                new Color(0x18, 0x18, 0x1B, 88));
        UIManager.put("ScrollBar.pressedThumbColor",
                new Color(0x18, 0x18, 0x1B, 125));

        // Tabel minimalis
        UIManager.put("Table.background", Ui.KARTU);
        UIManager.put("Table.foreground", Ui.TEKS);
        UIManager.put("Table.selectionBackground", Ui.AKSEN_LEMBUT);
        UIManager.put("Table.selectionForeground", Ui.TEKS);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.gridColor", Ui.GARIS_TABEL);
        UIManager.put("Table.rowHeight", Ui.TINGGI_BARIS_TABEL);
        UIManager.put("TableHeader.background", Ui.LATAR);
        UIManager.put("TableHeader.foreground", Ui.TEKS_LEMBUT);
        UIManager.put("TableHeader.height", Ui.TINGGI_HEADER_TABEL);
        UIManager.put("TableHeader.separatorColor", Ui.GARIS);
        UIManager.put("TableHeader.bottomSeparatorColor", Ui.GARIS);

        // TabbedPane bergaya garis bawah minimalis
        UIManager.put("TabbedPane.underlineColor", Ui.AKSEN);
        UIManager.put("TabbedPane.hoverColor", Ui.NETRAL_LEMBUT);
        UIManager.put("TabbedPane.selectedForeground", Ui.TEKS);
        UIManager.put("TabbedPane.foreground", Ui.TEKS_LEMBUT);
        UIManager.put("TabbedPane.contentAreaColor", Ui.GARIS);
        UIManager.put("TabbedPane.tabHeight", 38);

        // Integrasi title bar FlatLaf menyatu dengan canvas
        UIManager.put("TitlePane.unifiedBackground", true);
        UIManager.put("TitlePane.background", Ui.LATAR);
        UIManager.put("TitlePane.inactiveBackground", Ui.LATAR);
        UIManager.put("TitlePane.foreground", Ui.TEKS);
        UIManager.put("TitlePane.inactiveForeground", Ui.TEKS_LEMBUT);
        UIManager.put("TitlePane.borderColor", Ui.GARIS);

        // Tooltip kontras tinggi
        UIManager.put("ToolTip.background", Ui.AKSEN);
        UIManager.put("ToolTip.foreground", Ui.LATAR);
        UIManager.put("ToolTip.border",
                BorderFactory.createEmptyBorder(6, 10, 6, 10));
    }

    private static String fontFamily() {
        try {
            return FlatInterFont.FAMILY;
        } catch (RuntimeException | LinkageError exception) {
            return "Segoe UI";
        }
    }
}
