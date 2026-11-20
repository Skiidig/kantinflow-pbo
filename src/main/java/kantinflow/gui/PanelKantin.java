package kantinflow.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.border.EmptyBorder;

/**
 * Dasar halaman. Setiap halaman wajib dapat menyegarkan diri dari konteks
 * datanya sendiri.
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
        setBorder(new EmptyBorder(
                Ui.skala(Ui.PADDING_HALAMAN.top),
                Ui.skala(Ui.PADDING_HALAMAN.left),
                Ui.skala(Ui.PADDING_HALAMAN.bottom),
                Ui.skala(Ui.PADDING_HALAMAN.right)));
    }

    public String getJudul() {
        return judul;
    }

    public String getKeterangan() {
        return keterangan;
    }

    /**
     * Aksi utama opsional yang ditampilkan rata kanan pada blok header
     * halaman, sejajar vertikal dengan judul dan subjudul.
     */
    public JComponent getAksiHeader() {
        return null;
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
     * True bila ada isian formulir yang belum disimpan. Dipakai halaman untuk
     * meminta konfirmasi sebelum mengganti barang pada halaman itu.
     */
    public boolean adaIsianBelumDisimpan() {
        return false;
    }

    public abstract void perbarui();
}
