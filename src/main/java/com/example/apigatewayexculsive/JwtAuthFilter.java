package com.example.apigatewayexculsive;

import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Runs once per request in the API Gateway.
 * Validates the Bearer JWT token from the Authorization header.
 *
 * This is a WebFilter rather than a GlobalFilter so it also covers requests that
 * do not match a gateway route - notably /actuator/**, which a GlobalFilter would
 * let through unauthenticated.
 *
 * Exempt paths/methods (no token required):
 *   - HTTP OPTIONS preflight requests
 *   - /api/auth/** - login and register endpoints
 *   - /actuator/health - liveness probe used by the container healthcheck
 *   - /ws-notifications/** - SockJS handshake; see below
 *
 * All other gateway routes require a valid, non-expired JWT.
 */
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class JwtAuthFilter implements WebFilter {

    private final JwtUtil jwtUtil;

    private boolean isExempt(ServerHttpRequest request) {
        // Always exempt OPTIONS preflight requests from JWT validation
        if (HttpMethod.OPTIONS.equals(request.getMethod())) {
            return true;
        }

        String path = request.getURI().getPath();
        if (path.startsWith("/api/auth/")) {
            return true;
        }

        // Only the health group - the other actuator endpoints stay behind the JWT
        // because they expose env, beans and config.
        if (path.equals("/actuator/health") || path.startsWith("/actuator/health/")) {
            return true;
        }

        // A browser cannot attach an Authorization header to a SockJS/WebSocket
        // handshake, so this hop is unauthenticated here and the token is instead
        // validated by the notification service from the STOMP CONNECT frame.
        return path.equals("/ws-notifications") || path.startsWith("/ws-notifications/");
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        if (isExempt(request)) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return reject(exchange, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7); // Strip "Bearer "

        if (!jwtUtil.validateToken(token)) {
            return reject(exchange, "Token is invalid or expired");
        }

        // Token is valid - forward to downstream service
        return chain.filter(exchange);
    }

    private Mono<Void> reject(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        byte[] body = ("{\"error\": \"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }
}