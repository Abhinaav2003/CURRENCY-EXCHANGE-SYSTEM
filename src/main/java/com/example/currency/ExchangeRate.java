package com.example.currency;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Rate of 1 USD expressed in the given currency, for one day. */
@Entity
@Table(name = "exchange_rate",
       uniqueConstraints = @UniqueConstraint(columnNames = {"currency", "rate_date"}))
public class ExchangeRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(nullable = false, precision = 24, scale = 8)
    private BigDecimal rate;

    @Column(name = "rate_date", nullable = false)
    private LocalDate rateDate;

    protected ExchangeRate() {}

    public ExchangeRate(String currency, BigDecimal rate, LocalDate rateDate) {
        this.currency = currency;
        this.rate = rate;
        this.rateDate = rateDate;
    }

    public Long getId() { return id; }
    public String getCurrency() { return currency; }
    public BigDecimal getRate() { return rate; }
    public LocalDate getRateDate() { return rateDate; }
}