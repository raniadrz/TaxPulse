package gr.taxpulse.credential.repository;

import gr.taxpulse.credential.entity.CredentialAccessLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CredentialAccessLogRepository extends JpaRepository<CredentialAccessLog, UUID> {

    List<CredentialAccessLog> findByClientIdOrderByCreatedAtDesc(UUID clientId, Pageable pageable);
}
