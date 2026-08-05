package com.thphatts.clinicportal.service.payment;

import com.thphatts.clinicportal.dto.response.PaymentResponse;
import com.thphatts.clinicportal.entity.Appointment;
import com.thphatts.clinicportal.entity.Payment;
import com.thphatts.clinicportal.entity.enums.AppointmentStatus;
import com.thphatts.clinicportal.entity.enums.PaymentMethod;
import com.thphatts.clinicportal.entity.enums.PaymentStatus;
import com.thphatts.clinicportal.repository.AppointmentRepository;
import com.thphatts.clinicportal.repository.PaymentRepository;
import com.thphatts.clinicportal.service.payment.gateway.PaymentCallbackResult;
import com.thphatts.clinicportal.service.payment.gateway.PaymentGateway;
import com.thphatts.clinicportal.service.payment.gateway.PaymentGatewayFactory;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;


@RequiredArgsConstructor
@Service
@Slf4j
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentGatewayFactory paymentGatewayFactory;
    private final AppointmentRepository appointmentRepository;

    @Transactional
    public PaymentResponse createPayment(Long appointmentId, PaymentMethod method, String ipAddress){
        // Chặn tạo trùng payment cho cùng 1 appointment
        paymentRepository.findPaymentByAppointmentId(appointmentId).ifPresent(existing -> {
            if (existing.getPaymentStatus() != PaymentStatus.FAILED
                && existing.getPaymentStatus() != PaymentStatus.EXPIRED){
                throw new IllegalStateException("Lịch hẹn này đã có giao dịch thanh toán đang xử lý hoặc đã hoàn tất");
            }
        });

        String orderRef = "PAY" + System.currentTimeMillis() + UUID.randomUUID().toString();
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cuộc hẹn"));
        BigDecimal amount = appointment.getAmount();
        Payment payment = Payment.builder()
                .appointmentId(appointmentId)
                .amount(amount)
                .paymentMethod(method)
                .paymentStatus(PaymentStatus.PENDING)
                .orderRef(orderRef)
                .createdAt(LocalDateTime.now())
                .build();

        payment = paymentRepository.save(payment);

        PaymentGateway gateway = paymentGatewayFactory.getGateway(method);
        String paymentUrl = gateway.createPaymentUrl(payment, ipAddress);

        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .orderRef(payment.getOrderRef())
                .amount(payment.getAmount())
                .status(payment.getPaymentStatus())
                .paymentUrl(paymentUrl)
                .build();
    }

    // Xử lí callback (return url và ipn webhook)
    @Transactional
    public boolean handleCallback(PaymentMethod method, Map<String, String> callbackParams){
        PaymentGateway gateway = paymentGatewayFactory.getGateway(method);

        // kiểm tra chữ ký trước tiên
        // nếu sai - từ chối
        if(!gateway.verifySignature(callbackParams)){
            log.warn("[Payment] Callback có chữ ký không hợp lệ, từ chối xử lý");
            return false;
        }
        PaymentCallbackResult result = gateway.parseCallback(callbackParams);

        if(!result.success()) {
            log.info("[Payment] Giao dịch {} thất bại, mã lỗi: {}", result.orderRef(), result.rawResponseCode());
            paymentRepository.markAsFailedIfPending(result.orderRef());
            return true;
        }

        // idempotency v2
        // thay vì dùng kiểu check-then-act thì sẽ dùng 1 câu update duy nhất có đkien where trong sql
        // đảm bảo rằng database check-then-act chạy liền mạch
        // và không bị 2 request chen ngang

        int rowsUpdated = paymentRepository.markAsSuccessIfPending(
                result.orderRef(),
                LocalDateTime.now(),
                result.gatewayTransactionId()
        );

        if(rowsUpdated == 1) {
            // chỉ request nào đổi status từ pending sang success mới thỏa mãn dùng vnpay gọi nhiều lần cùng giao dịch
            log.info("[Payment] Xác nhận thanh toán thành công cho orderRef={}", result.orderRef());
            Payment payment = paymentRepository.findPaymentByOrderRef(result.orderRef())
                    .orElseThrow(() -> new IllegalStateException("Không tìm thấy payment sau khi update"));

            // Cập nhật trạng thái Appointment sang CONFIRMED
            appointmentRepository.findById(payment.getAppointmentId()).ifPresent(appointment -> {
                appointment.setStatus(AppointmentStatus.CONFIRMED);
                appointmentRepository.save(appointment);
                log.info("[Payment] Đã cập nhật trạng thái Appointment id={} thành CONFIRMED", appointment.getId());
            });
        }
        else {
            // rowsUpdated == 0 : có 2 khả năng
            // 1. orderRef không tồn tại
            // 2. Giao dịch này đã được xử lý bởi 1 lần callback trước đó
            log.info("[Payment] orderRef={} đã được xử lý trước đó hoặc không tồn tại, bỏ qua (idempotent)",
                    result.orderRef());
        }
        return true;
    }
}
