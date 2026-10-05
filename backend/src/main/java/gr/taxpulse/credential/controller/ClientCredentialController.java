package gr.taxpulse.credential.controller;

import gr.taxpulse.credential.dto.CredentialLogEntry;
import gr.taxpulse.credential.dto.CredentialRequest;
import gr.taxpulse.credential.dto.CredentialResponse;
import gr.taxpulse.credential.dto.CredentialSecretResponse;
import gr.taxpulse.credential.service.ClientCredentialService;
import gr.taxpulse.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Client credentials. Office: ADMIN and ACCOUNTANT only (assistants never see them). Client
 * portal: the caller's own client, whose id always comes from the session.
 */
@RestController
@RequiredArgsConstructor
public class ClientCredentialController {

    private static final String STAFF = "/api/v1/clients/{clientId}/credentials";
    private static final String PORTAL = "/api/v1/portal/credentials";
    private static final String OFFICE = "hasAnyRole('ADMIN','ACCOUNTANT')";

    private final ClientCredentialService service;

    // ---- Office ----
    @GetMapping(STAFF)
    @PreAuthorize(OFFICE)
    public List<CredentialResponse> list(@PathVariable UUID clientId) {
        return service.list(clientId);
    }

    @PostMapping(STAFF + "/{id}/reveal")
    @PreAuthorize(OFFICE)
    public CredentialSecretResponse reveal(@PathVariable UUID clientId, @PathVariable UUID id) {
        return service.reveal(clientId, id);
    }

    @PostMapping(STAFF)
    @PreAuthorize(OFFICE)
    @ResponseStatus(HttpStatus.CREATED)
    public CredentialResponse create(@PathVariable UUID clientId, @Valid @RequestBody CredentialRequest request) {
        return service.create(clientId, request);
    }

    @PutMapping(STAFF + "/{id}")
    @PreAuthorize(OFFICE)
    public CredentialResponse update(@PathVariable UUID clientId, @PathVariable UUID id, @Valid @RequestBody CredentialRequest request) {
        return service.update(clientId, id, request);
    }

    @DeleteMapping(STAFF + "/{id}")
    @PreAuthorize(OFFICE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID clientId, @PathVariable UUID id) {
        service.delete(clientId, id);
    }

    @GetMapping(STAFF + "/log")
    @PreAuthorize(OFFICE)
    public List<CredentialLogEntry> log(@PathVariable UUID clientId) {
        return service.log(clientId);
    }

    // ---- Client portal ----
    @GetMapping(PORTAL)
    public List<CredentialResponse> portalList() {
        return service.list(ownClient());
    }

    @PostMapping(PORTAL + "/{id}/reveal")
    public CredentialSecretResponse portalReveal(@PathVariable UUID id) {
        return service.reveal(ownClient(), id);
    }

    @PostMapping(PORTAL)
    @ResponseStatus(HttpStatus.CREATED)
    public CredentialResponse portalCreate(@Valid @RequestBody CredentialRequest request) {
        return service.create(ownClient(), request);
    }

    @PutMapping(PORTAL + "/{id}")
    public CredentialResponse portalUpdate(@PathVariable UUID id, @Valid @RequestBody CredentialRequest request) {
        return service.update(ownClient(), id, request);
    }

    @DeleteMapping(PORTAL + "/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void portalDelete(@PathVariable UUID id) {
        service.delete(ownClient(), id);
    }

    @GetMapping(PORTAL + "/log")
    public List<CredentialLogEntry> portalLog() {
        return service.log(ownClient());
    }

    /** Portal routes are CLIENT-only (SecurityConfig), so the session always carries a client id. */
    private static UUID ownClient() {
        UUID clientId = CurrentUser.require().clientId();
        if (clientId == null) {
            throw new AccessDeniedException("Not a client portal account");
        }
        return clientId;
    }
}
