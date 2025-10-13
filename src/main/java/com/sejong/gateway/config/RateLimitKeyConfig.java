package com.sejong.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.List;

@Configuration
public class RateLimitKeyConfig {

    private static final List<String> IP_HEADERS = List.of(
            "X-Forwarded-For",      // ALB/NLB/일반 프록시
            "CF-Connecting-IP",     // Cloudflare
            "True-Client-IP",       // Akamai 등
            "X-Real-IP"             // Nginx
    );

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.fromCallable(() -> {
            // 1) 프록시 헤더에서 우선 추출
            var headers = exchange.getRequest().getHeaders();
            for (String h : IP_HEADERS) {
                String val = headers.getFirst(h);
                if (val != null && !val.isBlank()) {
                    // X-Forwarded-For: "client, proxy1, proxy2" → 첫 번째가 실제 클라이언트
                    String ip = val.split(",")[0].trim();
                    return normalizeIp(ip);
                }
            }
            // 2) 없으면 remoteAddress 사용
            InetSocketAddress ra = exchange.getRequest().getRemoteAddress();
            if (ra != null && ra.getAddress() != null) {
                return normalizeIp(ra.getAddress().getHostAddress());
            }
            return "unknown-ip";
        });
    }

    private String normalizeIp(String ip) {
        // IPv6 zone id 제거 (예: fe80::1%eth0 → fe80::1)
        int zone = ip.indexOf('%');
        if (zone > 0) ip = ip.substring(0, zone);
        // 포트가 따라온 경우 대비 (드물지만)
        if (ip.contains(":") && count(ip, ':') == 1 && ip.matches(".+:[0-9]+$")) {
            ip = ip.substring(0, ip.lastIndexOf(':'));
        }
        return ip;
    }

    private long count(String s, char c) {
        return s.chars().filter(ch -> ch == c).count();
    }
}