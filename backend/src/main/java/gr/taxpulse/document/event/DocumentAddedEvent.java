package gr.taxpulse.document.event;

import java.util.UUID;

/**
 * Published inside the upload transaction for every new document.
 *
 * @param uploadedByClient true when the file came through the client portal
 */
public record DocumentAddedEvent(
        UUID documentId,
        UUID clientId,
        UUID obligationId,
        String filename,
        UUID uploaderId,
        boolean uploadedByClient) {
}
