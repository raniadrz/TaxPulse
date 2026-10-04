package gr.taxpulse.client.repository;

import gr.taxpulse.client.entity.BookCategory;
import gr.taxpulse.client.entity.Client;
import gr.taxpulse.client.entity.ClientType;
import java.util.UUID;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * Composable query predicates for the client search screen. Each returns {@code null} when the
 * filter is absent, which Spring Data treats as "no restriction".
 */
public final class ClientSpecifications {

    private ClientSpecifications() {
    }

    /**
     * Quick search: a numeric term matches ΑΦΜ by prefix (index-friendly), any other term matches
     * name / trade name case-insensitively with {@code ILIKE}, which uses the trigram GIN index.
     */
    public static Specification<Client> matchesQuery(String query) {
        if (!StringUtils.hasText(query)) {
            return null;
        }
        String term = query.trim();
        return (root, cq, cb) -> {
            if (term.matches("\\d{1,9}")) {
                return cb.like(root.get("afm"), escapeLike(term) + "%", '\\');
            }
            HibernateCriteriaBuilder hcb = (HibernateCriteriaBuilder) cb;
            String pattern = "%" + escapeLike(term) + "%";
            return cb.or(
                    hcb.ilike(root.get("name"), pattern, '\\'),
                    hcb.ilike(root.get("tradeName"), pattern, '\\'));
        };
    }

    public static Specification<Client> hasType(ClientType type) {
        return type == null ? null : (root, cq, cb) -> cb.equal(root.get("clientType"), type);
    }

    public static Specification<Client> hasBookCategory(BookCategory category) {
        return category == null ? null : (root, cq, cb) -> cb.equal(root.get("bookCategory"), category);
    }

    public static Specification<Client> isActive(Boolean active) {
        return active == null ? null : (root, cq, cb) -> cb.equal(root.get("active"), active);
    }

    public static Specification<Client> assignedTo(UUID accountantId) {
        return accountantId == null ? null
                : (root, cq, cb) -> cb.equal(root.get("assignedAccountant").get("id"), accountantId);
    }

    /** Escapes LIKE wildcards so user input is matched literally. */
    static String escapeLike(String input) {
        return input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
