package com.example.demo.currency;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

@Getter
@Setter
public class CurrencyDTO {
    private String code;
    private String currencyNameCN;
    private BigDecimal rateFloat;
    @Setter
    @Getter(AccessLevel.NONE)
    private Timestamp updated;
    private String language;

    public String getUpdated() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return sdf.format(updated);
    }
}

