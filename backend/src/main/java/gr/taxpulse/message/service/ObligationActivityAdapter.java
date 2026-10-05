package gr.taxpulse.message.service;

import gr.taxpulse.obligation.service.ObligationActivityPort;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Counts client-portal documents and conversation messages per obligation, for one page at a time. */
@Component
@RequiredArgsConstructor
public class ObligationActivityAdapter implements ObligationActivityPort {

    private static final String SQL = """
            select o.id,
                   (select count(*) from documents d join users u on u.id = d.uploaded_by_id
                     where d.obligation_id = o.id and u.role = 'CLIENT') as client_documents,
                   (select count(*) from obligation_messages m where m.obligation_id = o.id) as messages
              from tax_obligations o
             where o.id in (:ids)
            """;

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, Activity> activityFor(Collection<UUID> obligationIds) {
        if (obligationIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Activity> result = new HashMap<>();
        jdbc.query(SQL, Map.of("ids", obligationIds), rs -> {
            result.put(rs.getObject("id", UUID.class),
                    new Activity(rs.getLong("client_documents"), rs.getLong("messages")));
        });
        return result;
    }
}
