package gr.taxpulse.user.repository;

import gr.taxpulse.user.entity.Role;
import gr.taxpulse.user.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    /** Backed by the functional unique index {@code lower(email)}. */
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /** Fallback recipients for alerts about unassigned work. */
    List<User> findByRoleAndActiveTrue(Role role);
}
