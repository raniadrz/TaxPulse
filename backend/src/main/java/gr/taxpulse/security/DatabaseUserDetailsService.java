package gr.taxpulse.security;

import gr.taxpulse.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Loads principals from the {@code users} table for username/password authentication. */
@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .map(user -> {
                    UserPrincipal principal = UserPrincipal.from(user);
                    // A portal account of a deactivated client is treated as disabled.
                    boolean clientActive = user.getClientId() == null
                            || userRepository.findClientActive(user.getClientId()).orElse(false);
                    return clientActive ? principal : principal.withActive(false);
                })
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
