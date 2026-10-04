package gr.taxpulse.notification.event;

import gr.taxpulse.notification.dto.NotificationCommand;
import java.util.UUID;

/** Published after a notification row is inserted; consumed by out-of-band channels (e-mail, push). */
public record NotificationCreatedEvent(UUID notificationId, NotificationCommand command) {
}
