package kantinflow.gui;

import javax.swing.JFrame;

/**
 * Pembungkus notifikasi toast ringan non-blocking untuk seluruh halaman.
 */
public final class Notifikasi {
    private Notifikasi() {
    }

    public static void pasang(JFrame jendela) {
        ToastNotification.pasang(jendela);
    }

    public static void sukses(String pesan) {
        ToastNotification.tampilkan(ToastNotification.Tipe.SUKSES, pesan);
    }

    public static void gagal(String pesan) {
        ToastNotification.tampilkan(ToastNotification.Tipe.GAGAL, pesan);
    }

    public static void peringatan(String pesan) {
        ToastNotification.tampilkan(ToastNotification.Tipe.PERINGATAN, pesan);
    }

    public static void info(String pesan) {
        ToastNotification.tampilkan(ToastNotification.Tipe.INFO, pesan);
    }

    public static void bersihkan() {
        ToastNotification.bersihkan();
    }
}
