package gr.taxpulse.portal.service;

import gr.taxpulse.ai.dto.ChatResponse;
import gr.taxpulse.ai.service.AiChatService;
import gr.taxpulse.client.entity.Client;
import gr.taxpulse.client.mapper.ClientMapper;
import gr.taxpulse.client.service.ClientObligationStatsPort;
import gr.taxpulse.client.service.ClientService;
import gr.taxpulse.common.dto.PageResponse;
import gr.taxpulse.document.dto.DocumentResponse;
import gr.taxpulse.document.service.DocumentService;
import gr.taxpulse.obligation.dto.ObligationSearchCriteria;
import gr.taxpulse.obligation.entity.ObligationStatus;
import gr.taxpulse.obligation.service.TaxObligationService;
import gr.taxpulse.portal.dto.PortalObligationResponse;
import gr.taxpulse.portal.dto.PortalProfileResponse;
import gr.taxpulse.security.CurrentUser;
import gr.taxpulse.user.entity.User;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Use-cases of the client portal. Every operation is scoped to the client bound to the caller's
 * account; no client id is ever taken from the request.
 */
@Service
public class PortalService {

    private final ClientService clientService;
    private final TaxObligationService obligationService;
    private final DocumentService documentService;
    private final AiChatService chatService;
    private final ClientObligationStatsPort statsPort;

    public PortalService(ClientService clientService,
                         TaxObligationService obligationService,
                         DocumentService documentService,
                         AiChatService chatService,
                         ObjectProvider<ClientObligationStatsPort> statsPort) {
        this.clientService = clientService;
        this.obligationService = obligationService;
        this.documentService = documentService;
        this.chatService = chatService;
        this.statsPort = statsPort.getIfAvailable(() -> ClientObligationStatsPort.NONE);
    }

    @Transactional(readOnly = true)
    public PortalProfileResponse profile() {
        UUID clientId = currentClientId();
        Client c = clientService.getEntity(clientId);
        var stats = statsPort.statsFor(List.of(clientId)).getOrDefault(clientId, ClientObligationStatsPort.Stats.EMPTY);
        User accountant = c.getAssignedAccountant();
        // Overdue items are counted separately; "next" means the next deadline still ahead.
        LocalDate nextDueDate = obligationService.search(
                        new ObligationSearchCriteria(clientId, List.copyOf(ObligationStatus.OPEN), null, null, null, null, null),
                        PageRequest.of(0, 1, Sort.by("dueDate")))
                .content().stream().findFirst().map(o -> o.dueDate()).orElse(null);
        return new PortalProfileResponse(c.getId(), c.getClientType(), c.getName(), c.getAfm(), c.getDoy(),
                c.getLegalForm(), c.getEmail(), c.getPhone() != null ? c.getPhone() : c.getMobile(),
                ClientMapper.toAddressDto(c.getAddress()),
                accountant == null ? null : new PortalProfileResponse.Accountant(accountant.getFullName(), accountant.getEmail()),
                stats.open(), stats.overdue(), nextDueDate);
    }

    public PageResponse<PortalObligationResponse> obligations(List<ObligationStatus> status, Pageable pageable) {
        var criteria = new ObligationSearchCriteria(currentClientId(), status, null, null, null, null, null);
        return obligationService.search(criteria, pageable).map(PortalObligationResponse::from);
    }

    public PageResponse<DocumentResponse> documents(Pageable pageable) {
        return documentService.listForClient(currentClientId(), pageable);
    }

    /** Uploads a document for the caller's client (the office is notified by ClientInteractionNotifier). */
    public DocumentResponse upload(UUID obligationId, MultipartFile file) {
        return documentService.upload(currentClientId(), obligationId, file);
    }

    public DocumentService.DownloadableDocument download(UUID documentId) {
        return documentService.downloadForClient(documentId, currentClientId());
    }

    public ChatResponse askDocuments(String question) {
        return chatService.askDocuments(question, currentClientId());
    }

    private static UUID currentClientId() {
        UUID clientId = CurrentUser.require().clientId();
        if (clientId == null) {
            throw new AccessDeniedException("Not a client portal account");
        }
        return clientId;
    }
}
