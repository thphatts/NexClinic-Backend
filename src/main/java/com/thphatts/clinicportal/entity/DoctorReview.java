package com.thphatts.clinicportal.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;


@Setter
@Getter
@Entity
@Table(name = "doctor_reviews")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorReview {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @Column(name = "rating",nullable = false)
    private Integer rating;

     @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = true)
    private Patient patient;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = true)
    private Appointment appointment;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "visit_count_snapshot")
    private Integer visitCountSnapshot;

    @Builder.Default
    @Column(nullable = false)
    private Boolean deleted = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
