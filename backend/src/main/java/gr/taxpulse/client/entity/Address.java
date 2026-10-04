package gr.taxpulse.client.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Postal address value object, embedded in the owning table. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class Address {

    @Column(name = "address_street")
    private String street;

    @Column(name = "address_city", length = 100)
    private String city;

    @Column(name = "address_postal_code", length = 10)
    private String postalCode;
}
