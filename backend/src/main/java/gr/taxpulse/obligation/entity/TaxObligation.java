package gr.taxpulse.obligation.entity;

import gr.taxpulse.client.entity.Client;
import gr.taxpulse.common.entity.BaseEntity;
import gr.taxpulse.common.exception.BusinessRuleException;
import gr.taxpulse.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A dated obligation of a client (e.g. "ΦΠΑ Γ' τριμήνου 2026").
 * Status changes go through domain methods so the state machine cannot be bypassed.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tax_obligations")
public class TaxObligation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Enumerated(EnumType.STRING)
    @Column(name = "obligation_type", nullable = false, length = 30)
    private ObligationType obligationType;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "period_start")
    private LocalDate periodStart;

    @Column(name = "period_end")
    private LocalDate periodEnd;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Setter(AccessLevel.NONE)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ObligationStatus status = ObligationStatus.PENDING_DOCS;

    @Column(precision = 14, scale = 2)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;

    @Setter(AccessLevel.NONE)
    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "submission_ref", length = 100)
    private String submissionRef;

    @Column(columnDefinition = "text")
    private String notes;

    /**
     * User-requested transition, validated against the state machine.
     *
     * @param today today's date in the office time zone; a re-opened obligation past its due date
     *              becomes {@code OVERDUE} immediately
     */
    public void transitionTo(ObligationStatus target, Instant now, LocalDate today) {
        if (target == status) {
            return;
        }
        if (!status.canTransitionTo(target)) {
            throw new BusinessRuleException("Μη επιτρεπτή αλλαγή κατάστασης: %s -> %s".formatted(status, target));
        }
        if (target == ObligationStatus.SUBMITTED) {
            submittedAt = now;
        } else {
            submittedAt = null;
            submissionRef = null;
        }
        status = (target.isOpen() && dueDate.isBefore(today)) ? ObligationStatus.OVERDUE : target;
    }

    /** System transition performed by the deadline scheduler. */
    public boolean markOverdueIfPastDue(LocalDate today) {
        if (status.isOpen() && dueDate.isBefore(today)) {
            status = ObligationStatus.OVERDUE;
            return true;
        }
        return false;
    }

    /**
     * Applies a new due date. A deadline extension (παράταση) on an overdue obligation re-opens it,
     * while moving an open one into the past makes it overdue.
     */
    public void reschedule(LocalDate newDueDate, LocalDate today) {
        this.dueDate = newDueDate;
        if (status == ObligationStatus.OVERDUE && !newDueDate.isBefore(today)) {
            status = ObligationStatus.IN_PROGRESS;
        } else {
            markOverdueIfPastDue(today);
        }
    }
}
