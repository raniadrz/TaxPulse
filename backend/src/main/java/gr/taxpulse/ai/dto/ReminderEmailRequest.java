package gr.taxpulse.ai.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ReminderEmailRequest(
        @NotNull UUID obligationId,
        Tone tone,
        @Size(max = 1000) String additionalInstructions) {

    public enum Tone { FORMAL, FRIENDLY, URGENT }

    public Tone toneOrDefault() {
        return tone == null ? Tone.FORMAL : tone;
    }
}
