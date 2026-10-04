package gr.taxpulse.obligation.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import gr.taxpulse.common.exception.BusinessRuleException;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TaxObligationTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 4);
    private static final Instant NOW = Instant.parse("2026-10-04T08:00:00Z");

    private static TaxObligation dueOn(LocalDate due) {
        TaxObligation o = new TaxObligation();
        o.reschedule(due, TODAY);
        return o;
    }

    @Test
    void newObligationStartsPendingDocs() {
        assertThat(dueOn(TODAY.plusDays(10)).getStatus()).isEqualTo(ObligationStatus.PENDING_DOCS);
    }

    @Test
    void submittingRecordsTimestamp() {
        TaxObligation o = dueOn(TODAY.plusDays(10));
        o.transitionTo(ObligationStatus.IN_PROGRESS, NOW, TODAY);
        o.transitionTo(ObligationStatus.SUBMITTED, NOW, TODAY);
        assertThat(o.getStatus()).isEqualTo(ObligationStatus.SUBMITTED);
        assertThat(o.getSubmittedAt()).isEqualTo(NOW);
    }

    @Test
    void overdueCannotBeSetManually() {
        TaxObligation o = dueOn(TODAY.plusDays(10));
        assertThatThrownBy(() -> o.transitionTo(ObligationStatus.OVERDUE, NOW, TODAY))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void schedulerMarksPastDueOpenObligationsOverdue() {
        TaxObligation o = dueOn(TODAY.plusDays(1));
        assertThat(o.markOverdueIfPastDue(TODAY.plusDays(2))).isTrue();
        assertThat(o.getStatus()).isEqualTo(ObligationStatus.OVERDUE);
    }

    @Test
    void submittedObligationsNeverBecomeOverdue() {
        TaxObligation o = dueOn(TODAY.plusDays(1));
        o.transitionTo(ObligationStatus.SUBMITTED, NOW, TODAY);
        assertThat(o.markOverdueIfPastDue(TODAY.plusDays(30))).isFalse();
    }

    @Test
    void overdueObligationCanOnlyBeSubmittedLate() {
        TaxObligation o = dueOn(TODAY.minusDays(1));
        assertThat(o.getStatus()).isEqualTo(ObligationStatus.OVERDUE);
        assertThatThrownBy(() -> o.transitionTo(ObligationStatus.IN_PROGRESS, NOW, TODAY))
                .isInstanceOf(BusinessRuleException.class);
        o.transitionTo(ObligationStatus.SUBMITTED, NOW, TODAY);
        assertThat(o.getStatus()).isEqualTo(ObligationStatus.SUBMITTED);
    }

    @Test
    void deadlineExtensionReopensOverdueObligation() {
        TaxObligation o = dueOn(TODAY.minusDays(1));
        o.reschedule(TODAY.plusDays(15), TODAY);
        assertThat(o.getStatus()).isEqualTo(ObligationStatus.IN_PROGRESS);
    }

    @Test
    void reopeningPastDueSubmissionMakesItOverdue() {
        TaxObligation o = dueOn(TODAY.plusDays(1));
        o.transitionTo(ObligationStatus.SUBMITTED, NOW, TODAY);
        o.transitionTo(ObligationStatus.IN_PROGRESS, NOW, TODAY.plusDays(5));
        assertThat(o.getStatus()).isEqualTo(ObligationStatus.OVERDUE);
        assertThat(o.getSubmittedAt()).isNull();
    }
}
