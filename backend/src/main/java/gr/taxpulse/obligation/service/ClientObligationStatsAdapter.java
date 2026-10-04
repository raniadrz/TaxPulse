package gr.taxpulse.obligation.service;

import gr.taxpulse.client.service.ClientObligationStatsPort;
import gr.taxpulse.obligation.entity.ObligationStatus;
import gr.taxpulse.obligation.repository.TaxObligationRepository;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Obligation-module implementation of the CRM's {@link ClientObligationStatsPort}. */
@Component
@RequiredArgsConstructor
public class ClientObligationStatsAdapter implements ClientObligationStatsPort {

    private final TaxObligationRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, Stats> statsFor(Collection<UUID> clientIds) {
        if (clientIds.isEmpty()) {
            return Map.of();
        }
        return repository.aggregateByClient(clientIds, ObligationStatus.OPEN).stream()
                .collect(Collectors.toMap(
                        TaxObligationRepository.ClientObligationStatsView::getClientId,
                        v -> new Stats(nz(v.getOpenCount()), nz(v.getOverdueCount()), v.getNextDueDate())));
    }

    private static long nz(Long value) {
        return value == null ? 0 : value;
    }
}
