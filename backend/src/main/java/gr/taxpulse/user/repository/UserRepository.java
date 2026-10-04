package gr.taxpulse.user.repository;

import gr.taxpulse.user.entity.Role;
import gr.taxpulse.user.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, UUID> {

    /** Backed by the functional unique index {@code lower(email)}. */
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /** Minimal projection checked on every authenticated request (see UserAccessCache). */
    @Query("select u.active as active, u.role as role from User u where u.id = :id")
    Optional<AccessStatusView> findAccessStatusById(UUID id);

    interface AccessStatusView {
        boolean getActive();

        Role getRole();
    }

    /** Fallback recipients for alerts about unassigned work. */
    List<User> findByRoleAndActiveTrue(Role role);
}
