package gr.taxpulse.client.entity;

import gr.taxpulse.common.entity.BaseEntity;
import gr.taxpulse.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Aggregate root of the CRM: a client of the accounting office.
 *
 * <p>Activity codes and representatives are part of the aggregate (cascade + orphan removal),
 * so they are always modified through the client and never via their own repositories.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "clients")
public class Client extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "client_type", nullable = false, length = 20)
    private ClientType clientType;

    /** ΑΦΜ - mapped as CHAR(9) in PostgreSQL. */
    @Column(nullable = false, length = 9, columnDefinition = "bpchar(9)")
    private String afm;

    /** ΔΟΥ - competent tax office. */
    @Column(nullable = false, length = 100)
    private String doy;

    /** Εταιρική επωνυμία or ονοματεπώνυμο. */
    @Column(nullable = false)
    private String name;

    @Column(name = "trade_name")
    private String tradeName;

    @Column(name = "legal_form", length = 30)
    private String legalForm;

    @Enumerated(EnumType.STRING)
    @Column(name = "book_category", nullable = false, length = 10)
    private BookCategory bookCategory;

    @Column(name = "gemi_number", length = 20)
    private String gemiNumber;

    private String email;

    @Column(length = 30)
    private String phone;

    @Column(length = 30)
    private String mobile;

    @Embedded
    private Address address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_accountant_id")
    private User assignedAccountant;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(nullable = false)
    private boolean active = true;

    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("primary DESC, code ASC")
    private List<ClientActivityCode> activityCodes = new ArrayList<>();

    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("primary DESC, fullName ASC")
    private List<ClientRepresentative> representatives = new ArrayList<>();

    /**
     * Synchronises ΚΑΔ with the desired list, matching by code.
     *
     * <p>Existing rows are updated in place rather than deleted and re-inserted: Hibernate flushes
     * inserts before orphan deletes, so a clear-and-add would violate {@code ux_client_kad}.</p>
     */
    public void syncActivityCodes(List<ClientActivityCode> desired) {
        Map<String, ClientActivityCode> wanted = new LinkedHashMap<>();
        desired.forEach(code -> wanted.put(code.getCode(), code));
        activityCodes.removeIf(existing -> !wanted.containsKey(existing.getCode()));
        wanted.values().forEach(code -> activityCodes.stream()
                .filter(existing -> existing.getCode().equals(code.getCode()))
                .findFirst()
                .ifPresentOrElse(existing -> {
                    existing.setDescription(code.getDescription());
                    existing.setPrimary(code.isPrimary());
                }, () -> {
                    code.setClient(this);
                    activityCodes.add(code);
                }));
    }

    /** Replaces all representatives, keeping both sides of the association consistent. */
    public void replaceRepresentatives(List<ClientRepresentative> reps) {
        representatives.clear();
        reps.forEach(rep -> {
            rep.setClient(this);
            representatives.add(rep);
        });
    }

    public Optional<ClientActivityCode> primaryActivityCode() {
        return activityCodes.stream().filter(ClientActivityCode::isPrimary).findFirst();
    }
}
