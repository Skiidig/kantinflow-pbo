package kantinflow.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;

/**
 * Renderer pill untuk kolom status/jenis.
 *
 * <p>Komponen panel dan badge dipakai ulang per nilai agar paint tabel tidak
 * membuat objek baru pada setiap sel. GridBagLayout menempatkan pill tepat di
 * tengah tinggi baris, termasuk saat tabel memakai font scaling.</p>
 */
public class BadgeRenderer implements TableCellRenderer {
    private final JPanel panel = new JPanel(new GridBagLayout());
    private final GridBagConstraints batasan = new GridBagConstraints();
    private final Map<String, Badge> cache = new HashMap<>();
    private final javax.swing.border.Border batas =
            new EmptyBorder(0, Ui.skala(8), 0, Ui.skala(8));

    public BadgeRenderer() {
        panel.setOpaque(true);
        batasan.anchor = GridBagConstraints.CENTER;
        batasan.fill = GridBagConstraints.NONE;
        batasan.weightx = 0;
        batasan.weighty = 0;
        batasan.insets = new java.awt.Insets(0, 0, 0, 0);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        String teks = value == null ? "" : value.toString();
        boolean aktif = table.isEnabled();
        String kunci = (aktif ? "aktif:" : "nonaktif:") + teks;
        Badge badge = cache.computeIfAbsent(kunci, ignored -> aktif
                ? Badge.status(teks)
                : new Badge(teks, Ui.TEKS_LEMBUT, Ui.NETRAL_LEMBUT));

        int hover = table instanceof TabelKantin
                ? ((TabelKantin) table).getBarisHover() : -1;
        Color latar;
        if (!aktif) {
            latar = Ui.NETRAL_LEMBUT;
        } else if (isSelected) {
            latar = table.getSelectionBackground();
        } else {
            latar = row == hover ? Ui.HOVER : Ui.KARTU;
        }
        panel.setBackground(latar);
        panel.setBorder(batas);
        panel.removeAll();
        panel.add(badge, batasan);
        return panel;
    }
}
