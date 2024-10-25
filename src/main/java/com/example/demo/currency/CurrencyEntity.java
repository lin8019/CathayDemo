package com.example.demo.currency;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
@Table(name = "bitcoin_exchange_rate")
@Getter
@Setter
public class CurrencyEntity {
    static final String CREATE_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS bitcoin_exchange_rate ("
            + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
            + "updated VARCHAR(255), "
            + "code VARCHAR(255) UNIQUE, "
            + "currency_name_cn VARCHAR(255), "
            + "rate_float DECIMAL(19, 4)"
            + ");";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "updated")
    private Timestamp updated;
    @Column(name = "code", unique = true)
    private String code;
    @Column(name = "currency_name_cn")
    private String CurrencyNameCN;
    @Column(name = "rate_float")
    private BigDecimal rateFloat;
}
