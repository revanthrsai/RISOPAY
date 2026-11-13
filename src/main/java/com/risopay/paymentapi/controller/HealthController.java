
package com.risopay.paymentapi.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/* Treat this class as a REST API controller.
Its methods can handle HTTP requests, and
their return values should be sent directly in the HTTP response.
 */
@RestController

/*This defines the common URL prefix for the controller's endpoints.*/
@RequestMapping("/api/v1")
public class HealthController {

    /*When an HTTP GET request arrives at /api/v1/health,
    execute the method associated with this mapping.*/
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "application", "RISOPAY",
                "status", "UP",
                "message", "RISOPAY payment API is running"
        );
    }
}
