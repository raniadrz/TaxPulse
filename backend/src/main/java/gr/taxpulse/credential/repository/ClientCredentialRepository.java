package gr.taxpulse.credential.repository;

import gr.taxpulse.credential.entity.ClientCredential;
import gr.taxpulse.credential.entity.CredentialKind;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientCredentialRepository extends JpaRepository<ClientCredential, UUID> {

    List<ClientCredential> findByClientIdOrderByKindAscLabelAsc(UUID clientId);

    Optional<ClientCredential> findByIdAndClientId(UUID id, UUID clientId);

    boolean existsByClientIdAndKind(UUID clientId, CredentialKind kind);

    boolean existsByClientIdAndKindAndIdNot(UUID clientId, CredentialKind kind, UUID id);
}
