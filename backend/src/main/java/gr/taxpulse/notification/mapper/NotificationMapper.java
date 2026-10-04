package gr.taxpulse.notification.mapper;

import gr.taxpulse.notification.dto.NotificationResponse;
import gr.taxpulse.notification.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getMessage(),
                n.getObligationId(), n.getClientId(), n.getReadAt() != null, n.getCreatedAt());
    }
}
