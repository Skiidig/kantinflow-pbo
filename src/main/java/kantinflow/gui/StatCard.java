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
import javax.swing.border.EmptyBorder;

/**
 * Kartu KPI minimalis (Linear/Vercel style): fokus pada label kecil di atas,
 * angka besar 28px Bold, indikator titik warna halus, dan border tegas saat
 * hover (#A1A1AA).
 */
public class StatCard extends CardPanel {
    private static final long serialVersionUID = 1L;

    private final JLabel nilai = new JLabel("-");
    private final JLabel judul;
    private final JLabel keterangan;
    private final TitikIndikator indikator;
    private final JLabel panahPintasan = new JLabel();
    private final JPanel strip;
    private Color aksenSaatIni;
    private transient Runnable aksiKlik;
    private boolean hover;
    private boolean aktif;

    public StatCard(String judul, String ikonNama, Color aksen) {
        super(new BorderLayout(0, Ui.skala(10)));
        this.aksenSaatIni = aksen;
        setBorder(new EmptyBorder(
                Ui.skala(Ui.PADDING_KARTU.top),
                Ui.skala(Ui.PADDING_KARTU.left),
                Ui.skala(Ui.PADDING_KARTU.bottom),
                Ui.skala(Ui.PADDING_KARTU.right)));

        this.judul = new JLabel(judul);
        this.judul.setFont(Ui.fontSemiBold(Ui.FONT_LABEL));
        this.judul.setForeground(Ui.TEKS_LEMBUT);

        this.indikator = new TitikIndikator(aksen);

        this.nilai.setFont(Ui.font(Font.BOLD, Ui.FONT_NILAI));
        this.nilai.setForeground(Ui.TEKS);

        this.keterangan = new JLabel(" ");
        this.keterangan.setFont(Ui.font(Font.PLAIN, Ui.FONT_LABEL));
        this.keterangan.setForeground(Ui.TEKS_LEMBUT);

        this.panahPintasan.setIcon(
                Ui.ikonAksi("arrow-up-right", 13, Ui.TEKS_LEMBUT));
        this.panahPintasan.setVisible(false);

        this.strip = new JPanel();
        this.strip.setOpaque(false);

        JPanel atas = new JPanel(new BorderLayout(8, 0));
        atas.setOpaque(false);
        atas.add(this.judul, BorderLayout.CENTER);
        atas.add(this.indikator, BorderLayout.EAST);

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
        Color warna = (hover || aktif) ? Ui.TEKS : Ui.PLACEHOLDER;
        panahPintasan.setIcon(Ui.ikonAksi("arrow-up-right", 13, warna));
    }

    @Override
    protected Color warnaLatarKartu() {
        return aktif ? Ui.LATAR : Ui.KARTU;
    }

    @Override
    protected Color warnaGarisKartu() {
        if (aktif) {
            return Ui.AKSEN;
        }
        if (isFocusOwner()) {
            return Ui.AKSEN;
        }
        if (hover && aksiKlik != null) {
            return Ui.GARIS_HOVER;
        }
        return Ui.GARIS;
    }

    @Override
    protected float tebalGarisKartu() {
        return (aktif || isFocusOwner()) ? 1.5f : 1f;
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
        indikator.aturWarna(warna);
        boolean peringatan = warna.equals(Ui.MERAH)
                || warna.equals(Ui.MERAH_BADGE)
                || warna.equals(Ui.KUNING)
                || warna.equals(Ui.KUNING_BADGE);
        nilai.setForeground(peringatan ? warna : Ui.TEKS);
        perbaruiPanah();
        repaint();
    }

    private static final class TitikIndikator extends JPanel {
        private static final long serialVersionUID = 1L;
        private Color warna;

        private TitikIndikator(Color warna) {
            this.warna = warna;
            setOpaque(false);
            Dimension ukuran = new Dimension(Ui.skala(14), Ui.skala(14));
            setPreferredSize(ukuran);
            setMinimumSize(ukuran);
            setMaximumSize(ukuran);
        }

        private void aturWarna(Color baru) {
            this.warna = baru;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                Color dasar = warna == null ? Ui.GARIS_KUAT : warna;
                int dLuar = Ui.skala(12);
                int dDalam = Ui.skala(6);
                int xLuar = (getWidth() - dLuar) / 2;
                int yLuar = (getHeight() - dLuar) / 2;
                g2.setColor(new Color(dasar.getRed(), dasar.getGreen(),
                        dasar.getBlue(), 34));
                g2.fillOval(xLuar, yLuar, dLuar, dLuar);
                int xDalam = (getWidth() - dDalam) / 2;
                int yDalam = (getHeight() - dDalam) / 2;
                g2.setColor(dasar.equals(Ui.TEKS_LEMBUT)
                        ? Ui.GARIS_KUAT : dasar);
                g2.fillOval(xDalam, yDalam, dDalam, dDalam);
            } finally {
                g2.dispose();
            }
        }
    }
}
