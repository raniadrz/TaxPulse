package gr.taxpulse.message.repository;

import gr.taxpulse.message.entity.ObligationMessage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ObligationMessageRepository extends JpaRepository<ObligationMessage, UUID> {

    List<ObligationMessage> findByObligationIdOrderByCreatedAtAsc(UUID obligationId);
}
