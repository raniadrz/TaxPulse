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

/** Legal representative / administrator (Εκπρόσωπος / Διαχειριστής) of a client. Owned by {@link Client}. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "client_representatives")
public class ClientRepresentative {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(length = 9, columnDefinition = "bpchar(9)")
    private String afm;

    @Column(length = 100)
    private String role;

    private String email;

    @Column(length = 30)
    private String phone;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;
}
