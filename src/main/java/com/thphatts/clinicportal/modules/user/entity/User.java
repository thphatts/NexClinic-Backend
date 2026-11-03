package com.thphatts.clinicportal.modules.user.entity;

import com.thphatts.clinicportal.modules.person.entity.Person;
import com.thphatts.clinicportal.modules.user.entity.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @OneToOne(optional = false, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "person_id", unique = true, nullable = false)
    private Person person;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Role role = Role.ROLE_PATIENT;

   private LocalDateTime lastLogin;

   @Transient public String getName() { return person == null ? null : person.getFullName(); }
   @Transient public String getPhoneNumber() { return person == null ? null : person.getPhoneNumber(); }
   @Transient public String getAddress() { return person == null ? null : person.getAddress(); }
   @Transient public String getCitizenId() { return person == null ? null : person.getCitizenId(); }
}
