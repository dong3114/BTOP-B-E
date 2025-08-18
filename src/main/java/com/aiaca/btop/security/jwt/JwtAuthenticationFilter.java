package com.aiaca.btop.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final String[] authWhitelist;
    private final JwtUtil jwtUtil;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // 1. 옵션 요청과 인증 예외 요청 필터링
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        String uri = request.getRequestURI();
        for (String pattern : authWhitelist) {
            if (pathMatcher.match(pattern, uri)) return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // 2. 요청 URI, HTTP 메서드 확인
        final String requestURI = request.getRequestURI();    // 요청 URI
        final String method = request.getMethod();            // 요청 HTTP 메서드
        final String authHeader = request.getHeader("Authorization");   // 요청 헤더
        log.debug("doFilter 시작: 메서드 = {}, URI = {}",method, requestURI);

        // 3. 헤더 검사
        if (authHeader == null || !authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            // 권한에러반환
            chain.doFilter(request, response);
            return;
        }
        // 4. 토큰 유효성 검증
        final String token = authHeader.substring(7).trim();      // 접두어 Bearer 제거
        if(!jwtUtil.validateToken(token)) {
            log.info("[doFilter] 토큰이 없습니다.");
            chain.doFilter(request, response);
            return;
        }
        // 5. 토큰 클레임에서 사용자 / 권한 추출
        String memberNo = jwtUtil.extractMemberNo(token);
        int roleLevel = jwtUtil.extractRoleLevel(token);
        String roleName = roleLevel == 1 ? "ROLE_USER" : "ROLE_ADMIN";
        log.info("[doFilter] 인증성공 - 사용자: " + memberNo + " / 회원레벨: " + roleLevel);

        // 6. 인증 객체 설정 및 정보 추출
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        memberNo,
                        null,
                        List.of(new SimpleGrantedAuthority(roleName))
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        chain.doFilter(request, response);
    }
}
