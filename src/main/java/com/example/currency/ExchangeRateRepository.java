package com.example.currency;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> 
{
    Optional<ExchangeRate> findTopByOrderByRateDateDesc();
    List<ExchangeRate> findByRateDate(LocalDate date);
    Optional<ExchangeRate> findByCurrencyAndRateDate(String currency, LocalDate date);
    List<ExchangeRate> findByCurrencyAndRateDateGreaterThanEqualOrderByRateDateAsc(String currency, LocalDate from);
}