import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import kantinflow.gui.Badge;
import kantinflow.gui.BadgeRenderer;
import kantinflow.gui.TabelKantin;
import kantinflow.gui.Tema;

/**
 * Pemeriksaan renderer badge: pemetaan warna, kontras latar, posisi vertikal,
 * serta keadaan normal, hover, terpilih, dan nonaktif pada tema terang/gelap.
 */
public class BadgeUxHarness {
    private static int gagal;

    public static void main(String[] args) throws Exception {
        Tema.terapkan(false);
        ujiTema(false);
        Tema.terapkan(true);
        ujiTema(true);
        Tema.terapkan(false);
        System.out.println(gagal == 0
                ? "BADGE_UX_HARNESS_LULUS"
                : "BADGE_UX_HARNESS_GAGAL=" + gagal);
        System.exit(gagal == 0 ? 0 : 1);
    }

    private static void ujiTema(boolean gelap) {
        String tema = gelap ? "gelap" : "terang";
        int hijau = gelap ? 0x86EFAC : 0x15803D;
        int kuning = gelap ? 0xFDE68A : 0xB45309;
        int merah = gelap ? 0xFCA5A5 : 0xB91C1C;
        int oranye = gelap ? 0xFDBA74 : 0xC2410C;
        int netral = gelap ? 0x94A3B8 : 0x64748B;
        int kartu = gelap ? 0x111827 : 0xFFFFFF;
        int hover = gelap ? 0x1E293B : 0xEEF2FF;
        int netralLembut = gelap ? 0x1E293B : 0xF1F5F9;
        int pilihan = gelap ? 0x1E3A8A : 0xDBEAFE;

        cek("WARNA_PENERIMAAN_" + tema, hijau,
                Badge.warnaUntuk("Penerimaan")[0]);
        cek("WARNA_FEFO_" + tema, oranye,
                Badge.warnaUntuk("Pengeluaran FEFO")[0]);
        cek("WARNA_MANUAL_" + tema, oranye,
                Badge.warnaUntuk("Pengeluaran Manual")[0]);
        cek("WARNA_PEMBUANGAN_" + tema, merah,
                Badge.warnaUntuk("Pembuangan")[0]);
        cek("WARNA_SEGERA_" + tema, kuning,
                Badge.warnaUntuk("Segera kedaluwarsa")[0]);
        cek("WARNA_KEDALUWARSA_" + tema, merah,
                Badge.warnaUntuk("Kedaluwarsa")[0]);
        cek("WARNA_HABIS_" + tema, netral,
                Badge.warnaUntuk("Habis")[0]);

        TabelKantin tabel = new TabelKantin(new DefaultTableModel(
                new Object[][]{{"1", "Penerimaan"}},
                new Object[]{"No", "Jenis"}));
        tabel.setSize(320, 120);
        tabel.setRowHeight(40);
        BadgeRenderer renderer = new BadgeRenderer();
        tabel.getColumnModel().getColumn(1).setCellRenderer(renderer);

        JPanel normal = (JPanel) renderer.getTableCellRendererComponent(
                tabel, "Penerimaan", false, false, 0, 1);
        cekLatar("NORMAL_" + tema, normal, kartu);
        Badge badge = (Badge) normal.getComponent(0);
        cek("BADGE_VERTIKAL_" + tema, true, tengah(normal, badge));
        cek("BADGE_TINGGI_MINIMAL_" + tema, true,
                badge.getPreferredSize().height >= (gelap ? 24 : 24));

        tabel.setRowSelectionInterval(0, 0);
        JPanel dipilih = (JPanel) renderer.getTableCellRendererComponent(
                tabel, "Penerimaan", true, false, 0, 1);
        cekLatar("TERPILIH_" + tema, dipilih, tabel.getSelectionBackground());

        tabel.clearSelection();
        MouseEvent gerak = new MouseEvent(tabel, MouseEvent.MOUSE_MOVED,
                System.currentTimeMillis(), 0, 20, tabel.getRowHeight(0) / 2,
                0, false);
        for (MouseMotionListener pendengar : tabel.getMouseMotionListeners()) {
            pendengar.mouseMoved(gerak);
        }
        JPanel sorot = (JPanel) renderer.getTableCellRendererComponent(
                tabel, "Penerimaan", false, false, 0, 1);
        cekLatar("HOVER_" + tema, sorot, hover);

        tabel.setEnabled(false);
        JPanel nonaktif = (JPanel) renderer.getTableCellRendererComponent(
                tabel, "Penerimaan", false, false, 0, 1);
        cekLatar("NONAKTIF_" + tema, nonaktif, netralLembut);
        Badge badgeNonaktif = (Badge) nonaktif.getComponent(0);
        cek("NONAKTIF_TEKS_" + tema, netral,
                badgeNonaktif.getForeground().getRGB() & 0xFFFFFF);
    }

    private static boolean tengah(JPanel panel, Badge badge) {
        panel.setSize(320, 40);
        panel.doLayout();
        int tengahPanel = panel.getHeight() / 2;
        int tengahBadge = badge.getY() + badge.getHeight() / 2;
        return Math.abs(tengahPanel - tengahBadge) <= 2;
    }

    private static void cek(String nama, int harapan, Color warna) {
        cek(nama, harapan, warna == null ? -1 : warna.getRGB() & 0xFFFFFF);
    }

    private static void cek(String nama, int harapan, int nilai) {
        boolean lulus = harapan == nilai;
        System.out.println((lulus ? "[OK] " : "[GAGAL] ") + nama
                + " :: " + Integer.toHexString(nilai)
                + " expected " + Integer.toHexString(harapan));
        if (!lulus) {
            gagal++;
        }
    }

    private static void cek(String nama, boolean harapan, boolean nilai) {
        boolean lulus = harapan == nilai;
        System.out.println((lulus ? "[OK] " : "[GAGAL] ") + nama
                + " :: " + nilai);
        if (!lulus) {
            gagal++;
        }
    }

    private static void cekLatar(String nama, Component komponen, int harapan) {
        cek(nama, harapan, komponen.getBackground());
    }

    private static void cekLatar(String nama, Component komponen,
            Color harapan) {
        cekLatar(nama, komponen, harapan.getRGB() & 0xFFFFFF);
    }
}
