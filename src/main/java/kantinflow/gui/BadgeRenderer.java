package kantinflow.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;

/**
 * Renderer pill untuk kolom status/jenis.
 */
public class BadgeRenderer implements TableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setOpaque(true);
        int hover = table instanceof TabelKantin
                ? ((TabelKantin) table).getBarisHover() : -1;
        Color latar = isSelected ? table.getSelectionBackground()
                : (row == hover ? Ui.HOVER : Ui.KARTU);
        panel.setBackground(latar);
        panel.setBorder(new EmptyBorder(0, Ui.skala(8), 0, Ui.skala(8)));
        panel.add(Badge.status(value == null ? "" : value.toString()));
        return panel;
    }
}
