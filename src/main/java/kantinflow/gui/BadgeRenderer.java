package kantinflow.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;

import kantinflow.StatusKedaluwarsa;

/**
 * Renderer pill untuk kolom status dan jenis transaksi pada JTable.
 *
 * <p>Komponen panel dan badge dipakai ulang per nilai agar paint tabel tetap
 * ringan. Pill diratakan ke tengah mengikuti header kolom status dan mendukung
 * sorotan hover baris pada {@link TabelKantin}.</p>
 */
public class BadgeRenderer implements TableCellRenderer {
    private final JPanel panel = new JPanel(new GridBagLayout());
    private final GridBagConstraints batasan = new GridBagConstraints();
    private final Map<String, Badge> cache = new HashMap<>();
    private final Border batas = new EmptyBorder(
            0, Ui.skala(Ui.PADDING_SEL_TABEL),
            0, Ui.skala(Ui.PADDING_SEL_TABEL));
    private Badge terpasang;

    public BadgeRenderer() {
        panel.setOpaque(true);
        batasan.anchor = GridBagConstraints.CENTER;
        batasan.fill = GridBagConstraints.NONE;
        batasan.weightx = 0;
        batasan.weighty = 0;
        batasan.insets = new Insets(0, 0, 0, 0);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        StatusKedaluwarsa statusEnum = value instanceof StatusKedaluwarsa s
                ? s : null;
        String teks = statusEnum != null
                ? statusEnum.getLabel()
                : (value == null ? "" : value.toString());
        boolean aktif = table.isEnabled();
        String kunci = (aktif ? "aktif:" : "nonaktif:")
                + (statusEnum != null ? "enum:" + statusEnum.name() : teks);
        Badge badge = cache.computeIfAbsent(kunci, ignored -> {
            if (!aktif) {
                return new Badge(teks, Ui.TEKS_LEMBUT, Ui.NETRAL_LEMBUT);
            }
            return statusEnum != null ? Badge.status(statusEnum) : Badge.status(teks);
        });

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
        if (terpasang != badge) {
            panel.removeAll();
            panel.add(badge, batasan);
            terpasang = badge;
        }
        return panel;
    }
}
