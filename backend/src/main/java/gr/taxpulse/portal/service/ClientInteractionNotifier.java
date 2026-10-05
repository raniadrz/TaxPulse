package gr.taxpulse.portal.service;

import gr.taxpulse.client.entity.Client;
import gr.taxpulse.client.service.ClientService;
import gr.taxpulse.document.event.DocumentAddedEvent;
import gr.taxpulse.notification.dto.NotificationCommand;
import gr.taxpulse.notification.entity.NotificationType;
import gr.taxpulse.notification.service.NotificationService;
import gr.taxpulse.obligation.entity.ObligationStatus;
import gr.taxpulse.obligation.entity.TaxObligation;
import gr.taxpulse.obligation.event.ObligationStatusChangedEvent;
import gr.taxpulse.obligation.service.TaxObligationService;
import gr.taxpulse.security.UserPrincipal;
import gr.taxpulse.user.entity.Role;
import gr.taxpulse.user.entity.User;
import gr.taxpulse.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Keeps the office and the client informed of each other's actions:
 * <ul>
 *   <li>client uploads and messages reach the responsible staff member;</li>
 *   <li>office messages, shared documents and workflow progress reach the client's portal accounts.</li>
 * </ul>
 * Listeners run synchronously inside the originating transaction, so a notification exists exactly
 * when the action that caused it was committed.
 */
@Component
@RequiredArgsConstructor
public class ClientInteractionNotifier {

    private static final int SNIPPET_LENGTH = 160;

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final ClientService clientService;
    private final TaxObligationService obligationService;

    public void messagePosted(TaxObligation obligation, UUID messageId, UserPrincipal author, String body) {
        Client client = obligation.getClient();
        if (author.role() == Role.CLIENT) {
            for (User recipient : officeRecipients(obligation, client)) {
                send(recipient, NotificationType.MESSAGE,
                        "Μήνυμα από %s: %s".formatted(client.getName(), obligation.getTitle()),
                        "%s: %s".formatted(author.fullName(), snippet(body)),
                        obligation.getId(), client.getId(), "MSG:%s:%s".formatted(messageId, recipient.getId()));
            }
        } else {
            for (User recipient : clientRecipients(client.getId())) {
                send(recipient, NotificationType.MESSAGE,
                        "Νέο μήνυμα από το γραφείο: " + obligation.getTitle(),
                        "%s: %s".formatted(author.fullName(), snippet(body)),
                        obligation.getId(), client.getId(), "MSG:%s:%s".formatted(messageId, recipient.getId()));
            }
        }
    }

    @EventListener
    public void onStatusChanged(ObligationStatusChangedEvent e) {
        String title;
        String message;
        if (e.current() == ObligationStatus.SUBMITTED) {
            title = "Υποβλήθηκε: " + e.typeLabel();
            message = "Η υποχρέωση «%s» υποβλήθηκε από το γραφείο%s.".formatted(e.title(),
                    StringUtils.hasText(e.submissionRef()) ? " (αρ. πρωτοκόλλου " + e.submissionRef() + ")" : "");
        } else if (e.previous() == ObligationStatus.PENDING_DOCS && e.current() == ObligationStatus.IN_PROGRESS) {
            title = "Παραλάβαμε τα δικαιολογητικά: " + e.typeLabel();
            message = "Το γραφείο ξεκίνησε την επεξεργασία της υποχρέωσης «%s».".formatted(e.title());
        } else if (e.current() == ObligationStatus.PENDING_DOCS) {
            title = "Χρειάζονται δικαιολογητικά: " + e.typeLabel();
            message = "Το γραφείο χρειάζεται επιπλέον δικαιολογητικά για την υποχρέωση «%s». Δείτε τα μηνύματα της υποχρέωσης."
                    .formatted(e.title());
        } else {
            return; // e.g. a re-opened filing: internal bookkeeping, nothing for the client to do
        }
        for (User recipient : clientRecipients(e.clientId())) {
            send(recipient, NotificationType.STATUS_CHANGED, title, message, e.obligationId(), e.clientId(),
                    "STATUS:%s:%s:%s:%s".formatted(e.obligationId(), recipient.getId(), e.current(), UUID.randomUUID()));
        }
    }

    @EventListener
    public void onDocumentAdded(DocumentAddedEvent e) {
        Client client = clientService.getEntity(e.clientId());
        if (e.uploadedByClient()) {
            TaxObligation obligation = e.obligationId() == null ? null : obligationService.getDetailed(e.obligationId());
            String forWhat = obligation == null ? "" : " για την υποχρέωση «%s»".formatted(obligation.getTitle());
            for (User recipient : officeRecipients(obligation, client)) {
                send(recipient, NotificationType.DOCUMENT_RECEIVED,
                        "Νέο έγγραφο από πελάτη: " + client.getName(),
                        "Ο πελάτης %s (ΑΦΜ %s) ανέβασε το έγγραφο «%s»%s."
                                .formatted(client.getName(), client.getAfm(), e.filename(), forWhat),
                        e.obligationId(), client.getId(), "DOC_IN:%s:%s".formatted(e.documentId(), recipient.getId()));
            }
        } else {
            for (User recipient : clientRecipients(client.getId())) {
                send(recipient, NotificationType.DOCUMENT_RECEIVED,
                        "Νέο έγγραφο από το γραφείο",
                        "Το γραφείο πρόσθεσε το έγγραφο «%s» στα έγγραφά σας.".formatted(e.filename()),
                        e.obligationId(), client.getId(), "DOC_OUT:%s:%s".formatted(e.documentId(), recipient.getId()));
            }
        }
    }

    /** Tells the other side that the client's stored logins changed (never includes the secret itself). */
    public void credentialsChanged(UUID clientId, UserPrincipal actor, String kindLabel, String verb) {
        Client client = clientService.getEntity(clientId);
        String title = "Κωδικοί %s: %s".formatted(kindLabel, verb);
        String key = "CRED:%s:%s".formatted(clientId, UUID.randomUUID());
        if (actor.role() == Role.CLIENT) {
            for (User recipient : officeRecipients(null, client)) {
                send(recipient, NotificationType.CREDENTIALS_UPDATED, title + " από " + client.getName(),
                        "Ο πελάτης %s (ΑΦΜ %s): οι κωδικοί %s %s από %s."
                                .formatted(client.getName(), client.getAfm(), kindLabel, verb, actor.fullName()),
                        null, clientId, key + ":" + recipient.getId());
            }
        } else {
            for (User recipient : clientRecipients(clientId)) {
                send(recipient, NotificationType.CREDENTIALS_UPDATED, title + " από το γραφείο",
                        "Οι κωδικοί %s %s από %s. Αν δεν το περιμένατε, επικοινωνήστε με το γραφείο."
                                .formatted(kindLabel, verb, actor.fullName()),
                        null, clientId, key + ":" + recipient.getId());
            }
        }
    }

    /** Obligation assignee, else the client's accountant, else every active administrator. */
    private List<User> officeRecipients(TaxObligation obligation, Client client) {
        User target = obligation != null && obligation.getAssignedTo() != null
                ? obligation.getAssignedTo()
                : client.getAssignedAccountant();
        if (target != null && target.isActive() && target.getRole().isStaff()) {
            return List.of(target);
        }
        return userRepository.findByRoleAndActiveTrue(Role.ADMIN);
    }

    private List<User> clientRecipients(UUID clientId) {
        return userRepository.findByClientIdAndActiveTrue(clientId);
    }

    private void send(User recipient, NotificationType type, String title, String message,
                      UUID obligationId, UUID clientId, String dedupKey) {
        notificationService.notify(new NotificationCommand(recipient.getId(), recipient.getEmail(), type,
                title, message, obligationId, clientId, dedupKey));
    }

    private static String snippet(String body) {
        String flat = body.replaceAll("\\s+", " ").trim();
        return flat.length() <= SNIPPET_LENGTH ? flat : flat.substring(0, SNIPPET_LENGTH - 1) + "…";
    }
}
