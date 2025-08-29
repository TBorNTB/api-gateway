package com.sejong.gateway.filter.auth;

import com.sejong.gateway.security.JwtUtil;
import com.sejong.gateway.util.ErrorResponseUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

// GatewayFilter - JWT 검증 후 헤더 추가
@Component
@Slf4j
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private final JwtUtil jwtUtil;
    private final ErrorResponseUtil errorResponseUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, ErrorResponseUtil errorResponseUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
        this.errorResponseUtil = errorResponseUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            // Authorization 헤더에서 Bearer 토큰 추출
            String authHeader = request.getHeaders().getFirst("Authorization");
            
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                if (config.isLogEnabled()) {
                    log.warn("Authorization Bearer 토큰이 없음 - Path: {}", path);
                }
                return errorResponseUtil.writeBadRequestResponse(exchange, "Authorization Bearer 토큰이 필요합니다");
            }

            String token = authHeader.substring(7); // "Bearer " 제거

            try {
                // JWT 토큰 검증
                if (!jwtUtil.validateToken(token)) {
                    if (config.isLogEnabled()) {
                        log.warn("JWT 토큰 검증 실패 - Path: {}", path);
                    }
                    return errorResponseUtil.writeUnauthorizedResponse(exchange, "유효하지 않은 토큰입니다");
                }

                // 토큰에서 사용자 정보 추출
                String username = jwtUtil.getUsernameFromToken(token);
                String userRole = jwtUtil.getUserRoleFromToken(token);
//                String userEmail = jwtUtil.getUserEmailFromToken(token);

                // 백엔드 서비스로 사용자 정보 전달을 위한 헤더 추가
                ServerHttpRequest modifiedRequest = request.mutate()
                        .header("X-User-Id", username)
                        .header("X-User-Role", userRole)
//                        .header("X-User-Email", userEmail)
                        .build();

                if (config.isLogEnabled()) {
                    log.debug("JWT 인증 성공 - UserId: {}, Role: {}, Path: {}", username, userRole, path);
                }

                return chain.filter(exchange.mutate().request(modifiedRequest).build());

            } catch (Exception e) {
                log.error("JWT 토큰 검증 중 오류 발생 - Path: {}, Error: {}", path, e.getMessage());
                return errorResponseUtil.writeInternalServerErrorResponse(exchange, "토큰 처리 중 오류가 발생했습니다");
            }
        };
    }


    /**
     * 설정 클래스 - 확장 가능한 구조
     */
    @Data
    public static class Config {
        private boolean logEnabled = true;
    }
}