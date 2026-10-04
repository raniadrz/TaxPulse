package gr.taxpulse.document.repository;

import gr.taxpulse.document.entity.Document;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    @EntityGraph(attributePaths = {"uploadedBy", "obligation"})
    Page<Document> findByClientIdOrderByCreatedAtDesc(UUID clientId, Pageable pageable);

    @EntityGraph(attributePaths = {"client", "uploadedBy", "obligation"})
    @Query("select d from Document d where d.id = :id")
    Optional<Document> findDetailedById(UUID id);

    boolean existsByClientIdAndChecksumSha256(UUID clientId, String checksumSha256);
}
