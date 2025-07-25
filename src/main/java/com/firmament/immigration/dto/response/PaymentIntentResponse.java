package com.firmament.immigration.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class PaymentIntentResponse {
    private String clientSecret;
    private BigDecimal amount;
    private String currency;
}