package gr.taxpulse.message.controller;

import gr.taxpulse.message.dto.MessageRequest;
import gr.taxpulse.message.dto.MessageResponse;
import gr.taxpulse.message.service.ObligationMessageService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Obligation conversation, under the staff API and the client portal. Both routes share the
 * service, which enforces that a portal account only reaches its own client's obligations.
 */
@RestController
@RequiredArgsConstructor
public class ObligationMessageController {

    private final ObligationMessageService service;

    @GetMapping({"/api/v1/obligations/{id}/messages", "/api/v1/portal/obligations/{id}/messages"})
    public List<MessageResponse> list(@PathVariable UUID id) {
        return service.list(id);
    }

    @PostMapping({"/api/v1/obligations/{id}/messages", "/api/v1/portal/obligations/{id}/messages"})
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse post(@PathVariable UUID id, @Valid @RequestBody MessageRequest request) {
        return service.post(id, request.body());
    }
}
