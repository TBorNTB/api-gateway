package com.sejong.gateway;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/health")
    public String health() {
        return "OK";
    }

    @GetMapping("/error")
    public String testLoggingError(){
        throw new IllegalArgumentException();
    }
}
