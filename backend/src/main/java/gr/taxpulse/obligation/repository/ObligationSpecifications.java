package gr.taxpulse.obligation.repository;

import gr.taxpulse.obligation.entity.ObligationStatus;
import gr.taxpulse.obligation.entity.ObligationType;
import gr.taxpulse.obligation.entity.TaxObligation;
import jakarta.persistence.criteria.JoinType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/** Composable filters for the obligations list / calendar. {@code null} means "no restriction". */
public final class ObligationSpecifications {

    private ObligationSpecifications() {
    }

    /** Eagerly fetches client and assignee for list rows (skipped for the count query). */
    public static Specification<TaxObligation> fetchAssociations() {
        return (root, query, cb) -> {
            if (query != null && !Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
                root.fetch("client", JoinType.INNER);
                root.fetch("assignedTo", JoinType.LEFT);
            }
            return null;
        };
    }

    public static Specification<TaxObligation> forClient(UUID clientId) {
        return clientId == null ? null : (root, q, cb) -> cb.equal(root.get("client").get("id"), clientId);
    }

    public static Specification<TaxObligation> hasStatusIn(Collection<ObligationStatus> statuses) {
        return statuses == null || statuses.isEmpty() ? null : (root, q, cb) -> root.get("status").in(statuses);
    }

    public static Specification<TaxObligation> hasType(ObligationType type) {
        return type == null ? null : (root, q, cb) -> cb.equal(root.get("obligationType"), type);
    }

    public static Specification<TaxObligation> assignedTo(UUID userId) {
        return userId == null ? null : (root, q, cb) -> cb.equal(root.get("assignedTo").get("id"), userId);
    }

    public static Specification<TaxObligation> dueFrom(LocalDate from) {
        return from == null ? null : (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("dueDate"), from);
    }

    public static Specification<TaxObligation> dueTo(LocalDate to) {
        return to == null ? null : (root, q, cb) -> cb.lessThanOrEqualTo(root.get("dueDate"), to);
    }
}
