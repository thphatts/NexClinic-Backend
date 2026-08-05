package com.thphatts.clinicportal.repository;

import com.thphatts.clinicportal.entity.Payment;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {
    // 1. Tìm Payment theo orderRef (dùng khi VNPay gọi IPN về, cần tra ngược lại
    // đúng giao dịch nào)
    Optional<Payment> findPaymentByOrderRef(String orderRef);

    // 2. Tìm Payment theo appointmentId (dùng khi cần kiểm tra 1 lịch hẹn đã có
    // payment chưa,tránh tạo trùng payment cho cùng 1 appointment)
    Optional<Payment> findPaymentByAppointmentId(Long appointmentId);

    // chống Idempotency
    @Modifying
    @Transactional
    @Query("UPDATE Payment p SET p.paymentStatus = 'SUCCESS', p.paidAt = :paidAt, "
            + "p.gatewayTransactionId = :txnId "
            + "WHERE p.orderRef = :orderRef AND p.paymentStatus = 'PENDING'")
    int markAsSuccessIfPending(
            @Param("orderRef") String orderRef,
            @Param("paidAt") LocalDateTime paidAt,
            @Param("txnId") String txnId);

    @Modifying
    @Transactional
    @Query("UPDATE Payment p SET p.paymentStatus = 'FAILED' "
            + "WHERE p.orderRef = :orderRef AND p.paymentStatus = 'PENDING'")
    int markAsFailedIfPending(@Param("orderRef") String orderRef);
}