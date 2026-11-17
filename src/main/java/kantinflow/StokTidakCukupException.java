package kantinflow;

/**
 * Dilempar ketika jumlah pengeluaran melebihi sisa batch yang dipilih.
 */
public class StokTidakCukupException extends RuntimeException {
    public StokTidakCukupException(String message) {
        super(message);
    }
}
