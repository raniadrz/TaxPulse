package gr.taxpulse.config;

import gr.taxpulse.user.entity.Role;
import gr.taxpulse.user.entity.User;
import gr.taxpulse.user.repository.UserRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Creates the first administrator from configuration when the users table is empty.
 * Credentials come from the environment, never from a migration, so no password hash is committed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TaxPulseProperties properties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }
        TaxPulseProperties.Bootstrap cfg = properties.bootstrap();
        if (!StringUtils.hasText(cfg.adminEmail()) || !StringUtils.hasText(cfg.adminPassword())) {
            log.warn("No users exist and TAXPULSE_ADMIN_EMAIL / TAXPULSE_ADMIN_PASSWORD are not set: "
                    + "nobody will be able to log in.");
            return;
        }
        User admin = new User();
        admin.setEmail(cfg.adminEmail().trim().toLowerCase(Locale.ROOT));
        admin.setFullName(StringUtils.hasText(cfg.adminFullName()) ? cfg.adminFullName() : "Administrator");
        admin.setRole(Role.ADMIN);
        admin.setPasswordHash(passwordEncoder.encode(cfg.adminPassword()));
        userRepository.save(admin);
        log.info("Bootstrap administrator created: {}", admin.getEmail());
    }
}
