package gr.taxpulse.user.repository;

import gr.taxpulse.user.entity.Role;
import gr.taxpulse.user.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, UUID> {

    /** Backed by the functional unique index {@code lower(email)}. */
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /** Minimal projection checked on every authenticated request (see UserAccessCache). */
    @Query("""
            select u.active as active, u.role as role, u.clientId as clientId,
                   (select c.active from Client c where c.id = u.clientId) as clientActive
              from User u where u.id = :id
            """)
    Optional<AccessStatusView> findAccessStatusById(UUID id);

    interface AccessStatusView {
        boolean getActive();

        Role getRole();

        UUID getClientId();

        /** Null for staff; for a portal account, whether its client is still active. */
        Boolean getClientActive();

        /** A portal account works only while its client is active too. */
        default boolean isEnabled() {
            return getActive() && (getClientId() == null || Boolean.TRUE.equals(getClientActive()));
        }
    }

    /** Whether the given client is active (empty when it no longer exists). */
    @Query("select c.active from Client c where c.id = :clientId")
    Optional<Boolean> findClientActive(UUID clientId);

    List<User> findByRoleNot(Role role, Sort sort);

    List<User> findByRole(Role role, Sort sort);

    List<User> findByClientId(UUID clientId, Sort sort);

    List<User> findByClientIdAndActiveTrue(UUID clientId);

    /** Fallback recipients for alerts about unassigned work. */
    List<User> findByRoleAndActiveTrue(Role role);
}
