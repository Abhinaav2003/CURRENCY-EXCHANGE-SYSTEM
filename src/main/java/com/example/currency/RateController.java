package com.example.currency;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api")
public class RateController {
    private final RateService service;

    public RateController(RateService service) { this.service = service; }

    /** GET /api/rates : latest rates (base USD) */
    @GetMapping("/rates")
    public Map<String, Object> rates() { return service.latest(); }

    /** GET /api/convert?from=USD&to=INR&amount=100 */
    @GetMapping("/convert")
    public Map<String, Object> convert(@RequestParam String from, @RequestParam String to,
                                       @RequestParam(defaultValue = "1") BigDecimal amount) {
        return service.convert(from, to, amount);
    }

    /** GET /api/history/INR?days=30 : stored daily values for one currency */
    @GetMapping("/history/{currency}")
    public List<Map<String, Object>> history(@PathVariable String currency,
                                             @RequestParam(defaultValue = "30") int days) {
        return service.history(currency, days);
    }

    /** POST /api/refresh : force an update now */
    @PostMapping("/refresh")
    public Map<String, Object> refresh() { return Map.of("newRecords", service.refresh()); }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> notFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }
}