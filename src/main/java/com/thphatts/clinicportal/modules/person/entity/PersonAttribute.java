package com.thphatts.clinicportal.modules.person.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "person_attributes", indexes = @Index(name = "idx_person_attribute_lookup", columnList = "attribute_type, attribute_value"))
@Getter @Setter @NoArgsConstructor
public class PersonAttribute {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Column(name = "attribute_type", nullable = false, length = 40)
    private String attributeType;

    @Column(name = "attribute_value", nullable = false, length = 255)
    private String value;
}
