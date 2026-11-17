package com.safedesk.common.config;

import com.safedesk.auth.jwt.JwtAccessDeniedHandler;
import com.safedesk.auth.jwt.JwtAuthenticationEntryPoint;
import com.safedesk.auth.jwt.JwtAuthenticationFilter;
import com.safedesk.auth.jwt.JwtProvider;
import com.safedesk.auth.jwt.TokenBlacklist;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final TokenBlacklist tokenBlacklist;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(JwtProvider jwtProvider, TokenBlacklist tokenBlacklist,
                          JwtAuthenticationEntryPoint authenticationEntryPoint,
                          JwtAccessDeniedHandler accessDeniedHandler) {
        this.jwtProvider = jwtProvider;
        this.tokenBlacklist = tokenBlacklist;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider, tokenBlacklist), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                // 위에서부터 처음 맞는 규칙이 적용되므로, 인증이 필요한 /api/auth 경로를 /api/auth/** 보다 먼저 둔다.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/me", "/api/auth/logout").authenticated()
                        // HIS-06 상담 상세: 로그인만 확인 (토큰 없으면 401). 본인 상담인지는 서비스에서 확인
                        .requestMatchers("/api/v1/consultations/*/history").authenticated()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // TODO(인증): API별 서버 권한 검사. docs/api 명세의 "인증" 열 기준으로 작성함.
                        //  주석 해제 전 확인할 것
                        //  1. 각 API 담당자와 경로·권한 합의 (명세가 바뀌면 함께 수정)
                        //  2. 프론트의 모든 API 호출에 Authorization: Bearer 헤더가 붙어 있는지
                        //     (붙어 있지 않으면 해제하는 순간 해당 화면이 전부 401)
                        //  3. 맨 아래 anyRequest().permitAll()을 anyRequest().authenticated()로 변경
                        //  4. import org.springframework.http.HttpMethod; 추가
                        //  WebSocket(/app/chats/**) 인증은 HTTP 규칙이 아니라 STOMP CONNECT 인터셉터에서 따로 처리해야 함.
                        //
                        // 관리자 (policy.md, sanction.md, faq.md, history.md, customer-chat.md API-DEV-01)
                        // .requestMatchers("/api/admin/**", "/api/v1/dev/**").hasRole("ADMIN")
                        //
                        // 고객 본인 (history.md, customer-chat.md)
                        // .requestMatchers("/api/customers/me/**").hasRole("CUSTOMER")
                        // .requestMatchers(HttpMethod.POST, "/api/v1/chats", "/api/v1/chats/*/bot-messages").hasRole("CUSTOMER")
                        // .requestMatchers(HttpMethod.PATCH, "/api/v1/chats/*/handoff", "/api/v1/chats/*/cancel").hasRole("CUSTOMER")
                        //
                        // 상담원 본인 (history.md, chat.md)
                        // .requestMatchers("/api/agents/me/**", "/api/v1/agents/me/**").hasRole("AGENT")
                        //
                        // 경고·강제 종료 (moderation.md). POST 경고는 AGENT만 가능해서 GET 규칙보다 먼저 둔다.
                        // .requestMatchers(HttpMethod.POST, "/api/consultations/*/warnings").hasRole("AGENT")
                        // .requestMatchers("/api/consultations/*/warnings", "/api/consultations/*/terminate").hasAnyRole("AGENT", "ADMIN")
                        //
                        // 그 밖의 "필요" API(/api/v1/chats/queue, /accept, /close, 상담 이력 상세 등)는 로그인만 확인하고,
                        // 본인 상담인지는 각 서비스에서 memberId로 확인한다 (예: CHAT_ACCESS_DENIED).

                        .requestMatchers("/api/auth/**", "/ws/**").permitAll()
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
