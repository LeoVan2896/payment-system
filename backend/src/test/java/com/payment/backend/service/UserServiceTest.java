package com.payment.backend.service;

import com.payment.backend.entity.User;
import com.payment.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setFirstName("Huy");
        testUser.setLastName("Van");
        testUser.setEmail("huy@example.com");
        testUser.setPassword("rawPassword");
    }

    // ---------- registerUser ----------

    @Test
    void registerUser_givenNewEmail_encodesPasswordAndSaves() {
        when(userRepository.existsByEmail("huy@example.com")).thenReturn(false);
        when(passwordEncoder.encode("rawPassword")).thenReturn("$2a$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.registerUser(testUser);

        assertThat(result.getPassword()).isEqualTo("$2a$hashed");
        verify(passwordEncoder).encode("rawPassword");
        verify(userRepository).save(testUser);
    }

    @Test
    void registerUser_givenDuplicateEmail_throwsRuntimeException() {
        when(userRepository.existsByEmail("huy@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(testUser))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email already in use");

        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_givenNewEmail_doesNotStoreRawPassword() {
        when(userRepository.existsByEmail("huy@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.registerUser(testUser);

        assertThat(result.getPassword()).isNotEqualTo("rawPassword");
    }

    // ---------- findByEmail ----------

    @Test
    void findByEmail_givenExistingEmail_returnsOptionalWithUser() {
        when(userRepository.findByEmail("huy@example.com")).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.findByEmail("huy@example.com");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("huy@example.com");
    }

    @Test
    void findByEmail_givenNonExistingEmail_returnsEmptyOptional() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        Optional<User> result = userService.findByEmail("ghost@example.com");

        assertThat(result).isEmpty();
    }

    // ---------- findById ----------

    @Test
    void findById_givenValidId_returnsUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        User result = userService.findById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("huy@example.com");
    }

    @Test
    void findById_givenInvalidId_throwsRuntimeException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }
}
