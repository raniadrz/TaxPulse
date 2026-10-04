package gr.taxpulse.client.repository;

import gr.taxpulse.client.entity.Client;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface ClientRepository extends JpaRepository<Client, UUID>, JpaSpecificationExecutor<Client> {

    /** Exact ΑΦΜ lookup, served by the unique index {@code ux_clients_afm}. */
    @EntityGraph(attributePaths = {"assignedAccountant"})
    Optional<Client> findByAfm(String afm);

    boolean existsByAfm(String afm);

    boolean existsByAfmAndIdNot(String afm, UUID id);

    /** Full aggregate for the detail view, avoiding N+1 on the collections. */
    @EntityGraph(attributePaths = {"assignedAccountant", "activityCodes"})
    @Query("select c from Client c where c.id = :id")
    Optional<Client> findDetailedById(UUID id);

    long countByActiveTrue();
}
