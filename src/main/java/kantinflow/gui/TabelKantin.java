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
    private boolean horizontalSaatSempit;

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

    /**
     * Saat aktif, tabel tetap mengisi lebar viewport selama semua kolom muat
     * pada lebar minimumnya. Jika tidak muat, tabel memakai lebar pilihan
     * sendiri agar scrollbar horizontal muncul, bukan memotong kolom.
     */
    public void setHorizontalSaatSempit(boolean aktif) {
        horizontalSaatSempit = aktif;
        revalidate();
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        if (!horizontalSaatSempit) {
            return super.getScrollableTracksViewportWidth();
        }
        int lebar = getParent() == null ? 0 : getParent().getWidth();
        if (lebar <= 0) {
            return true;
        }
        int minimum = 0;
        for (int i = 0; i < getColumnModel().getColumnCount(); i++) {
            minimum += getColumnModel().getColumn(i).getMinWidth();
        }
        return minimum == 0 || lebar >= minimum;
    }
}
