package com.sejong.gateway.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class ErrorResponseUtil {
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * JWT 인증 실패 응답
     */
    public Mono<Void> writeUnauthorizedResponse(ServerWebExchange exchange, String message) {
        return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message);
    }

    /**
     * 내부 서버 오류 응답
     */
    public Mono<Void> writeInternalServerErrorResponse(ServerWebExchange exchange, String message) {
        return writeErrorResponse(exchange, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", message);
    }

    /**
     * 잘못된 요청 응답
     */
    public Mono<Void> writeBadRequestResponse(ServerWebExchange exchange, String message) {
        return writeErrorResponse(exchange, HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }

    private Mono<Void> writeErrorResponse(ServerWebExchange exchange,
                                         HttpStatus status,
                                         String errorCode,
                                         String message) {
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return response.setComplete();
        }

        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse errorResponse = new ErrorResponse(
                errorCode,
                message,
                Instant.now().toString(),
                exchange.getRequest().getURI().getPath()
        );

        try {
            String jsonBody = objectMapper.writeValueAsString(errorResponse);
            DataBuffer buffer = response.bufferFactory().wrap(jsonBody.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Error response JSON 생성 실패", e);
            return response.setComplete();
        }
    }

    @Data
    @AllArgsConstructor
    public static class ErrorResponse {
        private String error;
        private String message;
        private String timestamp;
        private String path;
    }
}
