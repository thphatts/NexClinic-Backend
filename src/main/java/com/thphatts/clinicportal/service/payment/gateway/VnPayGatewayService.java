package com.thphatts.clinicportal.service.payment.gateway;

import com.thphatts.clinicportal.common.HmacUtil;
import com.thphatts.clinicportal.common.VnPayUtil;
import com.thphatts.clinicportal.entity.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service("VNPAY")
@RequiredArgsConstructor
public class VnPayGatewayService implements PaymentGateway {
    @Value("${vnpay.secret-key}")
    private String secretKey;

    @Value("${vnpay.tmn-code}")
    private String tmnCode;

    @Value("${vnpay.pay-url}")
    private String payUrl;

    @Value("${vnpay.return-url}")
    private String returnUrl;

    @Override
    public String createPaymentUrl(Payment payment, String ipAddress) {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", tmnCode);
        params.put("vnp_Amount", payment.getAmount().multiply(BigDecimal.valueOf(100)).toBigInteger().toString());
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", payment.getOrderRef()); // mã đơn hàng duy nhất, VNPay sẽ echo lại mã này
        params.put("vnp_OrderInfo", "Thanh toan lich kham #" + payment.getAppointmentId());
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", returnUrl);
        params.put("vnp_IpAddr", ipAddress);
        params.put("vnp_CreateDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        // tính hash data (KHÔNG encode) rồi ký bằng hmacSHA512
        String hashData = VnPayUtil.buildQueryString(params, false);
        String secureHash = HmacUtil.hmacSHA512(secretKey, hashData);

        // tính query string (CÓ encode) để build URL cuối cùng
        String queryString = VnPayUtil.buildQueryString(params, true);

        // ghép URL hoàn chỉnh: payUrl + "?" + queryString + "&vnp_SecureHash="
        // + secureHash
        return payUrl + "?" + queryString + "&vnp_SecureHash=" + secureHash;
    }

    @Override
    public boolean verifySignature(Map<String, String> callbackParams) {
        Map<String, String> paramsCoppy = new HashMap<>(callbackParams);
        String receivedHash = paramsCoppy.remove("vnp_SecureHash");
        paramsCoppy.remove("vnp_SecureHashType");

        if (receivedHash == null) {
            log.warn("[VNPay] Callback thiếu vnp_SecureHash");
            return false;
        }

        String hashData = VnPayUtil.buildQueryString(paramsCoppy, false);
        String calculatedHash = HmacUtil.hmacSHA512(secretKey, hashData);

        boolean isValid = calculatedHash.equalsIgnoreCase(receivedHash);

        if (!isValid) {
            log.warn("[VNPay] Chữ ký không khớp — nghi ngờ dữ liệu bị can thiệp. orderRef={}",
                    callbackParams.get("vnp_TxnRef"));
        }
        return isValid;
    }

    @Override
    public PaymentCallbackResult parseCallback(Map<String, String> callbackParams) {
        boolean success = "00".equals(callbackParams.get("vnp_ResponseCode"));
        return new PaymentCallbackResult(
                callbackParams.get("vnp_TxnRef"),
                success,
                callbackParams.get("vnp_TransactionNo"),
                callbackParams.get("vnp_ResponseCode"));
    }
}
