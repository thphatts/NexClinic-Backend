package com.thphatts.clinicportal.controller;


import com.thphatts.clinicportal.common.ApiResponse;
import com.thphatts.clinicportal.dto.request.CreatePaymentRequest;
import com.thphatts.clinicportal.dto.response.PaymentResponse;
import com.thphatts.clinicportal.entity.enums.PaymentMethod;
import com.thphatts.clinicportal.service.AppointmentService;
import com.thphatts.clinicportal.service.payment.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.access.prepost.PreAuthorize;
import java.util.Map;

@RestController
@RequestMapping("api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    private final AppointmentService appointmentService;

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'ROLE_PATIENT', 'PATIENT')")
    public ResponseEntity<ApiResponse<PaymentResponse>> create (@Valid @RequestBody CreatePaymentRequest request, HttpServletRequest httpRequest) {
        String ipAddress = httpRequest.getRemoteAddr();

        PaymentResponse response = paymentService.createPayment(request.getAppointmentId(), request.getPaymentMethod(),ipAddress);
        return ResponseEntity.ok(new ApiResponse<>(200, "Tạo giao dịch thành công", response));
    }

    @GetMapping("/vnpay-return")
    public ResponseEntity<String> vnpayReturn(@RequestParam Map<String,String> params){
        boolean success = "00".equals(params.get("vnp_ResponseCode"));
        return ResponseEntity.ok(success ? "Thanh toán thành công! Vui lòng chờ hệ thống xác nhận."
                : "Thanh toán thất bại hoặc bị hủy.");
    }
    @GetMapping("/vnpay-ipn")
    public ResponseEntity<Map<String, String>> vnpayIpn(@RequestParam Map<String, String> params) {
        boolean processed = paymentService.handleCallback(PaymentMethod.VNPAY, params);

        return ResponseEntity.ok(processed
                ? Map.of("RspCode", "00", "Message", "Confirm Success")
                : Map.of("RspCode", "97", "Message", "Invalid Signature"));
    }
}
