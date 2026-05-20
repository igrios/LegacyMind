package com.ignacio.legacyanalyzer.infrastructure.adapters.input.controller;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
@CrossOrigin(origins = "*")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {

        Map<String, Object> response = new HashMap<>();

        response.put("status", "UP");
        response.put("service", "LegacyMind API");
        response.put("timestamp", Instant.now());

        return ResponseEntity.ok(response);
    }
}
