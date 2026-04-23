package edu.horbatiuk.apiservice.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Value("${cache.weather.ttl:600}")
    private long weatherTtl;

    @Value("${cache.currency.ttl:3600}")
    private long currencyTtl;

    @Bean
    public CacheManager cacheManager() {
        // Weather cache: expires after 10 minutes
        CaffeineCache weatherCache = new CaffeineCache("weather",
                Caffeine.newBuilder()
                        .expireAfterWrite(weatherTtl, TimeUnit.SECONDS)
                        .maximumSize(100)
                        .recordStats()
                        .build());

        // Currency cache: expires after 1 hour
        CaffeineCache currencyCache = new CaffeineCache("currency",
                Caffeine.newBuilder()
                        .expireAfterWrite(currencyTtl, TimeUnit.SECONDS)
                        .maximumSize(50)
                        .recordStats()
                        .build());

        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(weatherCache, currencyCache));
        return manager;
    }
}