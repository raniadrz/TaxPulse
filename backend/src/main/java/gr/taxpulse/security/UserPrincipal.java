package gr.taxpulse.security;

import gr.taxpulse.user.entity.Role;
import gr.taxpulse.user.entity.User;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Authenticated principal. Immutable snapshot of the user so the security context never
 * holds a managed JPA entity (which would be detached outside the transaction).
 */
public record UserPrincipal(UUID id, String email, String fullName, Role role, String passwordHash, boolean active,
                            UUID clientId) implements UserDetails {

    public static UserPrincipal from(User user) {
        return new UserPrincipal(user.getId(), user.getEmail(), user.getFullName(), user.getRole(),
                user.getPasswordHash(), user.isActive(), user.getClientId());
    }

    public UserPrincipal withActive(boolean value) {
        return new UserPrincipal(id, email, fullName, role, passwordHash, value, clientId);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.authority()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }

    /** Don't print credential material in logs. */
    @Override
    public String toString() {
        return "UserPrincipal[id=%s, email=%s, role=%s]".formatted(id, email, role);
    }
}
