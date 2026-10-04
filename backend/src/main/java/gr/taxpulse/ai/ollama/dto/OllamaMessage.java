package gr.taxpulse.ai.ollama.dto;

/** A chat turn in Ollama's wire format. Role is one of {@code system | user | assistant}. */
public record OllamaMessage(String role, String content) {

    public static OllamaMessage system(String content) {
        return new OllamaMessage("system", content);
    }

    public static OllamaMessage user(String content) {
        return new OllamaMessage("user", content);
    }

    public static OllamaMessage assistant(String content) {
        return new OllamaMessage("assistant", content);
    }
}
