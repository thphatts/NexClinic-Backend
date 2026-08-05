package com.thphatts.clinicportal.entity;

import com.thphatts.clinicportal.entity.enums.PaymentMethod;
import com.thphatts.clinicportal.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "appointment_id", nullable = false, unique = true)
    private Long appointmentId;

    //precision = tổng số chữ số có nghĩa (cả trước và sau dấu phẩy cộng lại).
    //scale = số chữ số sau dấu phẩy.
    @Column(nullable = false, precision = 15, scale = 0)
    private BigDecimal amount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    private String gatewayTransactionId;

    @Column(nullable = false, unique = true)
    private String orderRef;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime paidAt;

    @PrePersist
    protected void onCreate(){
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.paymentStatus == null) {
            this.paymentStatus = PaymentStatus.PENDING;
        }
    }
}
