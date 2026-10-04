package gr.taxpulse.notification.repository;

import gr.taxpulse.notification.entity.Notification;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);

    Page<Notification> findByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);

    long countByRecipientIdAndReadAtIsNull(UUID recipientId);

    Optional<Notification> findByIdAndRecipientId(UUID id, UUID recipientId);

    /**
     * Idempotent insert: {@code ON CONFLICT (dedup_key) DO NOTHING} lets concurrent scheduler runs
     * (or several application instances) race safely without aborting the surrounding transaction.
     * UUIDs are passed as text and cast, because untyped NULL binds are ambiguous for PostgreSQL.
     *
     * @return 1 if inserted, 0 if a notification with the same key already exists
     */
    @Modifying
    @Query(nativeQuery = true, value = """
            insert into notifications (id, recipient_id, obligation_id, client_id, type, channel,
                                       title, message, dedup_key, created_at)
            values (cast(:id as uuid), cast(:recipientId as uuid), cast(:obligationId as uuid),
                    cast(:clientId as uuid), :type, :channel, :title, :message, :dedupKey, :createdAt)
            on conflict (dedup_key) do nothing
            """)
    int insertIfAbsent(String id, String recipientId, String obligationId, String clientId, String type,
                       String channel, String title, String message, String dedupKey, Instant createdAt);

    @Modifying
    @Query("update Notification n set n.readAt = :now where n.recipientId = :recipientId and n.readAt is null")
    int markAllRead(UUID recipientId, Instant now);
}
