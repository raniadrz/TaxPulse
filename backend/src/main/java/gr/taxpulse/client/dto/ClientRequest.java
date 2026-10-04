package gr.taxpulse.client.dto;

import gr.taxpulse.client.entity.BookCategory;
import gr.taxpulse.client.entity.ClientType;
import gr.taxpulse.common.validation.ValidAfm;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** Payload for creating or fully updating a client (PUT semantics). */
public record ClientRequest(
        @NotNull ClientType clientType,
        @NotBlank @ValidAfm String afm,
        @NotBlank @Size(max = 100) String doy,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 255) String tradeName,
        @Size(max = 30) String legalForm,
        @NotNull BookCategory bookCategory,
        @Size(max = 20) String gemiNumber,
        @Email @Size(max = 255) String email,
        @Size(max = 30) String phone,
        @Size(max = 30) String mobile,
        @Valid AddressDto address,
        UUID assignedAccountantId,
        String notes,
        Boolean active,
        @Valid @Size(max = 50) List<ActivityCodeDto> activityCodes,
        @Valid @Size(max = 20) List<RepresentativeDto> representatives) {

    public List<ActivityCodeDto> activityCodesOrEmpty() {
        return activityCodes == null ? List.of() : activityCodes;
    }

    public List<RepresentativeDto> representativesOrEmpty() {
        return representatives == null ? List.of() : representatives;
    }

    @AssertTrue(message = "Επιτρέπεται μόνο ένας κύριος ΚΑΔ")
    boolean isSinglePrimaryActivityCode() {
        return activityCodesOrEmpty().stream().filter(ActivityCodeDto::primary).count() <= 1;
    }
}
