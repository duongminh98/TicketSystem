package com.heditra.paymentservice;

import com.heditra.events.core.DomainEvent;
import com.heditra.events.core.EventPublisher;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.concurrent.CompletableFuture;

@Configuration
@Profile("test")
public class TestConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("payments");
    }

    @Bean
    public EventPublisher eventPublisher() {
        return new EventPublisher() {
            @Override
            public <T extends DomainEvent> CompletableFuture<Void> publish(T event) {
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public <T extends DomainEvent> CompletableFuture<Void> publish(String topic, T event) {
                return CompletableFuture.completedFuture(null);
            }
        };
    }
}
