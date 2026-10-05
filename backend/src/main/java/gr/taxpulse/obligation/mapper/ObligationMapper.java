package gr.taxpulse.obligation.mapper;

import gr.taxpulse.obligation.dto.ObligationRequest;
import gr.taxpulse.obligation.dto.ObligationResponse;
import gr.taxpulse.obligation.entity.TaxObligation;
import gr.taxpulse.obligation.service.ObligationActivityPort;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Entity <-> DTO mapping for obligations. Associations and due date are applied by the service. */
@Component
public class ObligationMapper {

    public void apply(ObligationRequest request, TaxObligation o) {
        o.setObligationType(request.obligationType());
        o.setTitle(request.title().trim());
        o.setDescription(blankToNull(request.description()));
        o.setPeriodStart(request.periodStart());
        o.setPeriodEnd(request.periodEnd());
        o.setAmount(request.amount());
        o.setNotes(blankToNull(request.notes()));
    }

    public ObligationResponse toResponse(TaxObligation o, LocalDate today) {
        return toResponse(o, today, ObligationActivityPort.Activity.EMPTY);
    }

    public ObligationResponse toResponse(TaxObligation o, LocalDate today, ObligationActivityPort.Activity activity) {
        var client = new ObligationResponse.ClientRef(o.getClient().getId(), o.getClient().getName(), o.getClient().getAfm());
        var assignee = o.getAssignedTo() == null ? null
                : new ObligationResponse.UserRef(o.getAssignedTo().getId(), o.getAssignedTo().getFullName());
        return new ObligationResponse(
                o.getId(), client, o.getObligationType(), o.getObligationType().label(), o.getTitle(),
                o.getDescription(), o.getPeriodStart(), o.getPeriodEnd(), o.getDueDate(),
                ChronoUnit.DAYS.between(today, o.getDueDate()), o.getStatus(), o.getAmount(), assignee,
                o.getSubmittedAt(), o.getSubmissionRef(), o.getNotes(), o.getCreatedAt(), o.getUpdatedAt(),
                o.getVersion(), activity.clientDocuments(), activity.messages());
    }

    private static String blankToNull(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }
}
