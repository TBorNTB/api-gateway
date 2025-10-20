package com.sejong.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.regex.Pattern;

@Configuration
@Slf4j
public class RateLimitConfig {

    private static final Pattern COMMA_SPLIT = Pattern.compile("\\s*,\\s*");

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.fromSupplier(() -> {
            String key = resolveClientIp(exchange);
            log.info("[RateLimit] resolved key = {}", key);
            return key;
        });
    }

    private String resolveClientIp(ServerWebExchange exchange) {
        // 1) X-Forwarded-For: 첫 번째 유효 IP를 사용
        String xff = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (notBlank(xff)) {
            for (String part : COMMA_SPLIT.split(xff)) {
                String ip = normalizeIp(part.trim());
                if (isValidIp(ip)) return ip;
            }
        }

        // 2) RFC 7239 Forwarded: for=<ip> 파싱
        String fwd = exchange.getRequest().getHeaders().getFirst("Forwarded");
        if (notBlank(fwd)) {
            // ex) Forwarded: for=192.0.2.60;proto=http;by=203.0.113.43, for="[2001:db8::1]"
            for (String token : COMMA_SPLIT.split(fwd)) {
                int idx = token.toLowerCase().indexOf("for=");
                if (idx >= 0) {
                    String ip = token.substring(idx + 4).trim(); // after "for="
                    // 따옴표 제거
                    if (ip.startsWith("\"") && ip.endsWith("\"") && ip.length() >= 2) {
                        ip = ip.substring(1, ip.length() - 1);
                    }
                    // [IPv6]:port → IPv6
                    if (ip.startsWith("[")) {
                        int end = ip.indexOf(']');
                        if (end > 0) ip = ip.substring(1, end);
                    } else {
                        // IPv4:port 형태면 포트 제거 (IPv6는 위에서 처리)
                        int colon = ip.indexOf(':');
                        if (colon > 0 && ip.indexOf('.') != -1) {
                            ip = ip.substring(0, colon);
                        }
                    }
                    ip = normalizeIp(ip);
                    if (isValidIp(ip)) return ip;
                }
            }
        }

        // 3) 최후: remoteAddress
        var addr = exchange.getRequest().getRemoteAddress();
        if (addr != null && addr.getAddress() != null) {
            String ip = normalizeIp(addr.getAddress().getHostAddress());
            if (isValidIp(ip)) return ip;
        }
        return "unknown";
    }

    private boolean notBlank(String s) { return s != null && !s.isBlank(); }

    private boolean isValidIp(String ip) {
        if (!notBlank(ip)) return false;
        if ("unknown".equalsIgnoreCase(ip)) return false;
        return true;
    }

    /** IPv6 루프백/zone-id/공백 등 정리 */
    private String normalizeIp(String ip) {
        if (!notBlank(ip)) return ip;
        ip = ip.trim();

        // zone-id 제거 (fe80::1%eth0 → fe80::1)
        int zone = ip.indexOf('%');
        if (zone > 0) ip = ip.substring(0, zone);

        // IPv6 루프백 → IPv4 루프백으로 통일 (키 정규화 용이)
        if ("::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) return "127.0.0.1";

        return ip;
    }
}