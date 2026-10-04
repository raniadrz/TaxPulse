package gr.taxpulse.common.exception;

/** Thrown when a downstream dependency (e.g. Ollama) fails or is unreachable. Mapped to HTTP 502/503. */
public class ExternalServiceException extends RuntimeException {

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExternalServiceException(String message) {
        super(message);
    }
}
