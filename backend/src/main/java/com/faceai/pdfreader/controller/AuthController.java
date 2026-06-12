package com.faceai.pdfreader.controller;

import com.faceai.pdfreader.model.ApiResponse;
import com.faceai.pdfreader.model.AuthLoginRequest;
import com.faceai.pdfreader.model.AuthRegisterRequest;
import com.faceai.pdfreader.model.AuthResponse;
import com.faceai.pdfreader.model.AuthUserResponse;
import com.faceai.pdfreader.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody AuthRegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody AuthLoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<AuthUserResponse> me() {
        return ApiResponse.success(authService.me());
    }
}
