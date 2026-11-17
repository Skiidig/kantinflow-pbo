package kantinflow.penyimpanan;

public class PenyimpananException extends RuntimeException {
    public PenyimpananException(String message) {
        super(message);
    }

    public PenyimpananException(String message, Throwable cause) {
        super(message, cause);
    }
}
