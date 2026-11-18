package kantinflow.gui;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.JTable;
import javax.swing.table.TableModel;

/**
 * JTable dengan sorotan baris saat kursor berada di atas tabel.
 */
public class TabelKantin extends JTable {
    private int barisHover = -1;

    public TabelKantin(TableModel model) {
        super(model);
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                int baris = rowAtPoint(event.getPoint());
                if (baris != barisHover) {
                    barisHover = baris;
                    repaint();
                }
            }
        });
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent event) {
                if (barisHover != -1) {
                    barisHover = -1;
                    repaint();
                }
            }
        });
    }

    public int getBarisHover() {
        return barisHover;
    }
}
