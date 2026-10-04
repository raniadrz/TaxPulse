package gr.taxpulse.notification.service;

import gr.taxpulse.notification.event.NotificationCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;

/**
 * Optional e-mail channel ({@code taxpulse.notifications.email.enabled=true}).
 * Runs only after the notification transaction commits, so a rolled-back job never sends mail,
 * and an SMTP failure never rolls back the in-app notification.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "taxpulse.notifications.email", name = "enabled", havingValue = "true")
public class EmailNotificationListener {

    private final JavaMailSender mailSender;

    @Value("${taxpulse.notifications.email.from}")
    private String from;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationCreated(NotificationCreatedEvent event) {
        var cmd = event.command();
        if (!StringUtils.hasText(cmd.recipientEmail())) {
            return;
        }
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(from);
            mail.setTo(cmd.recipientEmail());
            mail.setSubject("[TaxPulse] " + cmd.title());
            mail.setText(cmd.message());
            mailSender.send(mail);
        } catch (MailException ex) {
            log.warn("Failed to e-mail notification {} to {}: {}", event.notificationId(), cmd.recipientEmail(), ex.getMessage());
        }
    }
}
