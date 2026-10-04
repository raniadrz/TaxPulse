package gr.taxpulse.client.service;

import gr.taxpulse.client.dto.ClientRequest;
import gr.taxpulse.client.dto.ClientResponse;
import gr.taxpulse.client.dto.ClientSearchCriteria;
import gr.taxpulse.client.dto.ClientSummaryResponse;
import gr.taxpulse.client.entity.Client;
import gr.taxpulse.client.mapper.ClientMapper;
import gr.taxpulse.client.repository.ClientRepository;
import gr.taxpulse.client.repository.ClientSpecifications;
import gr.taxpulse.common.dto.PageResponse;
import gr.taxpulse.common.exception.ConflictException;
import gr.taxpulse.common.exception.ResourceNotFoundException;
import gr.taxpulse.user.service.UserService;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CRM use-cases: create, update, search and remove clients. */
@Slf4j
@Service
@Transactional(readOnly = true)
public class ClientService {

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;
    private final UserService userService;
    private final ClientObligationStatsPort statsPort;

    public ClientService(ClientRepository clientRepository,
                         ClientMapper clientMapper,
                         UserService userService,
                         ObjectProvider<ClientObligationStatsPort> statsPort) {
        this.clientRepository = clientRepository;
        this.clientMapper = clientMapper;
        this.userService = userService;
        this.statsPort = statsPort.getIfAvailable(() -> ClientObligationStatsPort.NONE);
    }

    public PageResponse<ClientSummaryResponse> search(ClientSearchCriteria criteria, Pageable pageable) {
        Specification<Client> spec = Specification.allOf(
                ClientSpecifications.matchesQuery(criteria.q()),
                ClientSpecifications.hasType(criteria.type()),
                ClientSpecifications.hasBookCategory(criteria.bookCategory()),
                ClientSpecifications.isActive(criteria.active()),
                ClientSpecifications.assignedTo(criteria.assignedAccountantId()));

        Page<Client> page = clientRepository.findAll(spec, pageable);
        // One aggregate query for the whole page instead of one per row.
        Map<UUID, ClientObligationStatsPort.Stats> stats =
                statsPort.statsFor(page.getContent().stream().map(Client::getId).toList());
        return PageResponse.from(page, c -> clientMapper.toSummary(c, stats.get(c.getId())));
    }

    public ClientResponse findById(UUID id) {
        return clientMapper.toResponse(getDetailed(id));
    }

    public ClientResponse findByAfm(String afm) {
        return clientRepository.findByAfm(afm)
                .map(clientMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Client with ΑΦΜ", afm));
    }

    /** Loads the entity or fails with 404. Exposed for other modules that reference clients. */
    public Client getEntity(UUID id) {
        return clientRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Client", id));
    }

    @Transactional
    public ClientResponse create(ClientRequest request) {
        String afm = request.afm().trim();
        if (clientRepository.existsByAfm(afm)) {
            throw new ConflictException("Υπάρχει ήδη πελάτης με ΑΦΜ " + afm);
        }
        Client client = new Client();
        clientMapper.apply(request, client);
        assignAccountant(client, request.assignedAccountantId());
        Client saved = clientRepository.save(client);
        log.info("Client created id={} afm={}", saved.getId(), saved.getAfm());
        return clientMapper.toResponse(saved);
    }

    @Transactional
    public ClientResponse update(UUID id, ClientRequest request) {
        Client client = getDetailed(id);
        String afm = request.afm().trim();
        if (clientRepository.existsByAfmAndIdNot(afm, id)) {
            throw new ConflictException("Υπάρχει ήδη πελάτης με ΑΦΜ " + afm);
        }
        demotePrimaryKadIfChanging(client, request);
        clientMapper.apply(request, client);
        assignAccountant(client, request.assignedAccountantId());
        // Flush so the response carries the incremented @Version for the next optimistic update.
        clientRepository.flush();
        return clientMapper.toResponse(client);
    }

    /** Hard delete (cascades to obligations/documents). Prefer deactivation for routine use. */
    @Transactional
    public void delete(UUID id) {
        Client client = getEntity(id);
        clientRepository.delete(client);
        log.info("Client deleted id={} afm={}", id, client.getAfm());
    }

    private Client getDetailed(UUID id) {
        return clientRepository.findDetailedById(id).orElseThrow(() -> new ResourceNotFoundException("Client", id));
    }

    /**
     * The partial unique index {@code ux_client_kad_primary} allows one primary ΚΑΔ per client and is
     * checked per statement, so moving the flag between two rows must clear the old one first.
     */
    private void demotePrimaryKadIfChanging(Client client, ClientRequest request) {
        String current = client.primaryActivityCode().map(k -> k.getCode()).orElse(null);
        String requested = request.activityCodesOrEmpty().stream()
                .filter(k -> k.primary())
                .map(k -> ClientMapper.normalizeKad(k.code()))
                .findFirst()
                .orElse(request.activityCodesOrEmpty().isEmpty() ? null
                        : ClientMapper.normalizeKad(request.activityCodesOrEmpty().get(0).code()));
        if (current != null && !current.equals(requested)) {
            client.getActivityCodes().forEach(k -> k.setPrimary(false));
            clientRepository.flush();
        }
    }

    private void assignAccountant(Client client, UUID accountantId) {
        client.setAssignedAccountant(accountantId == null ? null : userService.getEntity(accountantId));
    }
}
