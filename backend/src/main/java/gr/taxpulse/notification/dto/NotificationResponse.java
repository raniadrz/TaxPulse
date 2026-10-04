package gr.taxpulse.notification.dto;

import gr.taxpulse.notification.entity.NotificationType;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        String title,
        String message,
        UUID obligationId,
        UUID clientId,
        boolean read,
        Instant createdAt) {
}
