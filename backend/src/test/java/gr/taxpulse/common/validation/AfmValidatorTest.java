package gr.taxpulse.common.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class AfmValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"094014201", "997645901", "800000002", "123456783"})
    void acceptsValidAfm(String afm) {
        assertThat(AfmValidator.isValidAfm(afm)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"094014202", "123456789", "000000000", "12345678", "1234567890", "09401420A", "EL094014201"})
    void rejectsInvalidAfm(String afm) {
        assertThat(AfmValidator.isValidAfm(afm)).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void nullIsLeftToNotNullButEmptyIsInvalid(String afm) {
        assertThat(new AfmValidator().isValid(afm, null)).isEqualTo(afm == null);
    }
}
