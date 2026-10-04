package gr.taxpulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point of the TaxPulse AI backend.
 *
 * <p>The code base is organised <em>package-by-feature</em> ({@code client}, {@code obligation},
 * {@code ai}, ...). Inside every feature the classic layers live in sub-packages
 * ({@code controller -> service -> repository -> entity}, plus {@code dto} and {@code mapper}),
 * which keeps related code together while preserving a strict dependency direction.</p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class TaxPulseApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaxPulseApplication.class, args);
    }
}
