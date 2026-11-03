package com.thphatts.clinicportal.modules.person.entity;

import jakarta.persistence.*;
import com.thphatts.clinicportal.modules.user.entity.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Shared demographic record for patients, clinicians, and account holders. */
@Entity
@Table(name = "persons")
@Getter
@Setter
@NoArgsConstructor
public class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "person", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<PersonName> names = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "person", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<PersonAddress> addresses = new java.util.ArrayList<>();

    @Column(name = "birthdate")
    private LocalDate birthdate;

    @Column(length = 1)
    private String gender;

    @Column(name = "birthdate_estimated", nullable = false)
    private boolean birthdateEstimated;

    @Column(nullable = false)
    private boolean dead;

    @Column(name = "death_date")
    private LocalDateTime deathDate;

    @Column(name = "deathdate_estimated", nullable = false)
    private boolean deathdateEstimated;

    @Column(nullable = false, unique = true, updatable = false)
    private java.util.UUID uuid;

    @Column(name = "date_created", nullable = false, updatable = false)
    private LocalDateTime dateCreated;

    @Column(name = "date_changed")
    private LocalDateTime dateChanged;

    @Column(nullable = false)
    private boolean voided;

    @Column(name = "void_reason")
    private String voidReason;

    @PrePersist
    void onCreate() {
        if (dateCreated == null) dateCreated = LocalDateTime.now();
        if (uuid == null) uuid = java.util.UUID.randomUUID();
    }

    @PreUpdate
    void onUpdate() {
        dateChanged = LocalDateTime.now();
    }

    // Compatibility accessors keep API DTOs independent from OpenMRS-style child tables.
    @Transient
    public String getFullName() {
        return names().stream().filter(PersonName::isPreferred).findFirst()
                .or(() -> names().stream().findFirst()).map(PersonName::getFullName).orElse(null);
    }

    public void setFullName(String fullName) {
        PersonName name = names().stream().filter(PersonName::isPreferred).findFirst()
                .orElseGet(() -> { PersonName n = new PersonName(); n.setPerson(this); names().add(n); return n; });
        name.setFullName(fullName);
        name.setPreferred(true);
    }

    @Transient
    public String getAddress() {
        return addresses().stream().filter(PersonAddress::isPreferred).findFirst()
                .or(() -> addresses().stream().findFirst()).map(PersonAddress::getAddressText).orElse(null);
    }

    public void setAddress(String address) {
        if (address == null || address.isBlank()) return;
        PersonAddress preferred = addresses().stream().filter(PersonAddress::isPreferred).findFirst()
                .orElseGet(() -> { PersonAddress a = new PersonAddress(); a.setPerson(this); addresses().add(a); return a; });
        preferred.setAddressText(address);
        preferred.setPreferred(true);
    }

    @Transient
    public String getPhoneNumber() {
        return getAttribute("PHONE");
    }

    public void setPhoneNumber(String phoneNumber) {
        setAttribute("PHONE", phoneNumber);
    }

    @Transient
    public String getCitizenId() {
        return getAttribute("CITIZEN_ID");
    }

    public void setCitizenId(String citizenId) {
        setAttribute("CITIZEN_ID", citizenId);
    }

    @Transient
    public String getEmail() { return getAttribute("EMAIL"); }

    public void setEmail(String email) { setAttribute("EMAIL", email); }

    @Transient
    public String getAttribute(String attributeType) {
        return attributes().stream().filter(a -> a.getAttributeType().equals(attributeType))
                .map(PersonAttribute::getValue).findFirst().orElse(null);
    }

    public void setAttribute(String attributeType, String value) {
        if (value == null || value.isBlank()) return;
        PersonAttribute attribute = attributes().stream()
                .filter(a -> a.getAttributeType().equals(attributeType)).findFirst().orElseGet(() -> {
                    PersonAttribute created = new PersonAttribute();
                    created.setPerson(this);
                    created.setAttributeType(attributeType);
                    attributes().add(created);
                    return created;
                });
        attribute.setValue(value);
    }

    @OneToMany(mappedBy = "person", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<PersonAttribute> attributes = new java.util.ArrayList<>();

    @OneToOne(mappedBy = "person", fetch = FetchType.LAZY)
    private User user;

    private java.util.List<PersonAttribute> attributes() {
        if (attributes == null) attributes = new java.util.ArrayList<>();
        return attributes;
    }

    private java.util.List<PersonName> names() {
        if (names == null) names = new java.util.ArrayList<>();
        return names;
    }

    private java.util.List<PersonAddress> addresses() {
        if (addresses == null) addresses = new java.util.ArrayList<>();
        return addresses;
    }
}
