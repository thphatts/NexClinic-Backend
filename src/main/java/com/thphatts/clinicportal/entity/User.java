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
    @Column(name = "address", length = 30)
    private String address;
    @Column(name = "phone", length = 10)
    private String phoneNumber;
    @Column(unique = true)
    private String email;
    private String username;
    private String password;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Role role = Role.ROLE_PATIENT;
}
