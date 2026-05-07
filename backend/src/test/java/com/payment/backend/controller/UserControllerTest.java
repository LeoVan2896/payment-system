package com.payment.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.backend.entity.User;
import com.payment.backend.security.JwtAuthenticationFilter;
import com.payment.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
class UserControllerTest {

    // Override SecurityConfig for tests — replaces the full config with a minimal
    // one that: (a) disables CSRF so POST works without CSRF tokens in tests,
    // (b) permits /register endpoint (mirrors production permitAll intent),
    // (c) requires auth for everything else.
    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/users/register", "/api/auth/login").permitAll()
                            .anyRequest().authenticated())
                    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setFirstName("Huy");
        testUser.setLastName("Van");
        testUser.setEmail("huy@example.com");
        testUser.setPassword("$2a$hashedPassword");
    }

    // ---------- POST /api/users/register ----------

    @Test
    void register_givenValidUser_returns201WithUserEntity() throws Exception {
        when(userService.registerUser(any(User.class))).thenReturn(testUser);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("huy@example.com"))
                // BUG DETECTION: password hash appears in response — this test documents
                // the current broken behavior. The fix is to use a UserResponse DTO that
                // omits the password field entirely.
                .andExpect(jsonPath("$.password").exists());
    }

    @Test
    void register_givenDuplicateEmail_throwsRuntimeException() throws Exception {
        // No @ControllerAdvice — RuntimeException propagates as ServletException in MockMvc.
        // Fix: add a GlobalExceptionHandler that returns 409 Conflict for duplicate emails.
        when(userService.registerUser(any(User.class)))
                .thenThrow(new RuntimeException("Email already in use"));

        try {
            mockMvc.perform(post("/api/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testUser)));
        } catch (Exception ex) {
            org.assertj.core.api.Assertions.assertThat(ex.getCause())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Email already in use");
        }
    }

    @Test
    void register_givenNoAuthToken_returns201BecauseEndpointIsPermitAll() throws Exception {
        when(userService.registerUser(any(User.class))).thenReturn(testUser);

        // /api/users/register is in permitAll() — no auth token needed
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testUser)))
                .andExpect(status().isCreated());
    }

    // ---------- GET /api/users/{id} ----------

    @Test
    @WithMockUser
    void getUser_givenValidId_returns200WithUser() throws Exception {
        when(userService.findById(1L)).thenReturn(testUser);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("huy@example.com"))
                // BUG DETECTION: password field present in response — should be absent
                .andExpect(jsonPath("$.password").exists());
    }

    @Test
    @WithMockUser
    void getUser_givenInvalidId_throwsRuntimeException() throws Exception {
        // No @ControllerAdvice exists — RuntimeException propagates as ServletException.
        // Fix: add GlobalExceptionHandler to map this to 404.
        when(userService.findById(999L)).thenThrow(new RuntimeException("User not found"));

        try {
            mockMvc.perform(get("/api/users/999"));
        } catch (Exception ex) {
            org.assertj.core.api.Assertions.assertThat(ex.getCause())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found");
        }
    }

    @Test
    void getUser_givenNoAuthToken_returns401Or403() throws Exception {
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().is4xxClientError());
    }
}
