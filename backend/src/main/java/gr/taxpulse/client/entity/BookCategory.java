package gr.taxpulse.client.entity;

/**
 * Κατηγορία βιβλίων (bookkeeping category) under the Greek accounting standards (ΕΛΠ).
 * <ul>
 *   <li>{@code NONE} - no business books (e.g. salaried individuals).</li>
 *   <li>{@code A} - Α' κατηγορίας (historically: simple revenue/expense records).</li>
 *   <li>{@code B} - Β' κατηγορίας / απλογραφικά (single-entry).</li>
 *   <li>{@code C} - Γ' κατηγορίας / διπλογραφικά (double-entry).</li>
 * </ul>
 */
public enum BookCategory {
    NONE,
    A,
    B,
    C
}
