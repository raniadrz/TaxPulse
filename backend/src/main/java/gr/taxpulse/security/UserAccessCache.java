package gr.taxpulse.security;

import gr.taxpulse.user.entity.Role;
import gr.taxpulse.user.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Current {@code active} flag and role of each user, consulted on every authenticated request so
 * that deactivations and role changes take effect immediately instead of when the JWT expires.
 *
 * <p>Entries live for a short TTL to keep this to roughly one indexed primary-key lookup per user
 * per {@link #TTL}. Changes made on this instance evict the entry right after commit; with several
 * instances the TTL bounds how long another node can serve a stale status.</p>
 */
@Component
public class UserAccessCache {

    static final Duration TTL = Duration.ofSeconds(30);

    private final UserRepository userRepository;
    private final Clock clock;
    private final Map<UUID, Entry> entries = new ConcurrentHashMap<>();

    public UserAccessCache(UserRepository userRepository, Clock clock) {
        this.userRepository = userRepository;
        this.clock = clock;
    }

    /** @return the user's current access status, or empty when the user no longer exists */
    public Optional<AccessStatus> get(UUID userId) {
        Instant now = clock.instant();
        Entry cached = entries.get(userId);
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return Optional.of(cached.status());
        }
        Optional<AccessStatus> loaded = userRepository.findAccessStatusById(userId)
                .map(v -> new AccessStatus(v.getActive(), v.getRole()));
        loaded.ifPresentOrElse(
                status -> entries.put(userId, new Entry(status, now.plus(TTL))),
                () -> entries.remove(userId));
        return loaded;
    }

    /** Evicts the user's entry once the current transaction commits (immediately if there is none). */
    public void evictAfterCommit(UUID userId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    entries.remove(userId);
                }
            });
        } else {
            entries.remove(userId);
        }
    }

    public record AccessStatus(boolean active, Role role) {
    }

    private record Entry(AccessStatus status, Instant expiresAt) {
    }
}
