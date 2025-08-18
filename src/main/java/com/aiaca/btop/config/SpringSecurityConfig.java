package com.aiaca.btop.config;

import com.aiaca.btop.security.jwt.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SpringSecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // 인증 예외 URL 리스트
    private static final String[] AUTH_WHITELIST = {
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/actuator/health",
            "/actuator/info",
            "/api/auth/**",
            "/api/home",
            "/api/member/register/**",
            "/api/member/login",

            // 웹소켓(JWT) 검증은 HandshakeInterceptor/ChannelInterceptor
            "/ws/**"        // 핸드셰이크 단계에서 권한에러 발생할 수 있어서 추가.
    };

    // 스프링Security 설정시작
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("spring Security 설정 시작");
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(AUTH_WHITELIST).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "권한이 없습니다.");
                        })
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        log.info("Spring Security 설정 완료");
        return http.build();
    }
    // CORS 설정 추가
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000")); // 프론트엔드 URL 추가, 배포시엔 xml 파일등으로 주입~
        config.setAllowCredentials(true);                               // 쿠키 쓸거 고려해서 추가 안쓸거면 false

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        // CORS 응답 헤더 노출 설정 (프론트에서 읽을 필요가 있을 때만 활성화)
        // 예: 파일 다운로드 시 "Content-Disposition", POST 요청 후 "Location" 헤더 확인
        // config.setExposedHeaders(List.of("Location", "Content-Disposition"));

        config.setMaxAge(3600L);    // 캐시(초)

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean("authWhitelist")
    public String[] authWhiteList() { return AUTH_WHITELIST;}
}
