package edu.horbatiuk.telegrambot.nlp;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class NlpResult {

    private NlpService.Intent intent;

    private String city;

    private String currency;

    private String targetCurrency;

    private Double amount;

    private String rawText;

    public static NlpResult unknown() {
        return NlpResult.builder()
                .intent(NlpService.Intent.UNKNOWN)
                .build();
    }

    public boolean hasCity()           { return city != null && !city.isBlank(); }
    public boolean hasCurrency()       { return currency != null && !currency.isBlank(); }
    public boolean hasTargetCurrency() { return targetCurrency != null && !targetCurrency.isBlank(); }
    public boolean hasAmount()         { return amount != null && amount > 0; }
}