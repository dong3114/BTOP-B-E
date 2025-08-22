package com.aiaca.btop.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * - OPTIONS, 화이트리스트, 그리고 WebSocket 업그레이드 요청은 필터링에서 제외
 * - Authorization: Bearer <token> 이면 검증하여 SecurityContext 설정
 * - 토큰이 없거나/유효하지 않으면 체인 계속 진행(EntryPoint에서 401 응답)
 */
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final String[] authWhitelist;
    private final JwtUtil jwtUtil;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // SpringSecurityConfig에서 new JwtAuthenticationFilter(authWhitelist, jwtUtil) 로 생성
    public JwtAuthenticationFilter(String[] authWhitelist, JwtUtil jwtUtil) {
        this.authWhitelist = (authWhitelist == null) ? new String[0] : authWhitelist;
        this.jwtUtil = jwtUtil;
    }

    /** OPTIONS, 화이트리스트, WebSocket 핸드셰이크는 필터 제외 */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Preflight
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        // WebSocket Handshake(Upgrade: websocket)
        String upgrade = request.getHeader("Upgrade");
        if (upgrade != null && "websocket".equalsIgnoreCase(upgrade)) return true;

        // 화이트리스트 경로
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

        final String requestURI = request.getRequestURI();
        final String method = request.getMethod();
        final String authHeader = request.getHeader("Authorization");

        log.debug("[JWT] filtering... method={}, uri={}", method, requestURI);

        // 이미 인증돼 있으면 패스
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(request, response);
            return;
        }

        // 헤더 없음/포맷 불일치 -> 패스(나중에 EntryPoint가 401 처리)
        if (authHeader == null || !authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            chain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7).trim();

        try {
            if (!jwtUtil.validateToken(token)) {
                log.info("[JWT] invalid token");
                chain.doFilter(request, response);
                return;
            }

            String memberNo = jwtUtil.extractMemberNo(token);
            int roleLevel = jwtUtil.extractRoleLevel(token);
            String roleName = (roleLevel == 1) ? "ROLE_USER" : "ROLE_ADMIN";

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            memberNo,
                            null,
                            List.of(new SimpleGrantedAuthority(roleName))
                    );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("[JWT] authenticated: memberNo={}, roleLevel={}", memberNo, roleLevel);
            chain.doFilter(request, response);

        } catch (Exception e) {
            log.warn("[JWT] exception while processing token: {}", e.getMessage());
            // 토큰 처리 중 예외 발생 시에도 체인 계속(EntryPoint가 최종 응답)
            chain.doFilter(request, response);
        }
    }
}
