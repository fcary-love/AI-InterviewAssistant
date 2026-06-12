package com.faceai.pdfreader.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.faceai.pdfreader.auth.AuthUser;
import com.faceai.pdfreader.auth.JwtService;
import com.faceai.pdfreader.auth.PasswordService;
import com.faceai.pdfreader.model.AuthLoginRequest;
import com.faceai.pdfreader.model.AuthRegisterRequest;
import com.faceai.pdfreader.model.AuthResponse;
import com.faceai.pdfreader.repository.AuthRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthRepository authRepository;
    @Mock
    private PasswordService passwordService;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_createsUser_andReturnsToken() {
        AuthRegisterRequest request = new AuthRegisterRequest("newuser", "pass123", "New User");
        when(authRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordService.hash("pass123")).thenReturn("hashed");
        AuthUser createdUser = new AuthUser(1L, "newuser", "New User");
        when(authRepository.createUser("newuser", "hashed", "New User")).thenReturn(createdUser);
        when(jwtService.createToken(createdUser)).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.token());
        verify(authRepository).createUser("newuser", "hashed", "New User");
    }

    @Test
    void register_throwsForDuplicateUsername() {
        AuthRegisterRequest request = new AuthRegisterRequest("existing", "pass123", "User");
        when(authRepository.existsByUsername("existing")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
    }

    @Test
    void login_returnsToken_forValidCredentials() {
        AuthLoginRequest request = new AuthLoginRequest("testuser", "pass123");
        AuthRepository.UserPasswordRecord record = new AuthRepository.UserPasswordRecord(
                1L, "testuser", "Test User", "hashed"
        );
        when(authRepository.findPasswordByUsername("testuser")).thenReturn(Optional.of(record));
        when(passwordService.matches("pass123", "hashed")).thenReturn(true);
        when(jwtService.createToken(new AuthUser(1L, "testuser", "Test User"))).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.token());
    }

    @Test
    void login_throwsForWrongPassword() {
        AuthLoginRequest request = new AuthLoginRequest("testuser", "wrong");
        AuthRepository.UserPasswordRecord record = new AuthRepository.UserPasswordRecord(
                1L, "testuser", "Test User", "hashed"
        );
        when(authRepository.findPasswordByUsername("testuser")).thenReturn(Optional.of(record));
        when(passwordService.matches("wrong", "hashed")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> authService.login(request));
    }

    @Test
    void login_throwsForNonexistentUser() {
        AuthLoginRequest request = new AuthLoginRequest("nobody", "pass");
        when(authRepository.findPasswordByUsername("nobody")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> authService.login(request));
    }

    @Test
    void loadUser_throwsWhenNotFound() {
        when(authRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> authService.loadUser(999L));
    }
}
