package gr.taxpulse.message.service;

import gr.taxpulse.common.exception.ResourceNotFoundException;
import gr.taxpulse.message.dto.MessageResponse;
import gr.taxpulse.message.entity.ObligationMessage;
import gr.taxpulse.message.repository.ObligationMessageRepository;
import gr.taxpulse.obligation.entity.TaxObligation;
import gr.taxpulse.obligation.service.TaxObligationService;
import gr.taxpulse.portal.service.ClientInteractionNotifier;
import gr.taxpulse.security.CurrentUser;
import gr.taxpulse.security.UserPrincipal;
import gr.taxpulse.user.entity.Role;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The accountant/client conversation of an obligation. Staff can open any thread; a client portal
 * account only the threads of its own client's obligations (others are reported as missing).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ObligationMessageService {

    private final ObligationMessageRepository repository;
    private final TaxObligationService obligationService;
    private final ClientInteractionNotifier notifier;
    private final Clock clock;

    public List<MessageResponse> list(UUID obligationId) {
        accessibleObligation(obligationId);
        UUID me = CurrentUser.require().id();
        return repository.findByObligationIdOrderByCreatedAtAsc(obligationId).stream()
                .map(m -> toResponse(m, me))
                .toList();
    }

    @Transactional
    public MessageResponse post(UUID obligationId, String body) {
        TaxObligation obligation = accessibleObligation(obligationId);
        UserPrincipal author = CurrentUser.require();
        ObligationMessage message = new ObligationMessage();
        message.setId(UUID.randomUUID());
        message.setObligationId(obligationId);
        message.setAuthorId(author.id());
        message.setAuthorName(author.fullName());
        message.setFromClient(author.role() == Role.CLIENT);
        message.setBody(body.trim());
        message.setCreatedAt(clock.instant());
        repository.save(message);
        notifier.messagePosted(obligation, message.getId(), author, message.getBody());
        return toResponse(message, author.id());
    }

    /** 404 when the obligation does not exist or, for a portal account, belongs to another client. */
    private TaxObligation accessibleObligation(UUID obligationId) {
        TaxObligation obligation = obligationService.getDetailed(obligationId);
        UserPrincipal caller = CurrentUser.require();
        if (caller.role() == Role.CLIENT && !obligation.getClient().getId().equals(caller.clientId())) {
            throw new ResourceNotFoundException("Obligation", obligationId);
        }
        return obligation;
    }

    private static MessageResponse toResponse(ObligationMessage m, UUID viewerId) {
        return new MessageResponse(m.getId(), m.getAuthorName(), m.isFromClient(), viewerId.equals(m.getAuthorId()),
                m.getBody(), m.getCreatedAt());
    }
}
