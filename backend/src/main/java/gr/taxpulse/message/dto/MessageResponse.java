package gr.taxpulse.message.dto;

import java.time.Instant;
import java.util.UUID;

/** @param mine whether the viewer wrote this message (drives left/right alignment in the UI) */
public record MessageResponse(UUID id, String authorName, boolean fromClient, boolean mine, String body, Instant createdAt) {
}
