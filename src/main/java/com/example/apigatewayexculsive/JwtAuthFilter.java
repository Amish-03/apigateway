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

<<<<<<< Updated upstream
import java.nio.charset.StandardCharsets;
=======
import java.io.IOException;
import java.util.List;
>>>>>>> Stashed changes

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
<<<<<<< Updated upstream
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
=======
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
protected boolean shouldNotFilter(HttpServletRequest request) {
    // Always exempt OPTIONS preflight requests from JWT validation
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
        return true;
    }

    String path = request.getRequestURI();
    return path.startsWith("/api/auth/")
            || path.startsWith("/ws-notifications")
            || path.startsWith("/actuator/health")
            || path.startsWith("/api/notifications/email/send");
}

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {
>>>>>>> Stashed changes

        if (isExempt(request)) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
<<<<<<< Updated upstream
            return reject(exchange, "Missing or invalid Authorization header");
=======
            applyCorsHeaders(request, response);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Missing or invalid Authorization header\"}");
            return;
>>>>>>> Stashed changes
        }

        String token = authHeader.substring(7); // Strip "Bearer "

        if (!jwtUtil.validateToken(token)) {
<<<<<<< Updated upstream
            return reject(exchange, "Token is invalid or expired");
=======
            applyCorsHeaders(request, response);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Token is invalid or expired\"}");
            return;
>>>>>>> Stashed changes
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

    private void applyCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        List<String> allowedOrigins = List.of("http://localhost:5173", "http://172.19.23.52:5173");

        if (origin != null && allowedOrigins.contains(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Vary", "Origin");
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Allow-Headers", "authorization, content-type");
            response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,PATCH,OPTIONS");
        }
    }
}