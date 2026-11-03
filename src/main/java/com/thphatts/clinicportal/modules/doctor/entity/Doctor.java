package com.thphatts.clinicportal.modules.doctor.entity;

import com.thphatts.clinicportal.modules.person.entity.Person;
import com.thphatts.clinicportal.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "doctors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Doctor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "person_id", unique = true, nullable = false)
    private Person person;
    @Column(name = "specialization", nullable = false, length = 100)
    private String specialization;
    @Column(name = "degree", length = 100)
    private String degree;
    @Column(name = "experience_years")
    @Builder.Default
    private Integer experienceYears = 0;

    @Column(name = "consultation_fee", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal consultationFee = BigDecimal.ZERO;

    @Column(name = "average_rating", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "total_reviews")
    @Builder.Default
    private Integer totalReviews = 0;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }

    @Transient public String getFullName() { return person == null ? null : person.getFullName(); }
    @Transient public String getPhone() { return person == null ? null : person.getPhoneNumber(); }
    @Transient public String getEmail() { return person == null ? null : person.getEmail(); }
}
