package edu.horbatiuk.apiservice.controller;

import edu.horbatiuk.apiservice.model.CurrencyResponse;
import edu.horbatiuk.apiservice.service.CurrencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/currency")
@RequiredArgsConstructor
public class CurrencyController {

    private final CurrencyService currencyService;


    @GetMapping
    public ResponseEntity<CurrencyResponse> getRates(
            @RequestParam(defaultValue = "USD") String base) {

        log.info("GET /currency - base: {}", base);
        CurrencyResponse response = currencyService.getRates(base);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/convert")
    public ResponseEntity<CurrencyResponse> convert(
            @RequestParam(defaultValue = "USD") String from,
            @RequestParam(defaultValue = "UAH") String to,
            @RequestParam(defaultValue = "1") double amount) {

        log.info("GET /currency/convert - from: {}, to: {}, amount: {}", from, to, amount);
        CurrencyResponse response = currencyService.convertCurrency(from, to, amount);
        return ResponseEntity.ok(response);
    }
}