package com.faceai.pdfreader.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.faceai.pdfreader.config.AuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "test-jwt-secret-that-is-long-enough-2026";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        AuthProperties properties = new AuthProperties(SECRET, 168);
        jwtService = new JwtService(properties);
    }

    @Test
    void createToken_and_parseToken_roundTrip() {
        AuthUser user = new AuthUser(42L, "testuser", "Test User");
        String token = jwtService.createToken(user);

        AuthUser parsed = jwtService.parseToken(token);

        assertEquals(42L, parsed.id());
        assertEquals("testuser", parsed.username());
        assertEquals("Test User", parsed.displayName());
    }

    @Test
    void parseToken_throwsForNullInput() {
        assertThrows(IllegalArgumentException.class, () -> jwtService.parseToken(null));
    }

    @Test
    void parseToken_throwsForBlankInput() {
        assertThrows(IllegalArgumentException.class, () -> jwtService.parseToken("  "));
    }

    @Test
    void parseToken_throwsForMalformedToken() {
        assertThrows(IllegalArgumentException.class, () -> jwtService.parseToken("not.a.valid.token.parts"));
    }

    @Test
    void parseToken_throwsForTamperedSignature() {
        AuthUser user = new AuthUser(1L, "user", "User");
        String token = jwtService.createToken(user);
        // tamper with the signature
        String tampered = token.substring(0, token.length() - 2) + "XX";
        assertThrows(IllegalArgumentException.class, () -> jwtService.parseToken(tampered));
    }

    @Test
    void parseToken_throwsForExpiredToken() {
        // create a service with 0 hour TTL to get an already-expired token
        AuthProperties shortTtl = new AuthProperties(SECRET, 0);
        JwtService shortTtlService = new JwtService(shortTtl);
        AuthUser user = new AuthUser(1L, "user", "User");
        String token = shortTtlService.createToken(user);

        assertThrows(IllegalArgumentException.class, () -> jwtService.parseToken(token));
    }
}
