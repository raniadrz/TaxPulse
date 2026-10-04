package gr.taxpulse.obligation.entity;

/** Categories of recurring tax / regulatory obligations handled by a Greek accounting office. */
public enum ObligationType {
    /** Περιοδική δήλωση ΦΠΑ. */
    VAT("ΦΠΑ"),
    /** Δήλωση φορολογίας εισοδήματος (Ε1 / Ν). */
    INCOME_TAX("Φόρος Εισοδήματος"),
    /** Αναλυτική Περιοδική Δήλωση (e-ΕΦΚΑ). */
    APD("ΑΠΔ"),
    /** Ηλεκτρονικά βιβλία ΑΑΔΕ (myDATA). */
    MYDATA("myDATA"),
    /** Γενικό Εμπορικό Μητρώο: οικονομικές καταστάσεις, μεταβολές. */
    GEMI("ΓΕΜΗ"),
    /** Ενιαίος Φόρος Ιδιοκτησίας Ακινήτων. */
    ENFIA("ΕΝΦΙΑ"),
    /** Παρακρατούμενοι φόροι (ΦΜΥ κ.λπ.). */
    WITHHOLDING_TAX("Παρακρατούμενοι Φόροι"),
    /** Μισθοδοσία / ΕΡΓΑΝΗ. */
    PAYROLL("Μισθοδοσία"),
    /** Ενδοκοινοτικές συναλλαγές. */
    INTRASTAT("Intrastat"),
    OTHER("Λοιπά");

    private final String label;

    ObligationType(String label) {
        this.label = label;
    }

    /** Greek display label (used in notifications and AI prompts). */
    public String label() {
        return label;
    }
}
