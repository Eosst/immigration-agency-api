package com.firmament.immigration.service;

import com.firmament.immigration.dto.request.LoginRequest;
import com.firmament.immigration.dto.response.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    LoginResponse getUserFromToken(String token);
}