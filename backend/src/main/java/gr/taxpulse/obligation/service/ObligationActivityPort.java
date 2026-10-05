package gr.taxpulse.obligation.service;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Port through which the tax calendar shows client interaction on each obligation (documents sent
 * from the portal, conversation size) without depending on the modules that own that data.
 */
public interface ObligationActivityPort {

    /** Null-object used when no implementation is wired (e.g. slice tests). */
    ObligationActivityPort NONE = obligationIds -> Map.of();

    Map<UUID, Activity> activityFor(Collection<UUID> obligationIds);

    /**
     * @param clientDocuments documents attached to the obligation from the client portal
     * @param messages        messages in the obligation's conversation
     */
    record Activity(long clientDocuments, long messages) {
        public static final Activity EMPTY = new Activity(0, 0);
    }
}
