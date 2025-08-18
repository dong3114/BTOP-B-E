package com.aiaca.btop.security.jwt;

import io.jsonwebtoken.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {
    private final JwtProperties jwtProperties;
    // jwt 토큰 생성
    public String generateToken(String memberNo, int roleLevel) {
        return Jwts.builder()
                .setSubject(memberNo)
                .claim("roleLevel", roleLevel)
                .setIssuer(jwtProperties.getIssuer())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getExpiration()))
                .signWith(SignatureAlgorithm.HS256, jwtProperties.getSecret())
                .compact();
    }
    // 토큰의 유효성 검사
    public boolean validateToken (String token){
        try {
            getClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.info("JWT 만료됨: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 JWT: " + e.getMessage());
        } catch (SignatureException e) {
            log.warn("JWT 서명 오류: " + e.getMessage());
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
        return Jwts.parser()
                .setSigningKey(jwtProperties.getSecret())
                .parseClaimsJws(token.replace("Bearer", "").trim())
                .getBody();
    }
}
