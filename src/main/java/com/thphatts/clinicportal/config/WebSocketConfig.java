package com.thphatts.clinicportal.config;

import com.thphatts.clinicportal.config.security.WebSocketAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // In-memory broker cho các topic và user queue
        registry.enableSimpleBroker("/topic", "/queue");
        // Prefix cho @MessageMapping trong controller
        registry.setApplicationDestinationPrefixes("/app");
        // Prefix cho user-specific messages: /user/{userId}/queue/...
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(
                        "http://localhost:3000",
                        "http://localhost:5173",
                        "https://*.vercel.app",
                        "https://*.onrender.com",
                        frontendUrl
                )
                .withSockJS(); // fallback cho môi trường không hỗ trợ native WebSocket
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Xác thực JWT trong STOMP CONNECT frame
        registration.interceptors(webSocketAuthInterceptor);
    }
}
