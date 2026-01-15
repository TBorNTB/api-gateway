package com.sejong.gateway.filter.global;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class GlobalLoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        long startTime = System.currentTimeMillis();
        String path = request.getURI().getPath();
        String method = request.getMethod().name();
        log.info("CookieHeader={}", exchange.getRequest().getHeaders().getFirst("Cookie"));
        log.info(">>>> Request PRE-Filter: [{}] {} - ID: {}", method, path, request.getId());

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            int statusCode = response.getStatusCode() != null ?
                    response.getStatusCode().value() : 0;

            log.info("<<<< Response POST-Filter: [{}] {} - Status: {}, Duration: {}ms",
                    method, path, statusCode, duration);
        }));
    }

    @Override
    public int getOrder() {
        // GlobalFilter들 중에서 가장 먼저 실행되도록 높은 우선순위 부여 (-2, -1보다 먼저)
        return -2;
    }
}
