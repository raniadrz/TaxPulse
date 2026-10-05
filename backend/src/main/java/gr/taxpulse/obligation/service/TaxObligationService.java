package gr.taxpulse.obligation.service;

import gr.taxpulse.client.service.ClientService;
import gr.taxpulse.common.dto.PageResponse;
import gr.taxpulse.common.exception.ResourceNotFoundException;
import gr.taxpulse.obligation.dto.ObligationRequest;
import gr.taxpulse.obligation.dto.ObligationResponse;
import gr.taxpulse.obligation.dto.ObligationSearchCriteria;
import gr.taxpulse.obligation.dto.StatusUpdateRequest;
import gr.taxpulse.obligation.entity.ObligationStatus;
import gr.taxpulse.obligation.entity.TaxObligation;
import gr.taxpulse.obligation.event.ObligationStatusChangedEvent;
import gr.taxpulse.obligation.mapper.ObligationMapper;
import gr.taxpulse.obligation.repository.ObligationSpecifications;
import gr.taxpulse.obligation.repository.TaxObligationRepository;
import gr.taxpulse.security.CurrentUser;
import gr.taxpulse.user.service.UserService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Use-cases of the tax calendar: CRUD, workflow transitions and filtered listing. */
@Slf4j
@Service
@Transactional(readOnly = true)
public class TaxObligationService {

    private final TaxObligationRepository repository;
    private final ObligationMapper mapper;
    private final ClientService clientService;
    private final UserService userService;
    private final OfficeClock officeClock;
    private final ApplicationEventPublisher events;
    private final ObligationActivityPort activityPort;

    public TaxObligationService(TaxObligationRepository repository,
                                ObligationMapper mapper,
                                ClientService clientService,
                                UserService userService,
                                OfficeClock officeClock,
                                ApplicationEventPublisher events,
                                ObjectProvider<ObligationActivityPort> activityPort) {
        this.repository = repository;
        this.mapper = mapper;
        this.clientService = clientService;
        this.userService = userService;
        this.officeClock = officeClock;
        this.events = events;
        this.activityPort = activityPort.getIfAvailable(() -> ObligationActivityPort.NONE);
    }

    public PageResponse<ObligationResponse> search(ObligationSearchCriteria c, Pageable pageable) {
        UUID assignee = Boolean.TRUE.equals(c.mine()) ? CurrentUser.require().id() : c.assignedToId();
        Specification<TaxObligation> spec = Specification.allOf(
                ObligationSpecifications.fetchAssociations(),
                ObligationSpecifications.forClient(c.clientId()),
                ObligationSpecifications.hasStatusIn(c.status()),
                ObligationSpecifications.hasType(c.type()),
                ObligationSpecifications.assignedTo(assignee),
                ObligationSpecifications.dueFrom(c.dueFrom()),
                ObligationSpecifications.dueTo(c.dueTo()));
        LocalDate today = officeClock.today();
        Page<TaxObligation> page = repository.findAll(spec, pageable);
        // One batched lookup for the whole page instead of one per row.
        Map<UUID, ObligationActivityPort.Activity> activity =
                activityPort.activityFor(page.getContent().stream().map(TaxObligation::getId).toList());
        return PageResponse.from(page, o -> mapper.toResponse(o, today,
                activity.getOrDefault(o.getId(), ObligationActivityPort.Activity.EMPTY)));
    }

    public ObligationResponse findById(UUID id) {
        return mapper.toResponse(getDetailed(id), officeClock.today(),
                activityPort.activityFor(List.of(id)).getOrDefault(id, ObligationActivityPort.Activity.EMPTY));
    }

    @Transactional
    public ObligationResponse create(ObligationRequest request) {
        TaxObligation obligation = new TaxObligation();
        obligation.setClient(clientService.getEntity(request.clientId()));
        mapper.apply(request, obligation);
        obligation.reschedule(request.dueDate(), officeClock.today());
        obligation.setAssignedTo(resolveAssignee(request.assignedToId(), obligation));
        TaxObligation saved = repository.save(obligation);
        log.info("Obligation created id={} client={} type={} due={}",
                saved.getId(), saved.getClient().getId(), saved.getObligationType(), saved.getDueDate());
        return mapper.toResponse(saved, officeClock.today());
    }

    @Transactional
    public ObligationResponse update(UUID id, ObligationRequest request) {
        TaxObligation obligation = getDetailed(id);
        if (!obligation.getClient().getId().equals(request.clientId())) {
            obligation.setClient(clientService.getEntity(request.clientId()));
        }
        mapper.apply(request, obligation);
        obligation.reschedule(request.dueDate(), officeClock.today());
        obligation.setAssignedTo(resolveAssignee(request.assignedToId(), obligation));
        repository.flush();
        return mapper.toResponse(obligation, officeClock.today());
    }

    @Transactional
    public ObligationResponse changeStatus(UUID id, StatusUpdateRequest request) {
        TaxObligation obligation = getDetailed(id);
        ObligationStatus previous = obligation.getStatus();
        obligation.transitionTo(request.status(), officeClock.now(), officeClock.today());
        if (obligation.getStatus() == ObligationStatus.SUBMITTED && StringUtils.hasText(request.submissionRef())) {
            obligation.setSubmissionRef(request.submissionRef().trim());
        }
        repository.flush();
        log.info("Obligation {} status {} -> {} by {}", id, previous, obligation.getStatus(),
                CurrentUser.get().map(p -> p.email()).orElse("system"));
        if (obligation.getStatus() != previous) {
            events.publishEvent(new ObligationStatusChangedEvent(obligation.getId(), obligation.getClient().getId(),
                    obligation.getTitle(), obligation.getObligationType().label(), previous, obligation.getStatus(),
                    obligation.getSubmissionRef()));
        }
        return mapper.toResponse(obligation, officeClock.today());
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Obligation", id)));
    }

    /** Loads the aggregate with client + assignee. Exposed for the AI module (reminder e-mails). */
    public TaxObligation getDetailed(UUID id) {
        return repository.findDetailedById(id).orElseThrow(() -> new ResourceNotFoundException("Obligation", id));
    }

    /** Explicit assignee wins; otherwise default to the client's accountant. */
    private gr.taxpulse.user.entity.User resolveAssignee(UUID assigneeId, TaxObligation obligation) {
        if (assigneeId != null) {
            return userService.getStaffEntity(assigneeId);
        }
        return obligation.getClient().getAssignedAccountant();
    }
}
