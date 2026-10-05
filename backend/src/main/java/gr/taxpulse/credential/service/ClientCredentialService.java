package gr.taxpulse.credential.service;

import gr.taxpulse.client.service.ClientService;
import gr.taxpulse.common.exception.BusinessRuleException;
import gr.taxpulse.common.exception.ConflictException;
import gr.taxpulse.common.exception.ResourceNotFoundException;
import gr.taxpulse.credential.dto.CredentialLogEntry;
import gr.taxpulse.credential.dto.CredentialRequest;
import gr.taxpulse.credential.dto.CredentialResponse;
import gr.taxpulse.credential.dto.CredentialSecretResponse;
import gr.taxpulse.credential.entity.ClientCredential;
import gr.taxpulse.credential.entity.CredentialAccessLog;
import gr.taxpulse.credential.entity.CredentialKind;
import gr.taxpulse.credential.repository.ClientCredentialRepository;
import gr.taxpulse.credential.repository.CredentialAccessLogRepository;
import gr.taxpulse.portal.service.ClientInteractionNotifier;
import gr.taxpulse.security.CurrentUser;
import gr.taxpulse.security.UserPrincipal;
import gr.taxpulse.user.entity.Role;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Client logins for public services, editable by the client (portal) and by the office.
 * Every reveal and change is audited, and each side is notified of the other's changes.
 * Passwords are never logged.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientCredentialService {

    private static final int LOG_LIMIT = 50;

    private final ClientCredentialRepository repository;
    private final CredentialAccessLogRepository logRepository;
    private final CredentialCipher cipher;
    private final ClientService clientService;
    private final ClientInteractionNotifier notifier;
    private final Clock clock;

    public List<CredentialResponse> list(UUID clientId) {
        checkAccess(clientId);
        return repository.findByClientIdOrderByKindAscLabelAsc(clientId).stream().map(ClientCredentialService::toResponse).toList();
    }

    /** Returns the password in clear and records who saw it. */
    @Transactional
    public CredentialSecretResponse reveal(UUID clientId, UUID credentialId) {
        checkAccess(clientId);
        ClientCredential credential = get(clientId, credentialId);
        String password = cipher.decrypt(credential.getPasswordEncrypted());
        audit(credential, CredentialAccessLog.Action.VIEW);
        return new CredentialSecretResponse(password);
    }

    @Transactional
    public CredentialResponse create(UUID clientId, CredentialRequest request) {
        checkAccess(clientId);
        clientService.getEntity(clientId);
        if (!StringUtils.hasText(request.password())) {
            throw new BusinessRuleException("Ο κωδικός είναι υποχρεωτικός");
        }
        if (request.kind() != CredentialKind.OTHER && repository.existsByClientIdAndKind(clientId, request.kind())) {
            throw new ConflictException("Υπάρχουν ήδη κωδικοί %s για τον πελάτη".formatted(request.kind().label()));
        }
        ClientCredential credential = new ClientCredential();
        credential.setClientId(clientId);
        apply(credential, request);
        repository.save(credential);
        audit(credential, CredentialAccessLog.Action.CREATE);
        notifier.credentialsChanged(clientId, CurrentUser.require(), credential.getKind().label(), "προστέθηκαν");
        return toResponse(credential);
    }

    @Transactional
    public CredentialResponse update(UUID clientId, UUID credentialId, CredentialRequest request) {
        checkAccess(clientId);
        ClientCredential credential = get(clientId, credentialId);
        if (request.kind() != CredentialKind.OTHER
                && repository.existsByClientIdAndKindAndIdNot(clientId, request.kind(), credentialId)) {
            throw new ConflictException("Υπάρχουν ήδη κωδικοί %s για τον πελάτη".formatted(request.kind().label()));
        }
        apply(credential, request);
        repository.flush();
        audit(credential, CredentialAccessLog.Action.UPDATE);
        notifier.credentialsChanged(clientId, CurrentUser.require(), credential.getKind().label(), "άλλαξαν");
        return toResponse(credential);
    }

    @Transactional
    public void delete(UUID clientId, UUID credentialId) {
        checkAccess(clientId);
        ClientCredential credential = get(clientId, credentialId);
        audit(credential, CredentialAccessLog.Action.DELETE);
        repository.delete(credential);
        notifier.credentialsChanged(clientId, CurrentUser.require(), credential.getKind().label(), "διαγράφηκαν");
    }

    public List<CredentialLogEntry> log(UUID clientId) {
        checkAccess(clientId);
        return logRepository.findByClientIdOrderByCreatedAtDesc(clientId, PageRequest.of(0, LOG_LIMIT)).stream()
                .map(l -> new CredentialLogEntry(l.getKind().label(), l.getUserName(), l.isByClient(), l.getAction(), l.getCreatedAt()))
                .toList();
    }

    /** Defence in depth behind the URL rules: a portal account only reaches its own client. */
    private static void checkAccess(UUID clientId) {
        UserPrincipal caller = CurrentUser.require();
        boolean allowed = caller.role() == Role.CLIENT
                ? clientId.equals(caller.clientId())
                : caller.role() == Role.ADMIN || caller.role() == Role.ACCOUNTANT;
        if (!allowed) {
            throw new AccessDeniedException("No access to these credentials");
        }
    }

    private ClientCredential get(UUID clientId, UUID credentialId) {
        return repository.findByIdAndClientId(credentialId, clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Credential", credentialId));
    }

    private void apply(ClientCredential credential, CredentialRequest request) {
        UserPrincipal actor = CurrentUser.require();
        credential.setKind(request.kind());
        credential.setLabel(StringUtils.hasText(request.label()) ? request.label().trim() : null);
        credential.setUsername(request.username().trim());
        if (StringUtils.hasText(request.password())) {
            credential.setPasswordEncrypted(cipher.encrypt(request.password()));
        }
        credential.setUpdatedById(actor.id());
        credential.setUpdatedByName(actor.fullName());
        credential.setUpdatedByClient(actor.role() == Role.CLIENT);
    }

    private void audit(ClientCredential credential, CredentialAccessLog.Action action) {
        UserPrincipal actor = CurrentUser.require();
        CredentialAccessLog entry = new CredentialAccessLog();
        entry.setId(UUID.randomUUID());
        entry.setClientId(credential.getClientId());
        entry.setCredentialId(credential.getId());
        entry.setKind(credential.getKind());
        entry.setUserId(actor.id());
        entry.setUserName(actor.fullName());
        entry.setByClient(actor.role() == Role.CLIENT);
        entry.setAction(action);
        entry.setCreatedAt(clock.instant());
        logRepository.save(entry);
        log.info("Credential {} {} for client {} by {}", credential.getId(), action, credential.getClientId(), actor.email());
    }

    private static CredentialResponse toResponse(ClientCredential c) {
        return new CredentialResponse(c.getId(), c.getKind(), c.getKind().label(), c.getLabel(), c.getUsername(),
                c.getUpdatedByName(), c.isUpdatedByClient(), c.getUpdatedAt());
    }
}
