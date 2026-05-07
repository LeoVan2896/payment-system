package com.payment.backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.ExpiredJwtException;
import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    // A valid Base64-encoded 256-bit secret (same as application.properties)
    private static final String TEST_SECRET =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long TEST_EXPIRATION = 86400000L; // 24 hours

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", TEST_EXPIRATION);
    }

    // ---------- generateToken ----------

    @Test
    void generateToken_givenEmail_returnsNonNullToken() {
        String token = jwtService.generateToken("huy@example.com");

        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void generateToken_givenEmail_returnsJwtWithThreeParts() {
        String token = jwtService.generateToken("huy@example.com");

        // JWT format: header.payload.signature
        assertThat(token.split("\\.")).hasSize(3);
    }

    // ---------- extractEmail ----------

    @Test
    void extractEmail_givenValidToken_returnsCorrectEmail() {
        String token = jwtService.generateToken("huy@example.com");

        String extractedEmail = jwtService.extractEmail(token);

        assertThat(extractedEmail).isEqualTo("huy@example.com");
    }

    @Test
    void extractEmail_givenTokenForDifferentUser_returnsCorrectEmail() {
        String token = jwtService.generateToken("other@example.com");

        assertThat(jwtService.extractEmail(token)).isEqualTo("other@example.com");
    }

    // ---------- isTokenValid ----------

    @Test
    void isTokenValid_givenValidTokenAndMatchingEmail_returnsTrue() {
        String token = jwtService.generateToken("huy@example.com");

        boolean valid = jwtService.isTokenValid(token, "huy@example.com");

        assertThat(valid).isTrue();
    }

    @Test
    void isTokenValid_givenValidTokenAndMismatchedEmail_returnsFalse() {
        String token = jwtService.generateToken("huy@example.com");

        boolean valid = jwtService.isTokenValid(token, "attacker@example.com");

        assertThat(valid).isFalse();
    }

    @Test
    void isTokenValid_givenExpiredToken_throwsExpiredJwtException() {
        // JJWT 0.12 throws ExpiredJwtException when parsing an expired token —
        // it does NOT silently return false. The caller (JwtAuthenticationFilter)
        // must handle this exception to avoid a 500 response.
        // This test documents the actual runtime behavior so callers know what to expect.
        ReflectionTestUtils.setField(jwtService, "expiration", -1L);
        String expiredToken = jwtService.generateToken("huy@example.com");

        assertThatThrownBy(() -> jwtService.isTokenValid(expiredToken, "huy@example.com"))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void generateToken_givenNullEmail_returnsTokenWithNullSubject() {
        // JJWT 0.12 silently accepts null subjects — the token is generated successfully
        // but extractEmail() will return null, causing a NullPointerException in
        // JwtAuthenticationFilter when it calls extractEmail(). This is a latent bug:
        // the service should reject null input before building the token.
        String token = jwtService.generateToken(null);
        assertThat(token).isNotBlank();
        // extractEmail returns null for null subject — this causes NPE in the filter
        assertThat(jwtService.extractEmail(token)).isNull();
    }
}
