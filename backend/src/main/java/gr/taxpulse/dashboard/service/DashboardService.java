package gr.taxpulse.dashboard.service;

import gr.taxpulse.client.repository.ClientRepository;
import gr.taxpulse.dashboard.dto.DashboardStatsResponse;
import gr.taxpulse.obligation.entity.ObligationStatus;
import gr.taxpulse.obligation.mapper.ObligationMapper;
import gr.taxpulse.obligation.repository.TaxObligationRepository;
import gr.taxpulse.obligation.service.OfficeClock;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only aggregation for the dashboard (all counters are index-backed COUNT queries). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private static final int UPCOMING_LIMIT = 8;

    private final ClientRepository clientRepository;
    private final TaxObligationRepository obligationRepository;
    private final ObligationMapper obligationMapper;
    private final OfficeClock officeClock;

    public DashboardStatsResponse stats() {
        LocalDate today = officeClock.today();
        var startOfMonth = today.withDayOfMonth(1).atStartOfDay(officeClock.zone()).toInstant();

        Map<ObligationStatus, Long> byStatus = new EnumMap<>(ObligationStatus.class);
        for (ObligationStatus status : ObligationStatus.values()) {
            byStatus.put(status, obligationRepository.countByStatus(status));
        }

        var actionable = EnumSet.of(ObligationStatus.PENDING_DOCS, ObligationStatus.IN_PROGRESS, ObligationStatus.OVERDUE);
        var upcoming = obligationRepository
                .findByStatusInOrderByDueDateAsc(actionable, PageRequest.of(0, UPCOMING_LIMIT))
                .stream().map(o -> obligationMapper.toResponse(o, today)).toList();

        return new DashboardStatsResponse(
                clientRepository.countByActiveTrue(),
                obligationRepository.countByStatusIn(ObligationStatus.OPEN),
                byStatus.get(ObligationStatus.OVERDUE),
                obligationRepository.countByStatusInAndDueDateBetween(ObligationStatus.OPEN, today, today.plusDays(7)),
                obligationRepository.countByStatusAndSubmittedAtGreaterThanEqual(ObligationStatus.SUBMITTED, startOfMonth),
                byStatus,
                upcoming);
    }
}
