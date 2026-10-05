package gr.taxpulse.obligation.repository;

import gr.taxpulse.obligation.entity.ObligationStatus;
import gr.taxpulse.obligation.entity.TaxObligation;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface TaxObligationRepository
        extends JpaRepository<TaxObligation, UUID>, JpaSpecificationExecutor<TaxObligation> {

    @EntityGraph(attributePaths = {"client", "assignedTo"})
    @Query("select o from TaxObligation o where o.id = :id")
    Optional<TaxObligation> findDetailedById(UUID id);

    /**
     * Open obligations due on exactly the given date: input of the reminder job.
     * Fetches everything needed to address the notification, avoiding N+1 queries.
     */
    @Query("""
            select o from TaxObligation o
              join fetch o.client c
              left join fetch o.assignedTo
              left join fetch c.assignedAccountant
             where o.dueDate = :dueDate and o.status in :statuses
            """)
    List<TaxObligation> findForReminder(LocalDate dueDate, Collection<ObligationStatus> statuses);

    /** Obligations in the given statuses whose deadline has passed: input of the overdue job. */
    @Query("""
            select o from TaxObligation o
              join fetch o.client c
              left join fetch o.assignedTo
              left join fetch c.assignedAccountant
             where o.dueDate < :today and o.status in :statuses
            """)
    List<TaxObligation> findPastDue(LocalDate today, Collection<ObligationStatus> statuses);

    /** Per-client counters for the CRM list (single grouped query for a whole page). */
    @Query("""
            select o.client.id as clientId,
                   sum(case when o.status in :openStatuses then 1 else 0 end) as openCount,
                   sum(case when o.status = gr.taxpulse.obligation.entity.ObligationStatus.OVERDUE then 1 else 0 end) as overdueCount,
                   min(case when o.status <> gr.taxpulse.obligation.entity.ObligationStatus.SUBMITTED then o.dueDate end) as nextDueDate
              from TaxObligation o
             where o.client.id in :clientIds
             group by o.client.id
            """)
    List<ClientObligationStatsView> aggregateByClient(Collection<UUID> clientIds, Collection<ObligationStatus> openStatuses);

    // ---- Dashboard counters ----

    long countByStatusIn(Collection<ObligationStatus> statuses);

    long countByStatus(ObligationStatus status);

    long countByStatusInAndDueDateBetween(Collection<ObligationStatus> statuses, LocalDate from, LocalDate to);

    long countByStatusAndSubmittedAtGreaterThanEqual(ObligationStatus status, Instant since);

    @EntityGraph(attributePaths = {"client", "assignedTo"})
    List<TaxObligation> findByStatusInOrderByDueDateAsc(Collection<ObligationStatus> statuses, Pageable pageable);

    /** Projection for {@link #aggregateByClient}. */
    interface ClientObligationStatsView {
        UUID getClientId();

        Long getOpenCount();

        Long getOverdueCount();

        LocalDate getNextDueDate();
    }
}
