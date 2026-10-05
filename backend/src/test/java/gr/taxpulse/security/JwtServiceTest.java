package gr.taxpulse.security;

import static org.assertj.core.api.Assertions.assertThat;

import gr.taxpulse.config.TaxPulseProperties;
import gr.taxpulse.user.entity.Role;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final Instant T0 = Instant.parse("2026-10-04T08:00:00Z");

    private static JwtService serviceAt(Instant now, String secret) {
        var props = new TaxPulseProperties(
                new TaxPulseProperties.Security(secret, Duration.ofHours(1), "taxpulse", null),
                new TaxPulseProperties.Cors(List.of("http://localhost")),
                new TaxPulseProperties.Reminders(true, List.of(1), "0 0 7 * * *", "Europe/Athens"),
                new TaxPulseProperties.Storage("/tmp"),
                new TaxPulseProperties.Bootstrap(null, null, null));
        return new JwtService(props, Clock.fixed(now, ZoneOffset.UTC));
    }

    private static final String SECRET = "test-secret-test-secret-test-secret-1234";
    private final UserPrincipal principal =
            new UserPrincipal(UUID.randomUUID(), "a@b.gr", "Μαρία Π.", Role.ACCOUNTANT, "hash", true, null);

    @Test
    void roundTripPreservesIdentityAndRole() {
        String token = serviceAt(T0, SECRET).issue(principal).token();
        var parsed = serviceAt(T0.plusSeconds(60), SECRET).parse(token);
        assertThat(parsed).isPresent();
        assertThat(parsed.get().id()).isEqualTo(principal.id());
        assertThat(parsed.get().role()).isEqualTo(Role.ACCOUNTANT);
        assertThat(parsed.get().fullName()).isEqualTo("Μαρία Π.");
    }

    @Test
    void expiredTokenIsRejected() {
        String token = serviceAt(T0, SECRET).issue(principal).token();
        assertThat(serviceAt(T0.plus(Duration.ofHours(2)), SECRET).parse(token)).isEmpty();
    }

    @Test
    void tokenSignedWithOtherKeyIsRejected() {
        String token = serviceAt(T0, "another-secret-another-secret-another-12").issue(principal).token();
        assertThat(serviceAt(T0, SECRET).parse(token)).isEmpty();
    }

    @Test
    void garbageIsRejected() {
        assertThat(serviceAt(T0, SECRET).parse("not.a.jwt")).isEmpty();
    }
}
