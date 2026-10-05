package gr.taxpulse.security;

import gr.taxpulse.config.TaxPulseProperties;
import gr.taxpulse.user.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Issues and validates stateless HS256 access tokens.
 *
 * <p>Claims: {@code sub}=user id, {@code email}, {@code name}, {@code role}. Role is embedded so
 * request authorization needs no DB round-trip; tokens are short-lived to bound staleness.</p>
 */
@Slf4j
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final TaxPulseProperties.Security config;
    private final Clock clock;

    public JwtService(TaxPulseProperties properties, Clock clock) {
        this.config = properties.security();
        this.signingKey = Keys.hmacShaKeyFor(config.jwtSecret().getBytes(StandardCharsets.UTF_8));
        this.clock = clock;
    }

    public IssuedToken issue(UserPrincipal principal) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(config.jwtExpiration());
        String token = Jwts.builder()
                .issuer(config.jwtIssuer())
                .subject(principal.id().toString())
                .claim("email", principal.email())
                .claim("name", principal.fullName())
                .claim("role", principal.role().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
        return new IssuedToken(token, expiresAt);
    }

    /**
     * Parses and verifies signature, issuer and expiry.
     *
     * @return the principal, or empty when the token is invalid / expired
     */
    public Optional<UserPrincipal> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(config.jwtIssuer())
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new UserPrincipal(
                    UUID.fromString(claims.getSubject()),
                    claims.get("email", String.class),
                    claims.get("name", String.class),
                    Role.valueOf(claims.get("role", String.class)),
                    null,
                    true,
                    null)); // the client binding is loaded from the database, see JwtAuthenticationFilter
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Rejected JWT: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public record IssuedToken(String token, Instant expiresAt) {
    }
}
