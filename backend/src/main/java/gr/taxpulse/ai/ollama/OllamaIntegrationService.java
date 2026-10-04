package gr.taxpulse.ai.ollama;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.taxpulse.ai.ollama.dto.OllamaChatRequest;
import gr.taxpulse.ai.ollama.dto.OllamaChatResponse;
import gr.taxpulse.ai.ollama.dto.OllamaEmbedRequest;
import gr.taxpulse.ai.ollama.dto.OllamaEmbedResponse;
import gr.taxpulse.ai.ollama.dto.OllamaGenerateRequest;
import gr.taxpulse.ai.ollama.dto.OllamaGenerateResponse;
import gr.taxpulse.ai.ollama.dto.OllamaMessage;
import gr.taxpulse.ai.ollama.dto.OllamaTagsResponse;
import gr.taxpulse.common.exception.ExternalServiceException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * HTTP adapter for the local Ollama REST API.
 *
 * <ul>
 *   <li>{@code POST /api/chat} - multi-turn chat</li>
 *   <li>{@code POST /api/generate} - single-shot completion</li>
 *   <li>{@code POST /api/embed} - embeddings for RAG</li>
 *   <li>{@code GET  /api/tags} - installed models (health check)</li>
 * </ul>
 *
 * <p><b>Structured outputs:</b> passing a JSON Schema in the {@code format} field makes Ollama
 * constrain token sampling to that grammar, so the reply is always syntactically valid JSON of
 * the requested shape. Semantic validation (e.g. ΑΦΜ check digit) remains the caller's job.</p>
 *
 * <p>All calls are non-streaming and run on virtual threads, so blocking is cheap. Every request
 * stays on the local network: no client data leaves the office (GDPR).</p>
 */
@Slf4j
@Service
public class OllamaIntegrationService implements ChatModelClient, EmbeddingModelClient {

    private final RestClient restClient;
    private final OllamaProperties properties;
    private final ObjectMapper objectMapper;

    public OllamaIntegrationService(@Qualifier(OllamaClientConfig.OLLAMA_REST_CLIENT) RestClient restClient,
                                    OllamaProperties properties,
                                    ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    // ------------------------------------------------------------------ chat

    @Override
    public String chat(List<OllamaMessage> messages) {
        return doChat(messages, null, properties.temperature());
    }

    @Override
    public <T> T chatStructured(List<OllamaMessage> messages, Map<String, Object> jsonSchema, Class<T> type) {
        Objects.requireNonNull(jsonSchema, "jsonSchema");
        // Deterministic decoding for extraction tasks.
        return parseJson(doChat(messages, jsonSchema, 0.0), type);
    }

    private String doChat(List<OllamaMessage> messages, Object format, double temperature) {
        OllamaChatRequest request = new OllamaChatRequest(
                properties.chatModel(), messages, false, format, options(temperature), properties.keepAlive());
        OllamaChatResponse response = call("chat", properties.chatModel(), () -> restClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(OllamaChatResponse.class));
        if (response == null || response.message() == null || response.message().content() == null) {
            throw new ExternalServiceException("Το Ollama επέστρεψε κενή απάντηση");
        }
        log.debug("Ollama chat done model={} promptTokens={} evalTokens={} durationMs={}", response.model(),
                response.promptEvalCount(), response.evalCount(),
                response.totalDuration() == null ? null : response.totalDuration() / 1_000_000);
        return response.message().content().trim();
    }

    // -------------------------------------------------------------- generate

    @Override
    public String generate(String systemPrompt, String prompt) {
        return doGenerate(systemPrompt, prompt, null, properties.temperature());
    }

    @Override
    public <T> T generateStructured(String systemPrompt, String prompt, Map<String, Object> jsonSchema, Class<T> type) {
        Objects.requireNonNull(jsonSchema, "jsonSchema");
        return parseJson(doGenerate(systemPrompt, prompt, jsonSchema, 0.0), type);
    }

    private String doGenerate(String systemPrompt, String prompt, Object format, double temperature) {
        OllamaGenerateRequest request = new OllamaGenerateRequest(
                properties.chatModel(), prompt, systemPrompt, false, format, options(temperature), properties.keepAlive());
        OllamaGenerateResponse response = call("generate", properties.chatModel(), () -> restClient.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(OllamaGenerateResponse.class));
        if (response == null || response.response() == null) {
            throw new ExternalServiceException("Το Ollama επέστρεψε κενή απάντηση");
        }
        return response.response().trim();
    }

    // ------------------------------------------------------------- embedding

    @Override
    public List<float[]> embed(List<String> inputs) {
        if (inputs.isEmpty()) {
            return List.of();
        }
        OllamaEmbedRequest request = new OllamaEmbedRequest(properties.embeddingModel(), inputs, properties.keepAlive());
        OllamaEmbedResponse response = call("embed", properties.embeddingModel(), () -> restClient.post()
                .uri("/api/embed")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(OllamaEmbedResponse.class));
        if (response == null || response.embeddings() == null || response.embeddings().size() != inputs.size()) {
            throw new ExternalServiceException("Μη αναμενόμενη απάντηση embeddings από το Ollama");
        }
        return response.embeddings();
    }

    // ---------------------------------------------------------------- health

    /** Never throws: reports reachability and whether the configured models are pulled. */
    public OllamaHealth health() {
        try {
            OllamaTagsResponse tags = restClient.get().uri("/api/tags").retrieve().body(OllamaTagsResponse.class);
            List<String> names = tags == null || tags.models() == null ? List.of()
                    : tags.models().stream().map(OllamaTagsResponse.Model::name).toList();
            return new OllamaHealth(true, properties.baseUrl(),
                    properties.chatModel(), isInstalled(names, properties.chatModel()),
                    properties.embeddingModel(), isInstalled(names, properties.embeddingModel()),
                    names, null);
        } catch (RestClientException ex) {
            return new OllamaHealth(false, properties.baseUrl(), properties.chatModel(), false,
                    properties.embeddingModel(), false, List.of(), ex.getMessage());
        }
    }

    /** "llama3.2" matches the installed tag "llama3.2:latest". */
    static boolean isInstalled(List<String> installed, String model) {
        String wanted = model.contains(":") ? model : model + ":latest";
        return installed.stream().anyMatch(name -> name.equals(model) || name.equals(wanted));
    }

    // --------------------------------------------------------------- helpers

    private Map<String, Object> options(double temperature) {
        return Map.of("temperature", temperature, "num_ctx", properties.contextWindow());
    }

    /** Uniform error translation for every Ollama call. */
    private <T> T call(String operation, String model, Supplier<T> request) {
        long start = System.nanoTime();
        try {
            return request.get();
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ExternalServiceException(
                        "Το μοντέλο '%s' δεν είναι εγκατεστημένο στο Ollama. Εκτελέστε: ollama pull %s"
                                .formatted(model, model), ex);
            }
            throw new ExternalServiceException("Το Ollama απέρριψε το αίτημα (%s): %s"
                    .formatted(ex.getStatusCode().value(), ex.getResponseBodyAsString()), ex);
        } catch (ResourceAccessException ex) {
            throw new ExternalServiceException(
                    "Η υπηρεσία AI (Ollama) δεν είναι διαθέσιμη στο " + properties.baseUrl(), ex);
        } catch (RestClientResponseException ex) {
            throw new ExternalServiceException("Σφάλμα Ollama (%s)".formatted(ex.getStatusCode().value()), ex);
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Αποτυχία επικοινωνίας με το Ollama: " + ex.getMessage(), ex);
        } finally {
            log.debug("Ollama {} took {} ms", operation, (System.nanoTime() - start) / 1_000_000);
        }
    }

    private <T> T parseJson(String content, Class<T> type) {
        try {
            return objectMapper.readValue(content, type);
        } catch (JsonProcessingException ex) {
            log.warn("Unparseable structured output from Ollama: {}", abbreviate(content));
            throw new ExternalServiceException("Το μοντέλο επέστρεψε μη έγκυρο JSON", ex);
        }
    }

    private static String abbreviate(String s) {
        return s.length() <= 500 ? s : s.substring(0, 500) + "...";
    }
}
