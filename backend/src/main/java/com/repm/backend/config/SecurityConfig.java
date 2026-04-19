package com.repm.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.repm.backend.service.CustomUserDetailsService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.authentication.dao.DaoAuthenticationProvider;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    // PASSWORD ENCODER
    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

    // AUTHENTICATION PROVIDER
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {

        DaoAuthenticationProvider auth = new DaoAuthenticationProvider();

        auth.setUserDetailsService(userDetailsService);
        auth.setPasswordEncoder(passwordEncoder());

        return auth;
    }

    // SUCCESS HANDLER
    @Bean
    public AuthenticationSuccessHandler mySuccessHandler() {

        return (request, response, authentication) -> {
            boolean isAdmin = authentication.getAuthorities()
                    .stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");

            Map<String, String> data = new HashMap<>();
            data.put("role", isAdmin ? "ADMIN" : "USER");
            data.put("message", "Login successful");

            new ObjectMapper().writeValue(response.getOutputStream(), data);
        };
    }

    // FAILURE HANDLER
    @Bean
    public AuthenticationFailureHandler myFailureHandler() {

        return (request, response, exception) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");

            Map<String, String> data = new HashMap<>();
            data.put("error", "Invalid username or password");

            new ObjectMapper().writeValue(response.getOutputStream(), data);
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("https://repm-project.vercel.app", "http://localhost:8080", "http://127.0.0.1:5500", "http://localhost:5500"));
        configuration.setAllowCredentials(true);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("authorization", "content-type", "x-auth-token"));
        configuration.setExposedHeaders(Arrays.asList("x-auth-token"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // SECURITY FILTER
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .authenticationProvider(authenticationProvider())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/",
                        "/login",
                        "/api/auth/**",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/uploads/**",
                        "/*.html",
                        "/user/*.html",
                        "/admin/*.html"
                ).permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/user/**").hasRole("USER")
                .anyRequest().authenticated()
            )
            .formLogin(login -> login
                    .loginProcessingUrl("/login")
                    .successHandler(mySuccessHandler())
                    .failureHandler(myFailureHandler())
                    .permitAll()
            )
            .logout(logout -> logout
                    .logoutUrl("/logout")
                    .logoutSuccessHandler((request, response, authentication) -> {
                        response.setStatus(HttpServletResponse.SC_OK);
                        response.setContentType("application/json");
                        Map<String, String> data = new HashMap<>();
                        data.put("message", "Logout successful");
                        new ObjectMapper().writeValue(response.getOutputStream(), data);
                    })
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .permitAll()
            )
            .sessionManagement(session -> session
                    .maximumSessions(1)
                    .maxSessionsPreventsLogin(true)
            );

        return http.build();
    }

    @Bean
    public org.springframework.security.web.session.HttpSessionEventPublisher httpSessionEventPublisher() {
        return new org.springframework.security.web.session.HttpSessionEventPublisher();
    }
}