package gr.taxpulse.client.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** ΚΑΔ (Κωδικός Αριθμός Δραστηριότητας) assigned to a client. Owned by {@link Client}. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "client_activity_codes")
public class ClientActivityCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(nullable = false, length = 12)
    private String code;

    private String description;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;
}
