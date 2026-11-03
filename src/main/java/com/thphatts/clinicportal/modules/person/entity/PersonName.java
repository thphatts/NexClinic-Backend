package com.thphatts.clinicportal.modules.person.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "person_names")
@Getter @Setter @NoArgsConstructor
public class PersonName {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Column(name = "given_name", nullable = false)
    private String givenName;

    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "family_name")
    private String familyName;

    @Column(nullable = false)
    private boolean preferred;

    @Transient
    public String getFullName() {
        return java.util.stream.Stream.of(familyName, middleName, givenName)
                .filter(s -> s != null && !s.isBlank()).collect(java.util.stream.Collectors.joining(" "));
    }

    public void setFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            this.givenName = null;
            this.middleName = null;
            this.familyName = null;
            return;
        }
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            this.givenName = parts[0];
            this.middleName = null;
            this.familyName = null;
            return;
        }
        this.familyName = parts[0];
        this.givenName = parts[parts.length - 1];
        this.middleName = parts.length > 2
                ? String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length - 1)) : null;
    }
}
