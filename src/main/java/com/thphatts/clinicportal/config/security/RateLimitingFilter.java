package com.thphatts.clinicportal.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thphatts.clinicportal.common.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_MINUTE = 15;
    private final Map<String, RequestTracker> ipTrackerMap = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static class RequestTracker {
        long startTimeMs;
        int count;

        RequestTracker(long startTimeMs) {
            this.startTimeMs = startTimeMs;
            this.count = 1;
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path.endsWith("/auth/login") || path.endsWith("/auth/register")) {
            String clientIp = getClientIp(request);
            long now = System.currentTimeMillis();

            RequestTracker tracker = ipTrackerMap.compute(clientIp, (ip, tr) -> {
                if (tr == null || (now - tr.startTimeMs) > 60000) {
                    return new RequestTracker(now);
                } else {
                    tr.count++;
                    return tr;
                }
            });

            if (tracker.count > MAX_REQUESTS_PER_MINUTE) {
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
