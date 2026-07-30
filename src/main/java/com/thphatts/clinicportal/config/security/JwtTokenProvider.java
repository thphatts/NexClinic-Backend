package com.thphatts.clinicportal.config.security;

import com.thphatts.clinicportal.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret}") // tự động lấy giá trị từ file application và gán vào biến lúc start
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}") // 24 Hours
    private long jwtExpirationMs;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret); // Decoder.BASE64.decode giải mã chuỗi Base64 thành mảng byte gốc
        return Keys.hmacShaKeyFor(keyBytes); // Keys.hmacShaKeyFor(keyBytes) tạo ra khóa bí mật dùng cho thuật toán HMAC-SHA
                                            // thuật toán ký số đối xứng - cùng 1 khóa để ký và verify
    }

    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole() != null ? user.getRole().name() : "ROLE_PATIENT");
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .claims(claims)
                .subject(user.getUsername()) // thiết lập chủ thể của token - dùng để nhận diện token này của ai
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();  // đóng gói mọi thứ lại thành 1 chuỗi JWT hoàn chỉnh (dạng xxxxx.yyyyy.zzzzz)
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (MalformedJwtException ex) {
            log.error("Token JWT không hợp lệ: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Token JWT đã hết hạn: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Token JWT không được hỗ trợ: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("Chuỗi JWT rỗng: {}", ex.getMessage());
        }
        return false;
    }

    public long getJwtExpirationMs() {
        return jwtExpirationMs;
    }
}
