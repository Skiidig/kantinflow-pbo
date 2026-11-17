package kantinflow.gui;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * Dasar halaman. Setiap halaman wajib dapat menyegarkan diri dari barang aktif.
 */
public abstract class PanelKantin extends JPanel {
    protected final KonteksKantin konteks;

    protected PanelKantin(KonteksKantin konteks, String judul, String keterangan) {
        super(new BorderLayout(0, 16));
        this.konteks = konteks;
        setBackground(Ui.LATAR);
        setBorder(new EmptyBorder(22, 24, 22, 24));
        add(Ui.header(judul, keterangan), BorderLayout.NORTH);
    }

    public abstract void perbarui();
}
