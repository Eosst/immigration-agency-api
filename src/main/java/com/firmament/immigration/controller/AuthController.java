package com.firmament.immigration.controller;

import com.firmament.immigration.dto.request.LoginRequest;
import com.firmament.immigration.dto.response.LoginResponse;
import com.firmament.immigration.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication endpoints")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Admin login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout")
    public ResponseEntity<Void> logout() {
        // For JWT, logout is typically handled client-side by removing the token
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user info")
    public ResponseEntity<LoginResponse> getCurrentUser(@RequestHeader("Authorization") String token) {
        // Extract user info from token
        String actualToken = token.replace("Bearer ", "");
        LoginResponse response = authService.getUserFromToken(actualToken);
        return ResponseEntity.ok(response);
    }
}