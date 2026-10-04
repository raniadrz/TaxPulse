package gr.taxpulse.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Bean Validation constraint for a check-digit valid Greek ΑΦΜ. */
@Documented
@Constraint(validatedBy = AfmValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidAfm {

    String message() default "Μη έγκυρος ΑΦΜ";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
