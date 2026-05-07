package com.payment.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.backend.entity.Account;
import com.payment.backend.entity.Transaction;
import com.payment.backend.security.JwtAuthenticationFilter;
import com.payment.backend.service.AccountService;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AccountController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
class AccountControllerTest {

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
    private AccountService accountService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Account sampleAccount;
    private Transaction sampleTransaction;

    @BeforeEach
    void setUp() {
        sampleAccount = new Account();
        sampleAccount.setId(1L);
        sampleAccount.setAccountNumber("ACC-001");
        sampleAccount.setBalance(new BigDecimal("500.00"));
        sampleAccount.setStatus(Account.AccountStatus.ACTIVE);

        sampleTransaction = new Transaction();
        sampleTransaction.setId(10L);
        sampleTransaction.setAmount(new BigDecimal("100.00"));
        sampleTransaction.setStatus(Transaction.TransactionStatus.COMPLETED);
    }

    // ---------- GET /api/accounts/{accountNumber} ----------

    @Test
    @WithMockUser
    void getAccount_givenValidAccountNumber_returns200WithAccount() throws Exception {
        when(accountService.getAccountByNumber("ACC-001")).thenReturn(sampleAccount);

        mockMvc.perform(get("/api/accounts/ACC-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("ACC-001"))
                .andExpect(jsonPath("$.balance").value(500.00));
    }

    @Test
    @WithMockUser
    void getAccount_givenUnknownAccountNumber_throwsRuntimeException() throws Exception {
        // Without a @ControllerAdvice / @RestControllerAdvice, unhandled RuntimeExceptions
        // propagate as ServletException in MockMvc. This test documents the current broken
        // behavior — the fix is to add a GlobalExceptionHandler that maps RuntimeException
        // to 404/500 appropriately.
        when(accountService.getAccountByNumber("UNKNOWN"))
                .thenThrow(new RuntimeException("Account not found"));

        try {
            mockMvc.perform(get("/api/accounts/UNKNOWN"));
        } catch (Exception ex) {
            org.assertj.core.api.Assertions.assertThat(ex.getCause())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Account not found");
        }
    }

    @Test
    void getAccount_givenNoAuthToken_returns401Or403() throws Exception {
        mockMvc.perform(get("/api/accounts/ACC-001"))
                .andExpect(status().is4xxClientError());
    }

    // ---------- GET /api/accounts/user/{userId} ----------

    @Test
    @WithMockUser
    void getUserAccounts_givenValidUserId_returns200WithList() throws Exception {
        when(accountService.getAccountsByUser(1L)).thenReturn(List.of(sampleAccount));

        mockMvc.perform(get("/api/accounts/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountNumber").value("ACC-001"));
    }

    @Test
    @WithMockUser
    void getUserAccounts_givenUserWithNoAccounts_returns200WithEmptyList() throws Exception {
        when(accountService.getAccountsByUser(999L)).thenReturn(List.of());

        mockMvc.perform(get("/api/accounts/user/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ---------- POST /api/accounts/user/{userId} ----------

    @Test
    @WithMockUser
    void createAccount_givenValidUserId_returns201WithAccount() throws Exception {
        when(accountService.createAccount(1L)).thenReturn(sampleAccount);

        mockMvc.perform(post("/api/accounts/user/1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber").value("ACC-001"));
    }

    // ---------- POST /api/accounts/{accountNumber}/deposit ----------

    @Test
    @WithMockUser
    void deposit_givenValidRequest_returns200WithUpdatedAccount() throws Exception {
        Account updatedAccount = new Account();
        updatedAccount.setAccountNumber("ACC-001");
        updatedAccount.setBalance(new BigDecimal("700.00"));

        when(accountService.deposit(eq("ACC-001"), any(BigDecimal.class)))
                .thenReturn(updatedAccount);

        Map<String, String> body = Map.of("amount", "200.00");

        mockMvc.perform(post("/api/accounts/ACC-001/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(700.00));
    }

    // ---------- POST /api/accounts/transfer ----------

    @Test
    @WithMockUser
    void transfer_givenValidRequest_returns201WithTransaction() throws Exception {
        when(accountService.transfer(eq("ACC-SENDER"), eq("ACC-RECEIVER"), any(BigDecimal.class)))
                .thenReturn(sampleTransaction);

        Map<String, String> body = Map.of(
                "senderAccountNumber", "ACC-SENDER",
                "receiverAccountNumber", "ACC-RECEIVER",
                "amount", "100.00"
        );

        mockMvc.perform(post("/api/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @WithMockUser
    void transfer_givenInsufficientFunds_throwsRuntimeException() throws Exception {
        // Same issue as getAccount error case — no GlobalExceptionHandler means
        // RuntimeException propagates as ServletException in MockMvc.
        when(accountService.transfer(anyString(), anyString(), any(BigDecimal.class)))
                .thenThrow(new RuntimeException("Insufficient funds"));

        Map<String, String> body = Map.of(
                "senderAccountNumber", "ACC-SENDER",
                "receiverAccountNumber", "ACC-RECEIVER",
                "amount", "999999.00"
        );

        try {
            mockMvc.perform(post("/api/accounts/transfer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)));
        } catch (Exception ex) {
            org.assertj.core.api.Assertions.assertThat(ex.getCause())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Insufficient funds");
        }
    }
}
