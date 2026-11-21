package dev.vedansh.cartwise.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cartwise.jwt")
public record JwtProperties(String secret, long ttlMinutes) {
}
