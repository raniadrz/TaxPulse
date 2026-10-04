package gr.taxpulse.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Strongly typed application configuration bound from the {@code taxpulse.*} namespace.
 * Validated at start-up so that a misconfigured deployment fails fast instead of at runtime.
 */
@Validated
@ConfigurationProperties(prefix = "taxpulse")
public record TaxPulseProperties(
        @Valid @NotNull Security security,
        @Valid @NotNull Cors cors,
        @Valid @NotNull Reminders reminders,
        @Valid @NotNull Storage storage,
        @Valid @NotNull Bootstrap bootstrap) {

    /** JWT settings. The secret must be at least 256 bits for HS256. */
    public record Security(
            @NotBlank @Size(min = 32, message = "JWT secret must be at least 32 characters") String jwtSecret,
            @NotNull Duration jwtExpiration,
            @NotBlank String jwtIssuer) {
    }

    public record Cors(@NotEmpty List<String> allowedOrigins) {
    }

    /**
     * Deadline reminder configuration.
     *
     * @param daysBefore days before the due date on which a reminder is generated (e.g. 7, 3, 1)
     * @param cron       cron expression of the reminder job
     * @param zone       time zone used to evaluate "today" (Europe/Athens for Greek deadlines)
     */
    public record Reminders(
            boolean enabled,
            @NotEmpty List<Integer> daysBefore,
            @NotBlank String cron,
            @NotBlank String zone) {
    }

    /** Local file storage root for uploaded documents. */
    public record Storage(@NotBlank String rootPath) {
    }

    /** Initial administrator created on an empty database. */
    public record Bootstrap(String adminEmail, String adminPassword, String adminFullName) {
    }
}
