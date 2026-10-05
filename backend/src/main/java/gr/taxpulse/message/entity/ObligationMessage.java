package gr.taxpulse.message.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One message of the accountant/client conversation about an obligation. Immutable once written.
 *
 * <p>Obligation and author are plain ids (like notifications) so the conversation module does not
 * pull the obligation aggregate into every read.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "obligation_messages")
public class ObligationMessage {

    @Id
    private UUID id;

    @Column(name = "obligation_id", nullable = false)
    private UUID obligationId;

    @Column(name = "author_id")
    private UUID authorId;

    @Column(name = "author_name", nullable = false, length = 150)
    private String authorName;

    @Column(name = "from_client", nullable = false)
    private boolean fromClient;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
