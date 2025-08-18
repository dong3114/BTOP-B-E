package com.aiaca.btop.security.jwt;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "jwt")
@Getter
@RequiredArgsConstructor
public class JwtProperties {
    private final String secret;
    private final String issuer;
    private final long expiration;
    private final String tokenPrefix;
}