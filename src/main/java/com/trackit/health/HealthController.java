package com.trackit.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/")
    public String home() {
        return "TrackIt Running 🚀";
    }

    @GetMapping({ "/health", "/api/v1/health" })
    public String health() {
        return "OK";
    }
}
