package gr.taxpulse.ai.ollama;

import gr.taxpulse.ai.ollama.dto.OllamaMessage;
import java.util.List;
import java.util.Map;

/**
 * Provider-agnostic port for text generation. Use cases depend on this abstraction, so the
 * local Ollama runtime could be replaced (vLLM, llama.cpp server, ...) without touching them.
 */
public interface ChatModelClient {

    /** Multi-turn chat, free-text answer ({@code /api/chat}). */
    String chat(List<OllamaMessage> messages);

    /**
     * Multi-turn chat whose answer is constrained to {@code jsonSchema} and deserialised to {@code type}.
     *
     * @param jsonSchema JSON Schema (as nested maps) passed to Ollama's {@code format} parameter
     */
    <T> T chatStructured(List<OllamaMessage> messages, Map<String, Object> jsonSchema, Class<T> type);

    /** Single-shot completion ({@code /api/generate}). */
    String generate(String systemPrompt, String prompt);

    /** Single-shot completion constrained to a JSON Schema ({@code /api/generate} + {@code format}). */
    <T> T generateStructured(String systemPrompt, String prompt, Map<String, Object> jsonSchema, Class<T> type);
}
