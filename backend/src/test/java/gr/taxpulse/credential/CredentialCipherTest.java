package gr.taxpulse.credential;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import gr.taxpulse.common.exception.ExternalServiceException;
import gr.taxpulse.config.TaxPulseProperties;
import gr.taxpulse.credential.service.CredentialCipher;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;

class CredentialCipherTest {

    private static CredentialCipher cipher(String key) {
        return new CredentialCipher(new TaxPulseProperties(
                new TaxPulseProperties.Security("x".repeat(32), Duration.ofHours(1), "taxpulse", key),
                new TaxPulseProperties.Cors(List.of("http://localhost")),
                new TaxPulseProperties.Reminders(true, List.of(1), "0 0 7 * * *", "Europe/Athens"),
                new TaxPulseProperties.Storage("/tmp"),
                new TaxPulseProperties.Bootstrap(null, null, null)));
    }

    private static String key(int fill) {
        byte[] raw = new byte[32];
        java.util.Arrays.fill(raw, (byte) fill);
        return Base64.getEncoder().encodeToString(raw);
    }

    @Test
    void roundTripsWithAFreshIvEachTime() {
        CredentialCipher c = cipher(key(7));
        String a = c.encrypt("Κωδικός!TAXIS 2026");
        String b = c.encrypt("Κωδικός!TAXIS 2026");
        assertThat(a).isNotEqualTo(b).doesNotContain("TAXIS");
        assertThat(c.decrypt(a)).isEqualTo("Κωδικός!TAXIS 2026");
    }

    @Test
    void wrongKeyOrTamperingFailsInsteadOfReturningGarbage() {
        String stored = cipher(key(7)).encrypt("secret");
        assertThatThrownBy(() -> cipher(key(8)).decrypt(stored)).isInstanceOf(ExternalServiceException.class);
        byte[] raw = Base64.getDecoder().decode(stored);
        raw[raw.length - 1] ^= 1;
        assertThatThrownBy(() -> cipher(key(7)).decrypt(Base64.getEncoder().encodeToString(raw)))
                .isInstanceOf(ExternalServiceException.class);
    }

    @Test
    void featureIsOffWithoutAKeyAndRejectsShortKeys() {
        assertThatThrownBy(() -> cipher(null).encrypt("x")).isInstanceOf(ExternalServiceException.class);
        assertThatThrownBy(() -> cipher(Base64.getEncoder().encodeToString(new byte[16])))
                .isInstanceOf(IllegalStateException.class);
    }
}
