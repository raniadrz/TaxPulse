package gr.taxpulse.obligation.reminder;

import gr.taxpulse.config.TaxPulseProperties;
import gr.taxpulse.notification.dto.NotificationCommand;
import gr.taxpulse.notification.entity.NotificationType;
import gr.taxpulse.notification.service.NotificationService;
import gr.taxpulse.obligation.entity.ObligationStatus;
import gr.taxpulse.obligation.entity.TaxObligation;
import gr.taxpulse.obligation.repository.TaxObligationRepository;
import gr.taxpulse.obligation.service.OfficeClock;
import gr.taxpulse.user.entity.Role;
import gr.taxpulse.user.entity.User;
import gr.taxpulse.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core of the automated alert system:
 * <ol>
 *   <li>flags open obligations whose deadline has passed as {@code OVERDUE} and alerts staff;</li>
 *   <li>sends "due in N days" reminders for each configured offset (default 7, 3, 1).</li>
 * </ol>
 * Every notification carries a deterministic dedup key, so the job is safe to re-run at any time.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeadlineReminderService {

    private static final DateTimeFormatter GREEK_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final TaxObligationRepository obligationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final OfficeClock officeClock;
    private final TaxPulseProperties properties;

    @Transactional
    public ReminderRunResult run() {
        LocalDate today = officeClock.today();
        List<User> admins = userRepository.findByRoleAndActiveTrue(Role.ADMIN);

        int overdue = 0;
        int notifications = 0;
        for (TaxObligation o : obligationRepository.findOpenPastDue(today, ObligationStatus.OPEN)) {
            if (o.markOverdueIfPastDue(today)) {
                overdue++;
                for (User recipient : recipientsOf(o, admins)) {
                    if (notificationService.notify(overdueNotice(o, recipient))) {
                        notifications++;
                    }
                }
            }
        }

        for (int daysBefore : properties.reminders().daysBefore()) {
            LocalDate dueDate = today.plusDays(daysBefore);
            for (TaxObligation o : obligationRepository.findForReminder(dueDate, ObligationStatus.OPEN)) {
                for (User recipient : recipientsOf(o, admins)) {
                    if (notificationService.notify(upcomingNotice(o, recipient, daysBefore))) {
                        notifications++;
                    }
                }
            }
        }

        ReminderRunResult result = new ReminderRunResult(today, overdue, notifications);
        log.info("Deadline reminder run: {}", result);
        return result;
    }

    /** Assignee, else the client's accountant, else every active administrator. */
    private static List<User> recipientsOf(TaxObligation o, List<User> admins) {
        User target = o.getAssignedTo() != null ? o.getAssignedTo() : o.getClient().getAssignedAccountant();
        if (target != null && target.isActive()) {
            return List.of(target);
        }
        return admins;
    }

    private static NotificationCommand upcomingNotice(TaxObligation o, User recipient, int daysBefore) {
        String when = daysBefore == 1 ? "αύριο" : "σε %d ημέρες".formatted(daysBefore);
        String title = "Λήξη %s: %s – %s".formatted(when, o.getObligationType().label(), o.getClient().getName());
        String message = "Η υποχρέωση «%s» του πελάτη %s (ΑΦΜ %s) λήγει στις %s. Τρέχουσα κατάσταση: %s."
                .formatted(o.getTitle(), o.getClient().getName(), o.getClient().getAfm(),
                        GREEK_DATE.format(o.getDueDate()), statusLabel(o.getStatus()));
        // Due date is part of the key: a rescheduled deadline produces fresh reminders.
        String key = "UPCOMING:%s:%s:D-%d:%s".formatted(o.getId(), recipient.getId(), daysBefore, o.getDueDate());
        return new NotificationCommand(recipient.getId(), recipient.getEmail(), NotificationType.DEADLINE_UPCOMING,
                title, message, o.getId(), o.getClient().getId(), key);
    }

    private static NotificationCommand overdueNotice(TaxObligation o, User recipient) {
        String title = "Εκπρόθεσμη: %s – %s".formatted(o.getObligationType().label(), o.getClient().getName());
        String message = "Η προθεσμία της υποχρέωσης «%s» του πελάτη %s (ΑΦΜ %s) έληξε στις %s χωρίς υποβολή."
                .formatted(o.getTitle(), o.getClient().getName(), o.getClient().getAfm(), GREEK_DATE.format(o.getDueDate()));
        String key = "OVERDUE:%s:%s:%s".formatted(o.getId(), recipient.getId(), o.getDueDate());
        return new NotificationCommand(recipient.getId(), recipient.getEmail(), NotificationType.DEADLINE_OVERDUE,
                title, message, o.getId(), o.getClient().getId(), key);
    }

    private static String statusLabel(ObligationStatus status) {
        return switch (status) {
            case PENDING_DOCS -> "Αναμονή δικαιολογητικών";
            case IN_PROGRESS -> "Σε εξέλιξη";
            case SUBMITTED -> "Υποβλήθηκε";
            case OVERDUE -> "Εκπρόθεσμη";
        };
    }

    public record ReminderRunResult(LocalDate runDate, int markedOverdue, int notificationsCreated) {
    }
}
