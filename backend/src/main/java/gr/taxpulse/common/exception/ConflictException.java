package gr.taxpulse.common.exception;

/** Thrown when a request violates a uniqueness or state rule (e.g. duplicate ΑΦΜ). Mapped to HTTP 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
