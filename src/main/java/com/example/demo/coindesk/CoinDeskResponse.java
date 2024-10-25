package com.example.demo.coindesk;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
public class CoinDeskResponse {
    private Time time;
    private Map<String, CurrencyInfo> bpi;

    @Getter
    @Setter
    public static class Time {
        private String updatedISO;

    }

    @Getter
    @Setter
    public static class CurrencyInfo {
        private String code;
        private String symbol;
        private String description;
        private String rate;
        @JsonProperty("rate_float")
        private BigDecimal rateFloat;  // 對應 JSON 中的 rate_float
    }
}
