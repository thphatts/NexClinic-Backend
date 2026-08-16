package com.thphatts.clinicportal.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thphatts.clinicportal.common.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int MAX_REQUESTS_PER_MINUTE = 15;
    private static final String KEY_PREFIX = "ratelimit:login:";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path.endsWith("/auth/login") || path.endsWith("/auth/register")) {
            String clientIp = getClientIp(request);
            String key = KEY_PREFIX + clientIp;

            // INCR là atomic ở tầng Redis — an toàn dù nhiều instance Render
            // cùng gọi song song, không cần đồng bộ hoá thủ công như ConcurrentHashMap cũ.
            Long count = redisTemplate.opsForValue().increment(key);

            // Chỉ set TTL ở lần tăng ĐẦU TIÊN trong cửa sổ 1 phút,
            // tránh mỗi request lại reset lại đồng hồ đếm ngược.
            if (count != null && count == 1L) {
                redisTemplate.expire(key, Duration.ofMinutes(1));
            }

            if (count != null && count > MAX_REQUESTS_PER_MINUTE) {
                log.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, path);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                ApiResponse<Void> apiResponse = ApiResponse.error(429, "Quá nhiều yêu cầu đăng nhập. Vui lòng thử lại sau 1 phút.");
                response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}