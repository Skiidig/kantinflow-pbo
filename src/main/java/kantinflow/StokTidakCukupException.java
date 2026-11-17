package kantinflow;

/**
 * Dilempar ketika jumlah pengeluaran melebihi stok batch yang tersedia.
 */
public class StokTidakCukupException extends RuntimeException {
    public StokTidakCukupException(String message) {
        super(message);
    }
}
