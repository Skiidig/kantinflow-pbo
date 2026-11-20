package kantinflow.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * Konfirmasi modal bergaya kartu untuk tindakan yang berisiko.
 */
public final class DialogKonfirmasi {
    private DialogKonfirmasi() {
    }

    public static boolean tampilkan(Component induk, String judul,
            String pesan, String labelKonfirmasi) {
        Window pemilik = SwingUtilities.getWindowAncestor(induk);
        JDialog dialog = new JDialog(pemilik, judul,
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setName("dialogKonfirmasi");
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        Ui.pasangTitleBar(dialog.getRootPane());

        JPanel konten = new JPanel(new BorderLayout(0, 16));
        konten.setBackground(Ui.LATAR);
        konten.setBorder(new EmptyBorder(Ui.skala(16), Ui.skala(16),
                Ui.skala(16), Ui.skala(16)));
        JPanel kartu = new CardPanel(new BorderLayout(0, 12));
        JPanel kepala = new JPanel(new BorderLayout(10, 0));
        kepala.setOpaque(false);
        JLabel ikon = new JLabel(Ui.ikonAksi("alert-triangle", 22, Ui.KUNING));
        JLabel labelJudul = new JLabel(judul);
        labelJudul.setFont(Ui.font(java.awt.Font.BOLD, Ui.FONT_KARTU));
        labelJudul.setForeground(Ui.TEKS);
        kepala.add(ikon, BorderLayout.WEST);
        kepala.add(labelJudul, BorderLayout.CENTER);
        kartu.add(kepala, BorderLayout.NORTH);

        JTextArea teks = new JTextArea(pesan);
        teks.setEditable(false);
        teks.setOpaque(false);
        teks.setLineWrap(true);
        teks.setWrapStyleWord(true);
        teks.setFont(Ui.font(java.awt.Font.PLAIN, Ui.FONT_BODY));
        teks.setForeground(Ui.TEKS_LEMBUT);
        teks.setBorder(BorderFactory.createEmptyBorder());
        JScrollPane scroll = new JScrollPane(teks);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setPreferredSize(new Dimension(Ui.skala(390), Ui.skala(100)));
        kartu.add(scroll, BorderLayout.CENTER);

        JButton batal = Ui.sekunder("Batal");
        JButton setuju = Ui.utama(labelKonfirmasi == null
                ? "Lanjutkan" : labelKonfirmasi);
        JPanel aksi = new JPanel();
        aksi.setOpaque(false);
        aksi.setLayout(new BoxLayout(aksi, BoxLayout.X_AXIS));
        aksi.add(Box.createHorizontalGlue());
        aksi.add(batal);
        aksi.add(Box.createHorizontalStrut(8));
        aksi.add(setuju);
        kartu.add(aksi, BorderLayout.SOUTH);
        konten.add(kartu, BorderLayout.CENTER);
        dialog.setContentPane(konten);
        dialog.getRootPane().setDefaultButton(setuju);
        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "batal");
        dialog.getRootPane().getActionMap().put("batal", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dialog.dispose();
            }
        });
        final boolean[] hasil = {false};
        batal.addActionListener(event -> dialog.dispose());
        setuju.addActionListener(event -> {
            hasil[0] = true;
            dialog.dispose();
        });
        dialog.pack();
        Rectangle batas = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getMaximumWindowBounds();
        dialog.setSize(Math.min(dialog.getWidth(),
                Math.max(360, batas.width - 48)),
                Math.min(dialog.getHeight(),
                        Math.max(240, batas.height - 64)));
        dialog.setLocationRelativeTo(induk);
        SwingUtilities.invokeLater(setuju::requestFocusInWindow);
        dialog.setVisible(true);
        return hasil[0];
    }
}
