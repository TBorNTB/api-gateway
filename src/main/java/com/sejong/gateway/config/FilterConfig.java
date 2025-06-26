package com.sejong.gateway.config;

import com.sejong.gateway.filter.auth.JwtAuthenticationFilter;
import com.sejong.gateway.security.JwtUtil;
import com.sejong.gateway.util.ErrorResponseUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    // YAML에서 JwtAuthentication 이름으로 사용하기 위한 빈 등록
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtUtil jwtUtil, ErrorResponseUtil errorResponseUtil) {
        return new JwtAuthenticationFilter(jwtUtil, errorResponseUtil);
    }
}
