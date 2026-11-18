package kantinflow.gui;

import javax.swing.JFrame;

import raven.toast.Notifications;

/**
 * Pembungkus notifikasi toast agar panel tidak bergantung langsung pada library.
 */
public final class Notifikasi {
    private static JFrame frame;

    private Notifikasi() {
    }

    public static void pasang(JFrame jendela) {
        frame = jendela;
        try {
            Notifications.getInstance().setJFrame(jendela);
        } catch (RuntimeException | LinkageError exception) {
            // Notifikasi opsional; status aksesibilitas tetap tersedia.
        }
    }

    public static void sukses(String pesan) {
        tampilkan(Notifications.Type.SUCCESS, pesan);
    }

    public static void gagal(String pesan) {
        tampilkan(Notifications.Type.ERROR, pesan);
    }

    public static void peringatan(String pesan) {
        tampilkan(Notifications.Type.WARNING, pesan);
    }

    public static void info(String pesan) {
        tampilkan(Notifications.Type.INFO, pesan);
    }

    public static void bersihkan() {
        try {
            Notifications.getInstance().clearAll();
        } catch (RuntimeException | LinkageError exception) {
            // Notifikasi opsional.
        }
    }

    private static void tampilkan(Notifications.Type tipe, String pesan) {
        if (frame == null || pesan == null || pesan.isBlank()) {
            return;
        }
        try {
            Notifications.getInstance().show(tipe,
                    Notifications.Location.TOP_RIGHT, 3200, pesan);
        } catch (RuntimeException | LinkageError exception) {
            // Jangan menggagalkan operasi bisnis karena notifikasi.
        }
    }
}
