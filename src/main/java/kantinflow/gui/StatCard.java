package kantinflow.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

/**
 * Kartu statistik ringkas dengan ikon, angka utama, keterangan, dan dukungan
 * aksi klik (shortcut navigasi/filter) dengan umpan balik hover & fokus.
 */
public class StatCard extends CardPanel {
    private static final long serialVersionUID = 1L;
    private static final Color LATAR_HOVER = new Color(0xF8FAFC);

    private final JLabel nilai = new JLabel("-");
    private final JLabel judul;
    private final JLabel keterangan;
    private final JLabel ikon;
    private final JLabel panahPintasan = new JLabel();
    private final JPanel strip;
    private final String ikonNama;
    private Color aksenSaatIni;
    private transient Runnable aksiKlik;
    private boolean hover;
    private boolean aktif;

    public StatCard(String judul, String ikonNama, Color aksen) {
        super(new BorderLayout(0, 7));
        this.ikonNama = ikonNama;
        this.aksenSaatIni = aksen;
        setBorder(new EmptyBorder(Ui.skala(18), Ui.skala(18),
                Ui.skala(18), Ui.skala(18)));
        this.judul = new JLabel(judul);
        this.judul.setFont(Ui.font(Font.BOLD, Ui.FONT_LABEL));
        this.judul.setForeground(Ui.TEKS_LEMBUT);
        this.keterangan = new JLabel(" ");
        this.keterangan.setFont(Ui.font(Font.PLAIN, Ui.FONT_KECIL));
        this.keterangan.setForeground(Ui.TEKS_LEMBUT);
        this.nilai.setFont(Ui.font(Font.BOLD, 28f));
        this.nilai.setForeground(Ui.TEKS);
        this.ikon = new JLabel(Ui.ikonAksi(ikonNama, 18, aksen)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    Color warna = getForeground();
                    g2.setColor(new Color(warna.getRed(), warna.getGreen(),
                            warna.getBlue(), 22));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(),
                            Ui.skala(10), Ui.skala(10));
                } finally {
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        this.ikon.setForeground(aksen);
        this.ikon.setHorizontalAlignment(SwingConstants.CENTER);
        this.ikon.setVerticalAlignment(SwingConstants.CENTER);
        this.ikon.setPreferredSize(new Dimension(Ui.skala(32), Ui.skala(32)));
        this.panahPintasan.setIcon(
                Ui.ikonAksi("arrow-up-right", 14, Ui.TEKS_LEMBUT));
        this.panahPintasan.setVisible(false);
        this.strip = new JPanel();
        this.strip.setOpaque(false);

        JPanel atas = new JPanel(new BorderLayout(8, 0));
        atas.setOpaque(false);
        atas.add(this.judul, BorderLayout.CENTER);
        atas.add(this.ikon, BorderLayout.EAST);

        JPanel barisKeterangan = new JPanel(new BorderLayout(6, 0));
        barisKeterangan.setOpaque(false);
        barisKeterangan.add(this.keterangan, BorderLayout.CENTER);
        barisKeterangan.add(this.panahPintasan, BorderLayout.EAST);

        JPanel bawah = new JPanel(new BorderLayout(0, 4));
        bawah.setOpaque(false);
        bawah.add(barisKeterangan, BorderLayout.NORTH);
        bawah.add(this.strip, BorderLayout.SOUTH);
        add(atas, BorderLayout.NORTH);
        add(this.nilai, BorderLayout.CENTER);
        add(bawah, BorderLayout.SOUTH);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                if (aksiKlik != null) {
                    hover = true;
                    perbaruiPanah();
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent event) {
                if (hover) {
                    hover = false;
                    perbaruiPanah();
                    repaint();
                }
            }

            @Override
            public void mouseClicked(MouseEvent event) {
                if (aksiKlik != null && event.getButton() == MouseEvent.BUTTON1) {
                    aksiKlik.run();
                }
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                if (aksiKlik != null
                        && (event.getKeyCode() == KeyEvent.VK_ENTER
                                || event.getKeyCode() == KeyEvent.VK_SPACE)) {
                    event.consume();
                    aksiKlik.run();
                }
            }
        });
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                repaint();
            }

            @Override
            public void focusLost(FocusEvent event) {
                repaint();
            }
        });
    }

    public void setAksiKlik(Runnable aksi, String petunjuk) {
        this.aksiKlik = aksi;
        boolean bisaKlik = aksi != null;
        setFocusable(bisaKlik);
        setCursor(bisaKlik
                ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                : Cursor.getDefaultCursor());
        setToolTipText(petunjuk);
        panahPintasan.setVisible(bisaKlik);
        perbaruiPanah();
        repaint();
    }

    public void setAktif(boolean aktif) {
        if (this.aktif == aktif) {
            return;
        }
        this.aktif = aktif;
        perbaruiPanah();
        repaint();
    }

    public boolean isAktif() {
        return aktif;
    }

    private void perbaruiPanah() {
        if (!panahPintasan.isVisible()) {
            return;
        }
        Color warna = (hover || aktif)
                ? (aksenSaatIni.equals(Ui.TEKS_LEMBUT) ? Ui.AKSEN : aksenSaatIni)
                : Ui.TEKS_LEMBUT;
        panahPintasan.setIcon(Ui.ikonAksi("arrow-up-right", 14, warna));
    }

    @Override
    protected Color warnaLatarKartu() {
        if (aktif) {
            Color dasar = aksenSaatIni.equals(Ui.TEKS_LEMBUT)
                    ? Ui.AKSEN : aksenSaatIni;
            return new Color(dasar.getRed(), dasar.getGreen(),
                    dasar.getBlue(), 14);
        }
        return hover && aksiKlik != null ? LATAR_HOVER : Ui.KARTU;
    }

    @Override
    protected Color warnaGarisKartu() {
        Color fokusWarna = aksenSaatIni.equals(Ui.TEKS_LEMBUT)
                ? Ui.AKSEN : aksenSaatIni;
        if (aktif || isFocusOwner()) {
            return fokusWarna;
        }
        if (hover && aksiKlik != null) {
            return new Color(fokusWarna.getRed(), fokusWarna.getGreen(),
                    fokusWarna.getBlue(), 140);
        }
        return Ui.GARIS;
    }

    @Override
    protected float tebalGarisKartu() {
        return (aktif || isFocusOwner()) ? 1.8f : 1f;
    }

    public JLabel getNilai() {
        return nilai;
    }

    public JLabel getKeterangan() {
        return keterangan;
    }

    public void setNilai(String teks) {
        nilai.setText(teks);
    }

    public void setKeterangan(String teks) {
        keterangan.setText(teks == null || teks.isBlank() ? " " : teks);
    }

    public void setStatus(String teks) {
        strip.removeAll();
        if (teks == null || teks.isBlank()) {
            strip.setPreferredSize(new Dimension(0, 0));
            return;
        }
        Badge badge = Badge.status(teks);
        strip.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        strip.add(badge);
        strip.setPreferredSize(null);
    }

    public void setAksen(Color warna) {
        this.aksenSaatIni = warna;
        ikon.setIcon(Ui.ikonAksi(ikonNama, 18, warna));
        ikon.setForeground(warna);
        nilai.setForeground(warna.equals(Ui.TEKS_LEMBUT) ? Ui.TEKS : warna);
        perbaruiPanah();
        ikon.repaint();
        repaint();
    }
}
