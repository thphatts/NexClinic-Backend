package com.thphatts.clinicportal.entity;

import jakarta.persistence.*;
import lombok.*;

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

    private String name;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "phone", length = 15)
    private String phoneNumber;

    @Column(unique = true, length = 100)
    private String email;

    @Column(length = 50)
    private String username;

    private String password;

    @Column(name = "citizen_id", unique = true, length = 12)
    private String citizenId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Role role = Role.ROLE_PATIENT;
}
