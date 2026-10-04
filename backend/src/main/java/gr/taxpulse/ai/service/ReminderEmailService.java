package gr.taxpulse.ai.service;

import static gr.taxpulse.ai.prompt.JsonSchemas.object;
import static gr.taxpulse.ai.prompt.JsonSchemas.props;
import static gr.taxpulse.ai.prompt.JsonSchemas.string;
import static java.util.Map.entry;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import gr.taxpulse.ai.dto.ReminderEmailRequest;
import gr.taxpulse.ai.dto.ReminderEmailResponse;
import gr.taxpulse.ai.ollama.ChatModelClient;
import gr.taxpulse.ai.prompt.PromptTemplates;
import gr.taxpulse.client.entity.Client;
import gr.taxpulse.client.entity.ClientRepresentative;
import gr.taxpulse.obligation.entity.TaxObligation;
import gr.taxpulse.obligation.service.TaxObligationService;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Use case 2: drafts a personalised reminder e-mail for a client obligation. */
@Service
@RequiredArgsConstructor
public class ReminderEmailService {

    static final Map<String, Object> SCHEMA = object(props(entry("subject", string()), entry("body", string())));

    private static final DateTimeFormatter GREEK_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale GREEK = Locale.forLanguageTag("el-GR");

    private final ChatModelClient chatModel;
    private final TaxObligationService obligationService;

    @Transactional(readOnly = true)
    public ReminderEmailResponse draft(ReminderEmailRequest request) {
        TaxObligation o = obligationService.getDetailed(request.obligationId());
        Client client = o.getClient();
        ClientRepresentative contact = client.getRepresentatives().stream()
                .filter(ClientRepresentative::isPrimary).findFirst()
                .orElse(client.getRepresentatives().isEmpty() ? null : client.getRepresentatives().get(0));

        String recipientName = contact != null ? contact.getFullName() : client.getName();
        String recipientEmail = contact != null && StringUtils.hasText(contact.getEmail()) ? contact.getEmail() : client.getEmail();
        String accountant = o.getAssignedTo() != null ? o.getAssignedTo().getFullName()
                : client.getAssignedAccountant() != null ? client.getAssignedAccountant().getFullName() : null;

        StringBuilder ctx = new StringBuilder("<context>\n")
                .append("Παραλήπτης: ").append(recipientName).append('\n')
                .append("Πελάτης: ").append(client.getName()).append(" (ΑΦΜ ").append(client.getAfm()).append(")\n")
                .append("Υποχρέωση: ").append(o.getObligationType().label()).append(" - ").append(o.getTitle()).append('\n')
                .append("Προθεσμία: ").append(GREEK_DATE.format(o.getDueDate())).append('\n')
                .append("Κατάσταση: ").append(o.getStatus()).append('\n');
        if (o.getPeriodStart() != null && o.getPeriodEnd() != null) {
            ctx.append("Περίοδος: ").append(GREEK_DATE.format(o.getPeriodStart()))
                    .append(" - ").append(GREEK_DATE.format(o.getPeriodEnd())).append('\n');
        }
        if (o.getAmount() != null) {
            ctx.append("Ποσό: ").append(NumberFormat.getCurrencyInstance(GREEK).format(o.getAmount())).append('\n');
        }
        if (accountant != null) {
            ctx.append("Υπεύθυνος λογιστής: ").append(accountant).append('\n');
        }
        ctx.append("</context>\n");

        String instructions = "Ύφος: " + toneDescription(request.toneOrDefault()) + "."
                + (o.getStatus().name().equals("PENDING_DOCS")
                ? " Ζήτησε από τον πελάτη να αποστείλει τα απαραίτητα παραστατικά/δικαιολογητικά." : "")
                + (StringUtils.hasText(request.additionalInstructions())
                ? "\nΕπιπλέον οδηγίες του λογιστή: " + request.additionalInstructions() : "");

        EmailDraft draft = chatModel.generateStructured(PromptTemplates.REMINDER_EMAIL_SYSTEM,
                ctx + instructions, SCHEMA, EmailDraft.class);
        return new ReminderEmailResponse(draft.subject(), draft.body(), recipientName, recipientEmail);
    }

    private static String toneDescription(ReminderEmailRequest.Tone tone) {
        return switch (tone) {
            case FORMAL -> "επίσημο και ευγενικό";
            case FRIENDLY -> "φιλικό αλλά επαγγελματικό";
            case URGENT -> "επείγον, τονίζοντας τις συνέπειες εκπρόθεσμης υποβολής (πρόστιμα/προσαυξήσεις)";
        };
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record EmailDraft(String subject, String body) {
    }
}
