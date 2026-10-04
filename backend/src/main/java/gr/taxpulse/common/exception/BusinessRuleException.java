package gr.taxpulse.common.exception;

/** Thrown when a domain invariant is violated (e.g. invalid status transition). Mapped to HTTP 422. */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
