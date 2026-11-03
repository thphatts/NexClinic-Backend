package com.thphatts.clinicportal.modules.patient.entity;

import com.thphatts.clinicportal.modules.appointment.entity.Checkup;
import com.thphatts.clinicportal.modules.person.entity.Person;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "patients")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Patient {
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;

    @OneToOne(optional = false)
   @JoinColumn(name = "person_id", unique = true, nullable = false)
   private Person person;

   @Column(name = "patient_code", nullable = false, unique = true)
   private String patientCode;

   @Column(name = "blood_type")
   private String bloodType;

   @Column(name = "medical_history_summary", columnDefinition = "TEXT")
   private String medicalHistorySummary;

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

   @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
   @Builder.Default
   private List<Checkup> checkups = new ArrayList<>();

   @Transient public String getFullName() { return person == null ? null : person.getFullName(); }
   @Transient public String getCitizenId() { return person == null ? null : person.getCitizenId(); }
   @Transient public String getPhone() { return person == null ? null : person.getPhoneNumber(); }
   @Transient public String getEmail() { return person == null ? null : person.getEmail(); }
   @Transient public LocalDate getDob() { return person == null ? null : person.getBirthdate(); }
   @Transient public String getGender() { return person == null ? null : person.getGender(); }
   @Transient public String getAddress() { return person == null ? null : person.getAddress(); }
   @Transient public String getUserId() { return person == null || person.getUser() == null ? null : person.getUser().getId(); }

}
