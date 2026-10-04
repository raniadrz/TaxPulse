package gr.taxpulse.ai.dto;

/** Draft only: the accountant reviews and sends it. Nothing is e-mailed automatically. */
public record ReminderEmailResponse(String subject, String body, String recipientName, String recipientEmail) {
}
