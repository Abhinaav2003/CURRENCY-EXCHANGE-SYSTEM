package com.example.currency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class RateService 
{
    private static final Logger log = LoggerFactory.getLogger(RateService.class);

    record ApiResponse(String result, Map<String, BigDecimal> rates) {}

    private final ExchangeRateRepository repo;
    private final RestClient client = RestClient.create();
    private final String apiUrl;

    public RateService(ExchangeRateRepository repo, @Value("${rates.api-url}") String apiUrl) {
        this.repo = repo;
        this.apiUrl = apiUrl;
    }

    /** Fetch fresh rates once a day (00:30 UTC). */
    @Scheduled(cron = "${rates.cron}", zone = "UTC")
    public void scheduledRefresh() {
        try { refresh(); } catch (Exception e) { log.error("Daily refresh failed", e); }
    }

    /** Load rates when the app boots (free hosts restart/sleep often). */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        try { ensureFresh(); } catch (Exception e) { log.error("Startup refresh failed", e); }
    }

    @Transactional
    public int refresh() {
        ApiResponse res = client.get().uri(apiUrl).retrieve().body(ApiResponse.class);
        if (res == null || res.rates() == null || res.rates().isEmpty()) {
            throw new IllegalStateException("Rates API returned no data");
        }
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        int saved = 0;
        for (var e : res.rates().entrySet()) {
            ExchangeRate r = repo.findByCurrencyAndRateDate(e.getKey(), today).orElse(null);
            if (r == null) { repo.save(new ExchangeRate(e.getKey(), e.getValue(), today)); saved++; }
        }
        log.info("Rates refreshed for {} ({} new)", today, saved);
        return saved;
    }

    /** If the newest data is older than today, refresh (covers missed cron runs). */
    public void ensureFresh() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        boolean stale = repo.findTopByOrderByRateDateDesc()
                .map(r -> r.getRateDate().isBefore(today)).orElse(true);
        if (stale) refresh();
    }

    public Map<String, Object> latest() {
        ensureFreshQuietly();
        ExchangeRate newest = repo.findTopByOrderByRateDateDesc()
                .orElseThrow(() -> new NoSuchElementException("No rates available yet"));
        Map<String, BigDecimal> rates = new TreeMap<>();
        repo.findByRateDate(newest.getRateDate()).forEach(r -> rates.put(r.getCurrency(), r.getRate()));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("base", "USD");
        out.put("date", newest.getRateDate());
        out.put("rates", rates);
        return out;
    }

    public Map<String, Object> convert(String from, String to, BigDecimal amount) {
        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> rates = (Map<String, BigDecimal>) latest().get("rates");
        from = from.toUpperCase(); to = to.toUpperCase();
        BigDecimal rf = rates.get(from), rt = rates.get(to);
        if (rf == null) throw new NoSuchElementException("Unknown currency: " + from);
        if (rt == null) throw new NoSuchElementException("Unknown currency: " + to);
        BigDecimal rate = rt.divide(rf, 8, RoundingMode.HALF_UP);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("from", from);
        out.put("to", to);
        out.put("amount", amount);
        out.put("rate", rate);
        out.put("result", amount.multiply(rate).setScale(4, RoundingMode.HALF_UP));
        return out;
    }

    public List<Map<String, Object>> history(String currency, int days) {
        LocalDate since = LocalDate.now(ZoneOffset.UTC).minusDays(days);
        List<Map<String, Object>> out = new ArrayList<>();
        repo.findByCurrencyAndRateDateGreaterThanEqualOrderByRateDateAsc(currency.toUpperCase(), since)
            .forEach(r -> out.add(Map.of("date", r.getRateDate(), "rate", r.getRate())));
        return out;
    }

    private void ensureFreshQuietly() {
        try { ensureFresh(); } catch (Exception e) { log.warn("Could not refresh: {}", e.getMessage()); }
    }
}