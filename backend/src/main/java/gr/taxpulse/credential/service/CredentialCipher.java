package gr.taxpulse.credential.service;

import gr.taxpulse.common.exception.ExternalServiceException;
import gr.taxpulse.config.TaxPulseProperties;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * AES-256-GCM for stored credentials: a fresh 96-bit IV per value and a 128-bit tag, so a tampered
 * or wrongly-keyed value fails to decrypt instead of yielding garbage. Stored as base64(iv || ct).
 */
@Component
public class CredentialCipher {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public CredentialCipher(TaxPulseProperties properties) {
        String encoded = properties.security().credentialsKey();
        if (!StringUtils.hasText(encoded)) {
            this.key = null;
            return;
        }
        byte[] raw = Base64.getDecoder().decode(encoded.trim());
        if (raw.length != 32) {
            throw new IllegalStateException("CREDENTIALS_ENCRYPTION_KEY must be base64 of exactly 32 bytes (openssl rand -base64 32)");
        }
        this.key = new SecretKeySpec(raw, "AES");
    }

    public String encrypt(String plain) {
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, requireKey(), new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + ct.length).put(iv).put(ct).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Credential encryption failed", e);
        }
    }

    public String decrypt(String stored) {
        try {
            byte[] all = Base64.getDecoder().decode(stored);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, requireKey(), new GCMParameterSpec(TAG_BITS, all, 0, IV_BYTES));
            return new String(cipher.doFinal(all, IV_BYTES, all.length - IV_BYTES), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            // Wrong / rotated key or tampered data: never return a partial value.
            throw new ExternalServiceException("Δεν ήταν δυνατή η αποκρυπτογράφηση του κωδικού (έλεγχος κλειδιού κρυπτογράφησης)");
        }
    }

    private SecretKey requireKey() {
        if (key == null) {
            throw new ExternalServiceException("Η αποθήκευση κωδικών δεν έχει ρυθμιστεί (CREDENTIALS_ENCRYPTION_KEY)");
        }
        return key;
    }
}
