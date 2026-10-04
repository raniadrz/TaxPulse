package gr.taxpulse.ai.dto;

import gr.taxpulse.obligation.entity.ObligationType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Validated extraction result. LLM output is treated as untrusted input: ΑΦΜ check digits are
 * verified, dates/amounts parsed strictly and anything dubious is reported in {@code warnings}.
 */
public record ExtractionResponse(List<ExtractedRecord> records, List<String> warnings, String model) {

    public record ExtractedRecord(
            String afm,
            boolean afmValid,
            String partyName,
            BigDecimal amount,
            String currency,
            LocalDate date,
            String documentType,
            ObligationType obligationType,
            String description,
            ClientMatch matchedClient) {
    }

    /** Existing client whose ΑΦΜ matches the extracted one. */
    public record ClientMatch(UUID id, String name) {
    }
}
