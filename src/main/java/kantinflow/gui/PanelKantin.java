package kantinflow.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.border.EmptyBorder;

/**
 * Dasar halaman. Setiap halaman wajib dapat menyegarkan diri dari barang aktif.
 */
public abstract class PanelKantin extends JPanel implements Scrollable {
    protected final KonteksKantin konteks;
    private final String judul;
    private final String keterangan;

    protected PanelKantin(KonteksKantin konteks, String judul, String keterangan) {
        super(new BorderLayout(0, 16));
        this.konteks = konteks;
        this.judul = judul;
        this.keterangan = keterangan;
        setBackground(Ui.LATAR);
        setBorder(new EmptyBorder(Ui.PADDING_HALAMAN));
    }

    public String getJudul() {
        return judul;
    }

    public String getKeterangan() {
        return keterangan;
    }

    /**
     * Lebar maksimum area konten halaman dalam satuan FlatLaf. Halaman
     * dashboard dapat memakai area yang sedikit lebih lebar.
     */
    public int lebarMaksimum() {
        return Ui.LEBAR_KONTEN;
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect,
            int orientation, int direction) {
        return 16;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect,
            int orientation, int direction) {
        return 96;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    /**
     * True bila ada isian formulir yang belum disimpan. Dipakai shell untuk
     * meminta konfirmasi sebelum mengganti barang aktif.
     */
    public boolean adaIsianBelumDisimpan() {
        return false;
    }

    public abstract void perbarui();
}
