package gr.taxpulse.dashboard.dto;

import gr.taxpulse.obligation.dto.ObligationResponse;
import gr.taxpulse.obligation.entity.ObligationStatus;
import java.util.List;
import java.util.Map;

/** KPI cards + "next deadlines" widget of the dashboard, served in one round-trip. */
public record DashboardStatsResponse(
        long activeClients,
        long openObligations,
        long overdueObligations,
        long dueWithin7Days,
        long submittedThisMonth,
        Map<ObligationStatus, Long> obligationsByStatus,
        List<ObligationResponse> upcomingDeadlines) {
}
