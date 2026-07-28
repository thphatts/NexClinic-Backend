package com.thphatts.clinicportal.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> home() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "AI-Powered Clinic Portal API",
                "documentation", "/swagger-ui/index.html",
                "health", "OK"
        ));
    }
}
