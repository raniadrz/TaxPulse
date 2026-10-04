package gr.taxpulse.client.mapper;

import gr.taxpulse.client.dto.ActivityCodeDto;
import gr.taxpulse.client.dto.AddressDto;
import gr.taxpulse.client.dto.ClientRequest;
import gr.taxpulse.client.dto.ClientResponse;
import gr.taxpulse.client.dto.ClientSummaryResponse;
import gr.taxpulse.client.dto.RepresentativeDto;
import gr.taxpulse.client.entity.Address;
import gr.taxpulse.client.entity.Client;
import gr.taxpulse.client.entity.ClientActivityCode;
import gr.taxpulse.client.entity.ClientRepresentative;
import gr.taxpulse.client.service.ClientObligationStatsPort.Stats;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Explicit Entity <-> DTO mapping for the client aggregate.
 * Associations that require repository look-ups (assigned accountant) are resolved by the service.
 */
@Component
public class ClientMapper {

    /** Copies scalar fields and child collections from the request onto the (new or managed) entity. */
    public void apply(ClientRequest request, Client client) {
        client.setClientType(request.clientType());
        client.setAfm(request.afm().trim());
        client.setDoy(request.doy().trim());
        client.setName(request.name().trim());
        client.setTradeName(blankToNull(request.tradeName()));
        client.setLegalForm(blankToNull(request.legalForm()));
        client.setBookCategory(request.bookCategory());
        client.setGemiNumber(blankToNull(request.gemiNumber()));
        client.setEmail(blankToNull(request.email()));
        client.setPhone(blankToNull(request.phone()));
        client.setMobile(blankToNull(request.mobile()));
        client.setAddress(toAddress(request.address()));
        client.setNotes(blankToNull(request.notes()));
        if (request.active() != null) {
            client.setActive(request.active());
        }
        client.syncActivityCodes(toActivityCodes(request.activityCodesOrEmpty()));
        client.replaceRepresentatives(request.representativesOrEmpty().stream().map(this::toRepresentative).toList());
    }

    public ClientResponse toResponse(Client c) {
        var accountant = c.getAssignedAccountant() == null ? null
                : new ClientResponse.AccountantRef(c.getAssignedAccountant().getId(), c.getAssignedAccountant().getFullName());
        return new ClientResponse(
                c.getId(), c.getClientType(), c.getAfm(), c.getDoy(), c.getName(), c.getTradeName(),
                c.getLegalForm(), c.getBookCategory(), c.getGemiNumber(), c.getEmail(), c.getPhone(),
                c.getMobile(), toAddressDto(c.getAddress()), accountant, c.getNotes(), c.isActive(),
                c.getActivityCodes().stream()
                        .map(k -> new ActivityCodeDto(k.getCode(), k.getDescription(), k.isPrimary())).toList(),
                c.getRepresentatives().stream()
                        .map(r -> new RepresentativeDto(r.getId(), r.getFullName(), r.getAfm(), r.getRole(),
                                r.getEmail(), r.getPhone(), r.isPrimary())).toList(),
                c.getCreatedAt(), c.getUpdatedAt(), c.getVersion());
    }

    public ClientSummaryResponse toSummary(Client c, Stats stats) {
        Stats s = stats == null ? Stats.EMPTY : stats;
        return new ClientSummaryResponse(
                c.getId(), c.getClientType(), c.getAfm(), c.getName(), c.getDoy(), c.getBookCategory(),
                c.primaryActivityCode().map(ClientActivityCode::getCode).orElse(null),
                c.getEmail(), c.getPhone(),
                c.getAssignedAccountant() == null ? null : c.getAssignedAccountant().getFullName(),
                c.isActive(), s.open(), s.overdue(), s.nextDueDate());
    }

    /** ΚΑΔ are stored as digits only, e.g. "69.20.10.01" -> "69201001". */
    public static String normalizeKad(String code) {
        return code.replace(".", "");
    }

    /** Normalises ΚΑΔ to digits only; if none is flagged primary, the first one becomes primary. */
    private List<ClientActivityCode> toActivityCodes(List<ActivityCodeDto> dtos) {
        boolean hasPrimary = dtos.stream().anyMatch(ActivityCodeDto::primary);
        return java.util.stream.IntStream.range(0, dtos.size()).mapToObj(i -> {
            ActivityCodeDto dto = dtos.get(i);
            ClientActivityCode code = new ClientActivityCode();
            code.setCode(normalizeKad(dto.code()));
            code.setDescription(blankToNull(dto.description()));
            code.setPrimary(hasPrimary ? dto.primary() : i == 0);
            return code;
        }).toList();
    }

    private ClientRepresentative toRepresentative(RepresentativeDto dto) {
        ClientRepresentative rep = new ClientRepresentative();
        rep.setFullName(dto.fullName().trim());
        rep.setAfm(blankToNull(dto.afm()));
        rep.setRole(blankToNull(dto.role()));
        rep.setEmail(blankToNull(dto.email()));
        rep.setPhone(blankToNull(dto.phone()));
        rep.setPrimary(dto.primary());
        return rep;
    }

    private static Address toAddress(AddressDto dto) {
        if (dto == null) {
            return null;
        }
        String postal = dto.postalCode() == null ? null : dto.postalCode().replace(" ", "");
        return new Address(blankToNull(dto.street()), blankToNull(dto.city()), blankToNull(postal));
    }

    private static AddressDto toAddressDto(Address a) {
        return a == null ? null : new AddressDto(a.getStreet(), a.getCity(), a.getPostalCode());
    }

    private static String blankToNull(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }
}
