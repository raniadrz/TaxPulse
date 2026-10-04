package gr.taxpulse.ai.service;

import gr.taxpulse.ai.dto.ChatMessageDto;
import gr.taxpulse.ai.dto.ChatRequest;
import gr.taxpulse.ai.dto.ChatResponse;
import gr.taxpulse.ai.ollama.ChatModelClient;
import gr.taxpulse.ai.ollama.OllamaProperties;
import gr.taxpulse.ai.ollama.dto.OllamaMessage;
import gr.taxpulse.ai.prompt.PromptTemplates;
import gr.taxpulse.client.entity.Client;
import gr.taxpulse.client.service.ClientService;
import gr.taxpulse.obligation.dto.ObligationSearchCriteria;
import gr.taxpulse.obligation.entity.ObligationStatus;
import gr.taxpulse.obligation.service.TaxObligationService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Copilot chat. Optionally grounded on a client's profile and open obligations. */
@Service
@RequiredArgsConstructor
public class AiChatService {

    /** Bounds prompt size: only the latest turns are sent to the model. */
    static final int MAX_HISTORY = 20;

    private final ChatModelClient chatModel;
    private final OllamaProperties ollamaProperties;
    private final ClientService clientService;
    private final TaxObligationService obligationService;

    @Transactional(readOnly = true)
    public ChatResponse chat(ChatRequest request) {
        List<OllamaMessage> messages = new ArrayList<>();
        messages.add(OllamaMessage.system(PromptTemplates.ASSISTANT_SYSTEM));
        if (request.clientId() != null) {
            messages.add(OllamaMessage.system(clientContext(request.clientId())));
        }
        messages.addAll(toOllama(request.messages()));
        return new ChatResponse(chatModel.chat(messages), ollamaProperties.chatModel(), List.of());
    }

    static List<OllamaMessage> toOllama(List<ChatMessageDto> history) {
        int from = Math.max(0, history.size() - MAX_HISTORY);
        return history.subList(from, history.size()).stream()
                .map(m -> m.role() == ChatMessageDto.Role.USER
                        ? OllamaMessage.user(m.content())
                        : OllamaMessage.assistant(m.content()))
                .toList();
    }

    private String clientContext(java.util.UUID clientId) {
        Client client = clientService.getEntity(clientId);
        var open = obligationService.search(
                new ObligationSearchCriteria(clientId,
                        List.of(ObligationStatus.PENDING_DOCS, ObligationStatus.IN_PROGRESS, ObligationStatus.OVERDUE),
                        null, null, null, null, null),
                PageRequest.of(0, 15, Sort.by("dueDate")));

        StringBuilder sb = new StringBuilder("<context>\nΣτοιχεία πελάτη:\n")
                .append("- Επωνυμία: ").append(client.getName()).append('\n')
                .append("- ΑΦΜ: ").append(client.getAfm()).append(", ΔΟΥ: ").append(client.getDoy()).append('\n')
                .append("- Τύπος: ").append(client.getClientType()).append(", Κατηγορία βιβλίων: ")
                .append(client.getBookCategory()).append('\n');
        client.primaryActivityCode().ifPresent(k -> sb.append("- Κύριος ΚΑΔ: ").append(k.getCode()).append('\n'));
        sb.append("Ανοιχτές υποχρεώσεις:\n");
        if (open.content().isEmpty()) {
            sb.append("- Καμία\n");
        }
        open.content().forEach(o -> sb.append("- ").append(o.obligationTypeLabel()).append(": ").append(o.title())
                .append(", λήξη ").append(o.dueDate()).append(", κατάσταση ").append(o.status()).append('\n'));
        return sb.append("</context>").toString();
    }
}
