package com.sejong.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class TestController {

    @GetMapping("/health")
    public String health() {
        return "OK";
    }

    @GetMapping("/error")
    public void testLoggingError(){
        log.error("CloudWatch 전송 테스트 - {}", System.currentTimeMillis());
    }
}
