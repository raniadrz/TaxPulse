package gr.taxpulse.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Enables {@code @CreatedDate} / {@code @LastModifiedDate} auditing on entities. */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
