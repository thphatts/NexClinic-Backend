package com.thphatts.clinicportal.modules.person.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "person_addresses")
@Getter @Setter @NoArgsConstructor
public class PersonAddress {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Column(name = "address_text", nullable = false)
    private String addressText;

    @Column(nullable = false)
    private boolean preferred;
}
