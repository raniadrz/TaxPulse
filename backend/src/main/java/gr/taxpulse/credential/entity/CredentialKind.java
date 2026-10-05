package gr.taxpulse.credential.entity;

/** Public service a stored login is for. */
public enum CredentialKind {
    TAXISNET("TAXISnet (ΑΑΔΕ)"),
    EFKA("e-ΕΦΚΑ"),
    OTHER("Άλλο");

    private final String label;

    CredentialKind(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
