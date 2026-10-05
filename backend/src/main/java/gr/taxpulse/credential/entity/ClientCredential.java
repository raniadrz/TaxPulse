package gr.taxpulse.credential.entity;

import gr.taxpulse.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A client's login for a public service. The password is stored only encrypted
 * (see CredentialCipher) and never leaves the server except through an audited reveal.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "client_credentials")
public class ClientCredential extends BaseEntity {

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CredentialKind kind;

    @Column(length = 100)
    private String label;

    @Column(nullable = false)
    private String username;

    @Column(name = "password_encrypted", nullable = false, columnDefinition = "text")
    private String passwordEncrypted;

    @Column(name = "updated_by_id")
    private UUID updatedById;

    @Column(name = "updated_by_name", length = 150)
    private String updatedByName;

    @Column(name = "updated_by_client", nullable = false)
    private boolean updatedByClient;
}
