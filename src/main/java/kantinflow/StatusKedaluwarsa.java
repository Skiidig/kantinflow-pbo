package kantinflow;

public enum StatusKedaluwarsa {
    KEDALUWARSA("Kedaluwarsa"),
    SEGERA_KEDALUWARSA("Segera kedaluwarsa"),
    AMAN("Aman");

    private final String label;

    StatusKedaluwarsa(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
