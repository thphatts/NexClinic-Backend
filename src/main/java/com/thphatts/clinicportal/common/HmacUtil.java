package com.thphatts.clinicportal.common;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

public class HmacUtil {

    public static String hmacSHA512(String key, String data) {
        try {
            // TODO 1: Tạo đối tượng SecretKeySpec từ chuỗi `key`
            // Gợi ý cú pháp: new SecretKeySpec(byte[] khóa, "tên thuật toán")
            // - byte[] khóa: chuyển chuỗi `key` thành mảng byte bằng key.getBytes(StandardCharsets.UTF_8)
            // - tên thuật toán: "HmacSHA512" (chú ý viết hoa/thường đúng, Java phân biệt)
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8),"HmacSHA512"); // <-- bạn thay dòng này

            // TODO 2: Tạo đối tượng Mac bằng thuật toán tương ứng
            // Gợi ý: Mac.getInstance("HmacSHA512") — đây là factory method, giống Jwts.parserBuilder() bạn đã quen
            Mac mac =Mac.getInstance("HmacSHA512"); // <-- bạn thay dòng này

            // TODO 3: Gán khóa vào Mac trước khi dùng
            // Gợi ý: gọi phương thức init() trên đối tượng mac, truyền vào secretKeySpec
            mac.init(secretKeySpec);
            // TODO 4: Thực hiện tính toán chữ ký trên dữ liệu `data`
            // Gợi ý: mac.doFinal(byte[] dữ liệu) trả về mảng byte kết quả (đây chính là chữ ký dạng byte thô)
            byte[] hashBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));// <-- bạn thay dòng này
            // TODO 5: Chuyển mảng byte kết quả thành chuỗi hex (dạng chữ, dễ nhìn/dễ so sánh)
            // Gợi ý: dùng HexFormat.of().formatHex(hashBytes) — bạn đã dùng class này ở RefreshTokenService rồi
            return HexFormat.of().formatHex(hashBytes); // <-- bạn thay dòng này

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Lỗi khi tạo chữ ký HMAC", e);
        }
    }
}
