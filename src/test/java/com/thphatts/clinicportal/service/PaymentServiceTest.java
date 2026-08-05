package com.thphatts.clinicportal.service;

import com.thphatts.clinicportal.dto.response.PaymentResponse;
import com.thphatts.clinicportal.entity.Appointment;
import com.thphatts.clinicportal.entity.Payment;
import com.thphatts.clinicportal.entity.enums.AppointmentStatus;
import com.thphatts.clinicportal.entity.enums.PaymentMethod;
import com.thphatts.clinicportal.entity.enums.PaymentStatus;
import com.thphatts.clinicportal.repository.AppointmentRepository;
import com.thphatts.clinicportal.repository.PaymentRepository;
import com.thphatts.clinicportal.service.payment.PaymentService;
import com.thphatts.clinicportal.service.payment.gateway.PaymentCallbackResult;
import com.thphatts.clinicportal.service.payment.gateway.PaymentGateway;
import com.thphatts.clinicportal.service.payment.gateway.PaymentGatewayFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Unit Tests")
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentGatewayFactory paymentGatewayFactory;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private PaymentGateway mockGateway;

    @InjectMocks
    private PaymentService paymentService;

    private Appointment mockAppointment;
    private Payment mockPayment;
    private final String CLIENT_IP = "127.0.0.1";

    @BeforeEach
    void setUp() {
        mockAppointment = Appointment.builder()
                .id(1L)
                .appointmentDate(LocalDate.now().plusDays(1))
                .timeSlot("09:00 - 09:30")
                .status(AppointmentStatus.PENDING)
                .amount(BigDecimal.valueOf(200000))
                .build();

        mockPayment = Payment.builder()
                .id("payment-uuid-001")   // Payment.id là String (UUID)
                .appointmentId(1L)
                .amount(BigDecimal.valueOf(200000))
                .paymentMethod(PaymentMethod.VNPAY)
                .paymentStatus(PaymentStatus.PENDING)
                .orderRef("PAY123456")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // =========================================================
    // CREATE PAYMENT TESTS
    // =========================================================
    @Nested
    @DisplayName("Tạo giao dịch thanh toán (Create Payment)")
    class CreatePaymentTests {

        @Test
        @DisplayName("Tạo Payment thành công - trả về URL thanh toán VNPay")
        void createPayment_Success_ReturnsPaymentUrl() {
            // Arrange
            when(paymentRepository.findPaymentByAppointmentId(1L)).thenReturn(Optional.empty());
            when(appointmentRepository.findById(1L)).thenReturn(Optional.of(mockAppointment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);
            when(paymentGatewayFactory.getGateway(PaymentMethod.VNPAY)).thenReturn(mockGateway);
            when(mockGateway.createPaymentUrl(any(Payment.class), eq(CLIENT_IP)))
                    .thenReturn("https://sandbox.vnpay.vn/pay?vnp_TxnRef=PAY123456");

            // Act
            PaymentResponse result = paymentService.createPayment(1L, PaymentMethod.VNPAY, CLIENT_IP);

            // Assert
            assertNotNull(result);
            assertNotNull(result.getPaymentUrl());
            assertTrue(result.getPaymentUrl().contains("vnpay"));
            assertEquals(PaymentStatus.PENDING, result.getStatus());
            assertEquals(BigDecimal.valueOf(200000), result.getAmount());

            verify(paymentRepository, times(1)).save(any(Payment.class));
            verify(mockGateway, times(1)).createPaymentUrl(any(Payment.class), eq(CLIENT_IP));
        }

        @Test
        @DisplayName("Tạo Payment thất bại - Lịch hẹn đã có giao dịch PENDING")
        void createPayment_Fail_DuplicatePendingPayment() {
            // Arrange - đã có payment PENDING cho appointment này
            Payment existingPendingPayment = Payment.builder()
                    .id("payment-uuid-existing")
                    .appointmentId(1L)
                    .paymentStatus(PaymentStatus.PENDING)
                    .build();
            when(paymentRepository.findPaymentByAppointmentId(1L))
                    .thenReturn(Optional.of(existingPendingPayment));

            // Act & Assert
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> paymentService.createPayment(1L, PaymentMethod.VNPAY, CLIENT_IP));

            assertTrue(ex.getMessage().contains("đã có giao dịch thanh toán đang xử lý"));
            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Tạo Payment thành công - Khi payment cũ đã FAILED (cho phép tạo lại)")
        void createPayment_Success_WhenPreviousPaymentFailed() {
            // Arrange - payment cũ đã FAILED → cho phép tạo mới
            Payment failedPayment = Payment.builder()
                    .id("payment-uuid-failed")
                    .appointmentId(1L)
                    .paymentStatus(PaymentStatus.FAILED)
                    .build();
            when(paymentRepository.findPaymentByAppointmentId(1L))
                    .thenReturn(Optional.of(failedPayment));
            when(appointmentRepository.findById(1L)).thenReturn(Optional.of(mockAppointment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);
            when(paymentGatewayFactory.getGateway(PaymentMethod.VNPAY)).thenReturn(mockGateway);
            when(mockGateway.createPaymentUrl(any(Payment.class), eq(CLIENT_IP)))
                    .thenReturn("https://sandbox.vnpay.vn/pay?vnp_TxnRef=PAY_NEW");

            // Act
            PaymentResponse result = paymentService.createPayment(1L, PaymentMethod.VNPAY, CLIENT_IP);

            // Assert
            assertNotNull(result);
            verify(paymentRepository, times(1)).save(any(Payment.class));
        }

        @Test
        @DisplayName("Tạo Payment thất bại - Không tìm thấy Lịch hẹn")
        void createPayment_Fail_AppointmentNotFound() {
            // Arrange
            when(paymentRepository.findPaymentByAppointmentId(999L)).thenReturn(Optional.empty());
            when(appointmentRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> paymentService.createPayment(999L, PaymentMethod.VNPAY, CLIENT_IP));

            assertTrue(ex.getMessage().contains("Không tìm thấy cuộc hẹn"));
        }
    }

    // =========================================================
    // HANDLE CALLBACK TESTS
    // =========================================================
    @Nested
    @DisplayName("Xử lý Callback từ VNPay (Handle Callback)")
    class HandleCallbackTests {

        private Map<String, String> callbackParams;

        @BeforeEach
        void setUpCallback() {
            callbackParams = Map.of(
                    "vnp_TxnRef", "PAY123456",
                    "vnp_ResponseCode", "00",
                    "vnp_TransactionNo", "VNP_TX_001",
                    "vnp_SecureHash", "valid_hash"
            );
        }

        @Test
        @DisplayName("Callback thành công - Cập nhật Payment SUCCESS + Appointment CONFIRMED")
        void handleCallback_Success_UpdatesPaymentAndAppointment() {
            // Arrange
            when(paymentGatewayFactory.getGateway(PaymentMethod.VNPAY)).thenReturn(mockGateway);
            when(mockGateway.verifySignature(callbackParams)).thenReturn(true);

            // PaymentCallbackResult(orderRef, success, gatewayTransactionId, rawResponseCode)
            PaymentCallbackResult successResult = new PaymentCallbackResult(
                    "PAY123456", true, "VNP_TX_001", "00"
            );
            when(mockGateway.parseCallback(callbackParams)).thenReturn(successResult);
            when(paymentRepository.markAsSuccessIfPending(eq("PAY123456"), any(LocalDateTime.class), eq("VNP_TX_001")))
                    .thenReturn(1); // 1 row updated = thành công

            when(paymentRepository.findPaymentByOrderRef("PAY123456"))
                    .thenReturn(Optional.of(mockPayment));
            when(appointmentRepository.findById(1L)).thenReturn(Optional.of(mockAppointment));

            // Act
            boolean result = paymentService.handleCallback(PaymentMethod.VNPAY, callbackParams);

            // Assert
            assertTrue(result);
            verify(appointmentRepository, times(1)).save(mockAppointment);
            assertEquals(AppointmentStatus.CONFIRMED, mockAppointment.getStatus());
        }

        @Test
        @DisplayName("Callback bị từ chối - Chữ ký không hợp lệ (HMAC mismatch)")
        void handleCallback_Rejected_InvalidSignature() {
            // Arrange
            when(paymentGatewayFactory.getGateway(PaymentMethod.VNPAY)).thenReturn(mockGateway);
            when(mockGateway.verifySignature(callbackParams)).thenReturn(false);

            // Act
            boolean result = paymentService.handleCallback(PaymentMethod.VNPAY, callbackParams);

            // Assert
            assertFalse(result);
            verify(paymentRepository, never()).markAsSuccessIfPending(any(), any(), any());
        }

        @Test
        @DisplayName("Callback giao dịch thất bại - Đánh dấu Payment FAILED")
        void handleCallback_PaymentFailed_MarkAsFailedIfPending() {
            // Arrange
            when(paymentGatewayFactory.getGateway(PaymentMethod.VNPAY)).thenReturn(mockGateway);
            when(mockGateway.verifySignature(callbackParams)).thenReturn(true);

            PaymentCallbackResult failedResult = new PaymentCallbackResult(
                    "PAY123456", false, null, "24" // 24 = khách hủy giao dịch
            );
            when(mockGateway.parseCallback(callbackParams)).thenReturn(failedResult);

            // Act
            boolean result = paymentService.handleCallback(PaymentMethod.VNPAY, callbackParams);

            // Assert
            assertTrue(result); // Trả về true vì đã xử lý hợp lệ (dù giao dịch thất bại)
            verify(paymentRepository, times(1)).markAsFailedIfPending("PAY123456");
            verify(appointmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Callback idempotent - Giao dịch đã xử lý trước đó → bỏ qua")
        void handleCallback_Idempotent_SkipsAlreadyProcessed() {
            // Arrange
            when(paymentGatewayFactory.getGateway(PaymentMethod.VNPAY)).thenReturn(mockGateway);
            when(mockGateway.verifySignature(callbackParams)).thenReturn(true);

            PaymentCallbackResult successResult = new PaymentCallbackResult(
                    "PAY123456", true, "VNP_TX_001", "00"
            );
            when(mockGateway.parseCallback(callbackParams)).thenReturn(successResult);
            // rowsUpdated = 0 → giao dịch đã được xử lý bởi callback trước đó
            when(paymentRepository.markAsSuccessIfPending(eq("PAY123456"), any(LocalDateTime.class), eq("VNP_TX_001")))
                    .thenReturn(0);

            // Act
            boolean result = paymentService.handleCallback(PaymentMethod.VNPAY, callbackParams);

            // Assert
            assertTrue(result);
            // Không được gọi appointmentRepository.save vì đây là callback trùng lặp
            verify(appointmentRepository, never()).save(any());
            verify(paymentRepository, never()).findPaymentByOrderRef(any());
        }
    }
}
