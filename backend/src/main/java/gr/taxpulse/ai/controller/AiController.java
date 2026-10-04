package gr.taxpulse.ai.controller;

import gr.taxpulse.ai.dto.ChatRequest;
import gr.taxpulse.ai.dto.ChatResponse;
import gr.taxpulse.ai.dto.ExtractionRequest;
import gr.taxpulse.ai.dto.ExtractionResponse;
import gr.taxpulse.ai.dto.ReminderEmailRequest;
import gr.taxpulse.ai.dto.ReminderEmailResponse;
import gr.taxpulse.ai.ollama.OllamaHealth;
import gr.taxpulse.ai.ollama.OllamaIntegrationService;
import gr.taxpulse.ai.service.AiChatService;
import gr.taxpulse.ai.service.DataExtractionService;
import gr.taxpulse.ai.service.ReminderEmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Local AI copilot endpoints (all processing happens on the on-premise Ollama server). */
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiChatService chatService;
    private final DataExtractionService extractionService;
    private final ReminderEmailService reminderEmailService;
    private final OllamaIntegrationService ollama;

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return chatService.chat(request);
    }

    /** Structured extraction (ΑΦΜ, ποσό, ημερομηνία, ...) from free text. */
    @PostMapping("/extract")
    public ExtractionResponse extract(@Valid @RequestBody ExtractionRequest request) {
        return extractionService.extract(request.text());
    }

    /** Draft (not send) a reminder e-mail for an obligation. */
    @PostMapping("/reminder-email")
    public ReminderEmailResponse reminderEmail(@Valid @RequestBody ReminderEmailRequest request) {
        return reminderEmailService.draft(request);
    }

    @GetMapping("/health")
    public OllamaHealth health() {
        return ollama.health();
    }
}
