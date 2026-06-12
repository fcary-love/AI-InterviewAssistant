package com.faceai.pdfreader.service;

import com.faceai.pdfreader.auth.AuthContext;
import com.faceai.pdfreader.auth.AuthUser;
import com.faceai.pdfreader.auth.JwtService;
import com.faceai.pdfreader.auth.PasswordService;
import com.faceai.pdfreader.model.AuthLoginRequest;
import com.faceai.pdfreader.model.AuthRegisterRequest;
import com.faceai.pdfreader.model.AuthResponse;
import com.faceai.pdfreader.model.AuthUserResponse;
import com.faceai.pdfreader.repository.AuthRepository;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthRepository authRepository;
    private final PasswordService passwordService;
    private final JwtService jwtService;

    public AuthService(AuthRepository authRepository, PasswordService passwordService, JwtService jwtService) {
        this.authRepository = authRepository;
        this.passwordService = passwordService;
        this.jwtService = jwtService;
    }

    public AuthResponse register(AuthRegisterRequest request) {
        String username = normalizeUsername(request.username());
        if (authRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("账号已存在，请换一个账号");
        }
        AuthUser user = authRepository.createUser(
                username,
                passwordService.hash(request.password()),
                request.displayName().trim()
        );
        return buildAuthResponse(user);
    }

    public AuthResponse login(AuthLoginRequest request) {
        String username = normalizeUsername(request.username());
        AuthRepository.UserPasswordRecord record = authRepository.findPasswordByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("账号或密码错误"));
        if (!passwordService.matches(request.password(), record.passwordHash())) {
            throw new IllegalArgumentException("账号或密码错误");
        }
        return buildAuthResponse(new AuthUser(record.id(), record.username(), record.displayName()));
    }

    public AuthUserResponse me() {
        AuthUser user = AuthContext.currentUser();
        return new AuthUserResponse(user.id(), user.username(), user.displayName());
    }

    public AuthUser loadUser(Long userId) {
        return authRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在或已停用"));
    }

    private AuthResponse buildAuthResponse(AuthUser user) {
        return new AuthResponse(
                jwtService.createToken(user),
                new AuthUserResponse(user.id(), user.username(), user.displayName())
        );
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
