package gr.taxpulse.security;

import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Static accessor for the authenticated principal, for services that need the acting user. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Optional<UserPrincipal> get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    public static UserPrincipal require() {
        return get().orElseThrow(() -> new IllegalStateException("No authenticated user in context"));
    }
}
