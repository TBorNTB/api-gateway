package com.sejong.gateway.filter.auth;

import com.sejong.gateway.security.JwtUtil;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

// GatewayFilter - JWT 검증 후 헤더 추가
@Component
@Slf4j
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            // Authorization 헤더에서 토큰 추출
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                if (config.isLogEnabled()) {
                    log.warn("Authorization 헤더가 없거나 형식이 잘못됨 - Path: {}", path);
                }
                return handleUnauthorized(exchange, "Authorization 헤더가 필요합니다");
            }

            String token = authHeader.substring(7);

            try {
                // JWT 토큰 검증
                if (!jwtUtil.validateToken(token)) {
                    if (config.isLogEnabled()) {
                        log.warn("JWT 토큰 검증 실패 - Path: {}", path);
                    }
                    return handleUnauthorized(exchange, "유효하지 않은 토큰입니다");
                }

                // 토큰에서 사용자 정보 추출
                String userId = jwtUtil.getUserIdFromToken(token);
                String userRole = jwtUtil.getUserRoleFromToken(token);
//                String userEmail = jwtUtil.getUserEmailFromToken(token);

                // 백엔드 서비스로 사용자 정보 전달을 위한 헤더 추가
                ServerHttpRequest modifiedRequest = request.mutate()
                        .header("X-User-Id", userId)
                        .header("X-User-Role", userRole)
//                        .header("X-User-Email", userEmail)
                        .build();

                if (config.isLogEnabled()) {
                    log.debug("JWT 인증 성공 - UserId: {}, Role: {}, Path: {}", userId, userRole, path);
                }

                return chain.filter(exchange.mutate().request(modifiedRequest).build());

            } catch (Exception e) {
                log.error("JWT 토큰 검증 중 오류 발생 - Path: {}, Error: {}", path, e.getMessage());
                return handleUnauthorized(exchange, "토큰 처리 중 오류가 발생했습니다");
            }
        };
    }

    /**
     * 인증 실패 시 응답 처리
     */
    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        String body = """
            {
                "error": "UNAUTHORIZED",
                "message": "%s",
                "timestamp": "%s",
                "path": "%s"
            }
            """.formatted(message, Instant.now(), exchange.getRequest().getURI().getPath());

        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    /**
     * 설정 클래스 - 확장 가능한 구조
     */
    @Data
    public static class Config {
        private boolean logEnabled = true;
    }
}