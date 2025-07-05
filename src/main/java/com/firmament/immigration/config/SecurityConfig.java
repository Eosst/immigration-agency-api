package com.firmament.immigration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Disable CSRF for REST APIs
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints - no authentication needed
                        .requestMatchers("/api/appointments/**").permitAll()
                        .requestMatchers("/api/timeslots/available").permitAll()

                        // Swagger UI - accessible without login
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/swagger-ui.html").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/api-docs/**").permitAll()

                        // H2 Console (for development)
                        .requestMatchers("/h2-console/**").permitAll()

                        // Actuator endpoints
                        .requestMatchers("/actuator/**").permitAll()

                        // For now, make admin endpoints public (we'll secure them later)
                        .requestMatchers("/api/timeslots/generate").permitAll()
                        .requestMatchers("/api/timeslots/*/block").permitAll()
                        .requestMatchers("/api/timeslots/*/unblock").permitAll()
                        .requestMatchers("/api/appointments/upcoming").permitAll()

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())); // Needed for H2 console

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}