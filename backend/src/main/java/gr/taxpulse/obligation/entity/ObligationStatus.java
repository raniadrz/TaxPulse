package gr.taxpulse.obligation.entity;

import java.util.EnumSet;
import java.util.Set;

/**
 * Workflow state of an obligation, encoded as a small state machine.
 *
 * <pre>
 *   PENDING_DOCS &lt;--&gt; IN_PROGRESS --&gt; SUBMITTED
 *        |                 |               |
 *        +------&gt; (deadline passes) &lt;-----+ (re-open past due)
 *                        OVERDUE --&gt; SUBMITTED (late filing)
 * </pre>
 *
 * {@code OVERDUE} is set only by the system (deadline scheduler), never manually.
 */
public enum ObligationStatus {
    PENDING_DOCS,
    IN_PROGRESS,
    SUBMITTED,
    OVERDUE;

    /** Statuses that still require work (used by reminders and counters). */
    public static final Set<ObligationStatus> OPEN = EnumSet.of(PENDING_DOCS, IN_PROGRESS);

    /** Manual transitions a user may request. */
    public boolean canTransitionTo(ObligationStatus target) {
        return switch (this) {
            case PENDING_DOCS -> target == IN_PROGRESS || target == SUBMITTED;
            case IN_PROGRESS -> target == PENDING_DOCS || target == SUBMITTED;
            case OVERDUE -> target == SUBMITTED;
            case SUBMITTED -> target == IN_PROGRESS; // re-open, e.g. for an amended (τροποποιητική) filing
        };
    }

    public boolean isOpen() {
        return OPEN.contains(this);
    }
}
