package gr.taxpulse.credential.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Append-only audit trail: who revealed or changed a client's credentials, and when. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "credential_access_log")
public class CredentialAccessLog {

    public enum Action { VIEW, CREATE, UPDATE, DELETE }

    @Id
    private UUID id;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "credential_id")
    private UUID credentialId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CredentialKind kind;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "user_name", nullable = false, length = 150)
    private String userName;

    @Column(name = "by_client", nullable = false)
    private boolean byClient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Action action;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
