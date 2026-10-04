package gr.taxpulse.ai.service;

import static gr.taxpulse.ai.prompt.JsonSchemas.arrayOf;
import static gr.taxpulse.ai.prompt.JsonSchemas.nullableEnum;
import static gr.taxpulse.ai.prompt.JsonSchemas.nullableNumber;
import static gr.taxpulse.ai.prompt.JsonSchemas.nullableString;
import static gr.taxpulse.ai.prompt.JsonSchemas.object;
import static gr.taxpulse.ai.prompt.JsonSchemas.props;
import static java.util.Map.entry;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import gr.taxpulse.ai.dto.ExtractionResponse;
import gr.taxpulse.ai.dto.ExtractionResponse.ClientMatch;
import gr.taxpulse.ai.dto.ExtractionResponse.ExtractedRecord;
import gr.taxpulse.ai.ollama.ChatModelClient;
import gr.taxpulse.ai.ollama.OllamaProperties;
import gr.taxpulse.ai.prompt.PromptTemplates;
import gr.taxpulse.client.repository.ClientRepository;
import gr.taxpulse.common.validation.AfmValidator;
import gr.taxpulse.obligation.entity.ObligationType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Use case 1: unstructured text -> validated structured records (ΑΦΜ, ποσό, ημερομηνία, ...). */
@Service
@RequiredArgsConstructor
public class DataExtractionService {

    /** JSON Schema enforced by Ollama's constrained decoding. */
    static final Map<String, Object> SCHEMA = object(props(
            entry("records", arrayOf(object(props(
                    entry("afm", nullableString()),
                    entry("partyName", nullableString()),
                    entry("amount", nullableNumber()),
                    entry("currency", nullableString()),
                    entry("date", nullableString()),
                    entry("documentType", nullableString()),
                    entry("obligationType", nullableEnum(Arrays.stream(ObligationType.values()).map(Enum::name).toList())),
                    entry("description", nullableString())))))));

    private final ChatModelClient chatModel;
    private final OllamaProperties ollamaProperties;
    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public ExtractionResponse extract(String text) {
        String prompt = "<text>\n" + text + "\n</text>";
        RawExtraction raw = chatModel.generateStructured(PromptTemplates.EXTRACTION_SYSTEM, prompt, SCHEMA, RawExtraction.class);

        List<String> warnings = new ArrayList<>();
        List<ExtractedRecord> records = raw.records() == null ? List.of()
                : raw.records().stream().map(r -> validate(r, warnings)).toList();
        return new ExtractionResponse(records, warnings, ollamaProperties.chatModel());
    }

    private ExtractedRecord validate(RawRecord r, List<String> warnings) {
        String afm = normalizeAfm(r.afm());
        boolean afmValid = afm != null && AfmValidator.isValidAfm(afm);
        if (afm != null && !afmValid) {
            warnings.add("Ο ΑΦΜ %s δεν περνά τον έλεγχο εγκυρότητας".formatted(afm));
        }
        ClientMatch match = afmValid
                ? clientRepository.findByAfm(afm).map(c -> new ClientMatch(c.getId(), c.getName())).orElse(null)
                : null;

        LocalDate date = null;
        if (StringUtils.hasText(r.date())) {
            try {
                date = LocalDate.parse(r.date().trim());
            } catch (DateTimeParseException ex) {
                warnings.add("Μη αναγνωρίσιμη ημερομηνία: " + r.date());
            }
        }

        BigDecimal amount = r.amount() == null ? null : r.amount().setScale(2, RoundingMode.HALF_UP);
        if (amount != null && amount.signum() < 0) {
            warnings.add("Αρνητικό ποσό %s - ελέγξτε αν πρόκειται για πιστωτικό".formatted(amount));
        }

        return new ExtractedRecord(afm, afmValid, trimToNull(r.partyName()), amount,
                r.currency() == null ? null : r.currency().trim().toUpperCase(), date,
                trimToNull(r.documentType()), parseType(r.obligationType()), trimToNull(r.description()), match);
    }

    /** Accepts "EL 094014201", "094 014 201", etc. */
    static String normalizeAfm(String afm) {
        if (!StringUtils.hasText(afm)) {
            return null;
        }
        String digits = afm.replaceAll("(?i)^\\s*(EL|GR)", "").replaceAll("\\D", "");
        return digits.isEmpty() ? null : digits;
    }

    private static ObligationType parseType(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return ObligationType.valueOf(value.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String trimToNull(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }

    /** Raw model output, deliberately lenient (strings) - validated above. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record RawExtraction(List<RawRecord> records) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RawRecord(String afm, String partyName, BigDecimal amount, String currency, String date,
                     String documentType, String obligationType, String description) {
    }
}
