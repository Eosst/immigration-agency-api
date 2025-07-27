package com.firmament.immigration.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "pricing")
@Data // From Lombok, for getters/setters
public class PricingConfig {

    private String baseCurrency;
    private BigDecimal rateCadToMad;
    private Map<String, Integer> cadDuration;
    private Map<String, Integer> madDuration;
}