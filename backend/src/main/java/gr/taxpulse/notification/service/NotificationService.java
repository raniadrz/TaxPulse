package gr.taxpulse.notification.service;

import gr.taxpulse.common.dto.PageResponse;
import gr.taxpulse.common.exception.ResourceNotFoundException;
import gr.taxpulse.notification.dto.NotificationCommand;
import gr.taxpulse.notification.dto.NotificationResponse;
import gr.taxpulse.notification.entity.NotificationChannel;
import gr.taxpulse.notification.event.NotificationCreatedEvent;
import gr.taxpulse.notification.mapper.NotificationMapper;
import gr.taxpulse.notification.repository.NotificationRepository;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates and serves in-app notifications; other channels subscribe to {@link NotificationCreatedEvent}. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository repository;
    private final NotificationMapper mapper;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    /**
     * Inserts the notification unless one with the same dedup key exists.
     *
     * @return {@code true} when a new notification was created
     */
    @Transactional
    public boolean notify(NotificationCommand cmd) {
        UUID id = UUID.randomUUID();
        int inserted = repository.insertIfAbsent(
                id.toString(), cmd.recipientId().toString(), str(cmd.obligationId()), str(cmd.clientId()),
                cmd.type().name(), NotificationChannel.IN_APP.name(), cmd.title(), cmd.message(),
                cmd.dedupKey(), clock.instant());
        if (inserted == 1) {
            events.publishEvent(new NotificationCreatedEvent(id, cmd));
            return true;
        }
        return false;
    }

    public PageResponse<NotificationResponse> list(UUID recipientId, boolean unreadOnly, Pageable pageable) {
        var page = unreadOnly
                ? repository.findByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(recipientId, pageable)
                : repository.findByRecipientIdOrderByCreatedAtDesc(recipientId, pageable);
        return PageResponse.from(page, mapper::toResponse);
    }

    public long unreadCount(UUID recipientId) {
        return repository.countByRecipientIdAndReadAtIsNull(recipientId);
    }

    /** Marks one of the caller's own notifications as read (404 for someone else's). */
    @Transactional
    public NotificationResponse markRead(UUID recipientId, UUID notificationId) {
        var notification = repository.findByIdAndRecipientId(notificationId, recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));
        if (notification.getReadAt() == null) {
            notification.setReadAt(clock.instant());
        }
        return mapper.toResponse(notification);
    }

    @Transactional
    public int markAllRead(UUID recipientId) {
        return repository.markAllRead(recipientId, clock.instant());
    }

    private static String str(UUID id) {
        return id == null ? null : id.toString();
    }
}
