package gr.taxpulse.credential.dto;

import gr.taxpulse.credential.entity.CredentialAccessLog;
import java.time.Instant;

public record CredentialLogEntry(String kindLabel, String userName, boolean byClient, CredentialAccessLog.Action action,
                                 Instant createdAt) {
}
