package com.firmament.immigration.service.impl;

import com.firmament.immigration.dto.request.LoginRequest;
import com.firmament.immigration.dto.response.LoginResponse;
import com.firmament.immigration.exception.BusinessException;
import com.firmament.immigration.service.AuthService;
import com.firmament.immigration.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPasswordHash;

    @Value("${app.jwt.expiration}")
    private Long jwtExpiration;

    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());
        log.debug("Expected username: {}", adminUsername);
        log.debug("Received username: {}", request.getUsername());
        log.debug("Password hash from config: {}", adminPasswordHash);

        // Check if username matches
        if (!adminUsername.equals(request.getUsername())) {
            log.warn("Username mismatch - expected: '{}', received: '{}'", adminUsername, request.getUsername());
            throw new BusinessException("Invalid username or password");
        }

        // Check if password matches
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), adminPasswordHash);
        log.debug("Password matches: {}", passwordMatches);

        if (!passwordMatches) {
            log.warn("Password verification failed");
            // Let's also test if the hash is valid
            log.debug("Testing hash with known password 'admin123': {}",
                    passwordEncoder.matches("admin123", adminPasswordHash));
            throw new BusinessException("Invalid username or password");
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(request.getUsername(), "ADMIN");

        log.info("Successful login for user: {}", request.getUsername());

        return LoginResponse.builder()
                .token(token)
                .type("Bearer")
                .username(request.getUsername())
                .role("ADMIN")
                .expiresIn(jwtExpiration / 1000) // Convert to seconds
                .build();
    }

    @Override
    public LoginResponse getUserFromToken(String token) {
        try {
            String username = jwtUtil.extractUsername(token);
            String role = jwtUtil.extractRole(token);

            // Validate token
            if (!jwtUtil.validateToken(token, username)) {
                throw new BusinessException("Invalid or expired token");
            }

            return LoginResponse.builder()
                    .username(username)
                    .role(role)
                    .build();
        } catch (Exception e) {
            throw new BusinessException("Invalid token");
        }
    }
}