package com.aiaca.btop.security.jwt;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    // application.yml/.properties에서 바인딩
    private final String secret;
    private final String issuer;
    private final long expiration;   // ms
    private final String tokenPrefix; // 보통 "Bearer "

    public JwtProperties(String secret, String issuer, long expiration, String tokenPrefix) {
        this.secret = secret;
        this.issuer = issuer;
        this.expiration = expiration;
        this.tokenPrefix = tokenPrefix;
    }
    public String getSecret() { return secret; }
    public String getIssuer() { return issuer; }
    public long getExpiration() { return expiration; }
    public String getTokenPrefix() { return tokenPrefix; }
}