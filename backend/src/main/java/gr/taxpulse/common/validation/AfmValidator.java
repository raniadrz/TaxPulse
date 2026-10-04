package gr.taxpulse.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validates a Greek ΑΦΜ (Αριθμός Φορολογικού Μητρώου) using the official check-digit algorithm.
 *
 * <p>Algorithm: for the first 8 digits {@code d1..d8}, compute {@code Σ di * 2^(9-i)}; the 9th
 * digit must equal {@code (sum mod 11) mod 10}. "000000000" is rejected explicitly.</p>
 */
public class AfmValidator implements ConstraintValidator<ValidAfm, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Null handling is delegated to @NotNull / @NotBlank so optional ΑΦΜ fields are supported.
        return value == null || isValidAfm(value);
    }

    /** Pure function, reusable outside Bean Validation (e.g. to verify LLM-extracted values). */
    public static boolean isValidAfm(String afm) {
        if (afm == null || !afm.matches("\\d{9}") || "000000000".equals(afm)) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 8; i++) {
            sum += Character.digit(afm.charAt(i), 10) << (8 - i);
        }
        int check = (sum % 11) % 10;
        return check == Character.digit(afm.charAt(8), 10);
    }
}
