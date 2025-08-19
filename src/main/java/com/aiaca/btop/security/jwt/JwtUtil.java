package com.aiaca.btop.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {
    private final JwtProperties jwtProperties;
    private final Key key;

    public JwtUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        // secret을 Base64로 보지 않고 "그대로" 바이트로 사용
        // HS256은 최소 32바이트 이상 권장 (짧으면 WeakKeyException)
        this.key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    // jwt 토큰 생성
    public String generateToken(String memberNo, int roleLevel) {
        return Jwts.builder()
                .setSubject(memberNo)
                .claim("roleLevel", roleLevel)
                .setIssuer(jwtProperties.getIssuer())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getExpiration()))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
    // 토큰의 유효성 검사
    public boolean validateToken (String token){
        try {
            getClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.info("JWT 만료됨: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT 잘못된 인자: " + e.getMessage());
        }
        return false;
    }
    public long extractExpires(String token) {return getClaims(token).getExpiration().getTime();}
    // 회원 번호 추출
    public String extractMemberNo(String token) {return getClaims(token).getSubject();}
    // 회원 권한레벨 추출
    public int extractRoleLevel(String token) {return getClaims(token).get("roleLevel", Integer.class);}
    // 토큰 파싱
    private Claims getClaims(String token) {
        String raw = stripPrefix(token, jwtProperties.getTokenPrefix()); // "Bearer " 제거
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(raw)
                .getBody();
    }
    private static String stripPrefix(String token, String prefix) {
        if (token == null) return "";
        String t = token.trim();
        String p = (prefix == null || prefix.isBlank()) ? "Bearer " : prefix;
        // 선두의 prefix만 제거 (대소문자 무시)
        if (t.regionMatches(true, 0, p, 0, p.length())) {
            return t.substring(p.length()).trim();
        }
        return t;
    }
}
