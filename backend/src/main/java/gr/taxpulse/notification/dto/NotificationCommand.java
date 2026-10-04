package gr.taxpulse.notification.dto;

import gr.taxpulse.notification.entity.NotificationType;
import java.util.Objects;
import java.util.UUID;

/**
 * Request to notify one recipient. {@code dedupKey} makes the operation idempotent.
 *
 * @param recipientEmail used by the optional e-mail channel
 */
public record NotificationCommand(
        UUID recipientId,
        String recipientEmail,
        NotificationType type,
        String title,
        String message,
        UUID obligationId,
        UUID clientId,
        String dedupKey) {

    public NotificationCommand {
        Objects.requireNonNull(recipientId, "recipientId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(message, "message");
    }
}
