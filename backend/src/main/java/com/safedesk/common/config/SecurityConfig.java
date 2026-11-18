package com.safedesk.common.config;

import com.safedesk.auth.jwt.JwtAccessDeniedHandler;
import com.safedesk.auth.jwt.JwtAuthenticationEntryPoint;
import com.safedesk.auth.jwt.JwtAuthenticationFilter;
import com.safedesk.auth.jwt.JwtProvider;
import com.safedesk.auth.jwt.TokenBlacklist;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
                // 위에서부터 처음 맞는 규칙이 적용된다. 역할 규칙은 docs/api 명세의 "인증" 열 기준.
                // 본인 상담인지 같은 세부 확인은 각 서비스에서 memberId로 한다 (예: CHAT_ACCESS_DENIED).
                // WebSocket(/app/chats/**) 인증은 HTTP 규칙이 아니라 StompAuthChannelInterceptor 가 맡는다.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/auth/signup", "/api/auth/refresh",
                                         "/api/auth/check-login-id").permitAll()
                        .requestMatchers("/ws/**", "/error").permitAll()

                        .requestMatchers("/api/admin/**", "/api/dev/**").hasRole("ADMIN")

                        .requestMatchers("/api/customers/me/**").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/chats", "/api/chats/*/bot-messages").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.PATCH, "/api/chats/*/handoff", "/api/chats/*/cancel").hasRole("CUSTOMER")

                        .requestMatchers("/api/agents/me/**").hasRole("AGENT")
                        .requestMatchers(HttpMethod.GET, "/api/chats/queue").hasRole("AGENT")
                        .requestMatchers(HttpMethod.PATCH, "/api/chats/*/accept").hasRole("AGENT")

                        .anyRequest().authenticated()
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
