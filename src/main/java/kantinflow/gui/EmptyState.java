package kantinflow.gui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

/**
 * Empty state rapi dengan ikon besar, judul, deskripsi, dan tombol aksi
 * opsional saat tabel atau hasil pencarian kosong.
 */
public class EmptyState extends JPanel {
    private static final long serialVersionUID = 1L;
    private final LingkaranIkon lingkaran;
    private final JLabel judul = new JLabel();
    private final JLabel deskripsi = new JLabel();
    private final JPanel aksi = new JPanel();

    public EmptyState(String ikonNama, String judul, String deskripsi) {
        super(new GridBagLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(
                Ui.skala(22), Ui.skala(20), Ui.skala(22), Ui.skala(20)));
        setMinimumSize(new Dimension(Ui.skala(200), Ui.skala(128)));
        setPreferredSize(new Dimension(Ui.skala(420), Ui.skala(146)));
        JPanel isi = new JPanel();
        isi.setOpaque(false);
        isi.setLayout(new BoxLayout(isi, BoxLayout.Y_AXIS));
        lingkaran = new LingkaranIkon(
                Ui.ikonAksi(ikonNama, 26, Ui.TEKS_LEMBUT));
        lingkaran.setAlignmentX(CENTER_ALIGNMENT);
        this.judul.setText(judul);
        this.judul.setFont(Ui.font(Font.BOLD, Ui.FONT_KARTU));
        this.judul.setForeground(Ui.TEKS);
        this.judul.setHorizontalAlignment(SwingConstants.CENTER);
        this.judul.setAlignmentX(CENTER_ALIGNMENT);
        this.deskripsi.setText(deskripsi);
        this.deskripsi.setFont(Ui.font(Font.PLAIN, Ui.FONT_CATATAN));
        this.deskripsi.setForeground(Ui.TEKS_LEMBUT);
        this.deskripsi.setHorizontalAlignment(SwingConstants.CENTER);
        this.deskripsi.setAlignmentX(CENTER_ALIGNMENT);
        this.aksi.setOpaque(false);
        this.aksi.setLayout(new BoxLayout(aksi, BoxLayout.X_AXIS));
        this.aksi.setAlignmentX(CENTER_ALIGNMENT);
        isi.add(lingkaran);
        isi.add(Box.createVerticalStrut(Ui.skala(10)));
        isi.add(this.judul);
        isi.add(Box.createVerticalStrut(Ui.skala(4)));
        isi.add(this.deskripsi);
        isi.add(Box.createVerticalStrut(Ui.skala(10)));
        isi.add(this.aksi);
        add(isi);
    }

    public EmptyState setIcon(String ikonNama) {
        lingkaran.aturIkon(Ui.ikonAksi(ikonNama, 26, Ui.TEKS_LEMBUT));
        return this;
    }

    public EmptyState setAction(JButton tombol) {
        aksi.removeAll();
        if (tombol != null) {
            tombol.setAlignmentX(CENTER_ALIGNMENT);
            aksi.add(tombol);
        }
        aksi.setVisible(tombol != null);
        aksi.revalidate();
        aksi.repaint();
        return this;
    }

    public EmptyState setDescription(String teks) {
        deskripsi.setText(teks);
        return this;
    }

    public String getText() {
        return judul.getText() + " " + deskripsi.getText();
    }

    public EmptyState setTitle(String teks) {
        judul.setText(teks);
        return this;
    }

    private static final class LingkaranIkon extends JPanel {
        private static final long serialVersionUID = 1L;
        private transient Icon ikon;

        private LingkaranIkon(Icon ikon) {
            this.ikon = ikon;
            setOpaque(false);
            setPreferredSize(new Dimension(Ui.skala(60), Ui.skala(60)));
            setMinimumSize(getPreferredSize());
            setMaximumSize(getPreferredSize());
        }

        private void aturIkon(Icon baru) {
            this.ikon = baru;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Ui.NETRAL_LEMBUT);
                g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
            } finally {
                g2.dispose();
            }
            if (ikon != null) {
                int x = (getWidth() - ikon.getIconWidth()) / 2;
                int y = (getHeight() - ikon.getIconHeight()) / 2;
                ikon.paintIcon(this, graphics, x, y);
            }
            super.paintComponent(graphics);
        }
    }
}
