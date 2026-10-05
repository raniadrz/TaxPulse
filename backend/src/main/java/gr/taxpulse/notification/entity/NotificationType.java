package gr.taxpulse.notification.entity;

public enum NotificationType {
    DEADLINE_UPCOMING,
    DEADLINE_OVERDUE,
    DOCUMENT_PROCESSED,
    SYSTEM,
    /** A new message in an obligation's accountant/client conversation. */
    MESSAGE,
    /** The office moved one of the client's obligations forward (e.g. submitted it). */
    STATUS_CHANGED,
    /** A document arrived from the other side (client upload, or one shared by the office). */
    DOCUMENT_RECEIVED
}
