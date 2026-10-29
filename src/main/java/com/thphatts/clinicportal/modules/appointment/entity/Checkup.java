package com.thphatts.clinicportal.modules.appointment.entity;

import com.thphatts.clinicportal.modules.patient.entity.Patient;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "checkups")
public class Checkup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String diagnoses;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private Patient patient;
}
