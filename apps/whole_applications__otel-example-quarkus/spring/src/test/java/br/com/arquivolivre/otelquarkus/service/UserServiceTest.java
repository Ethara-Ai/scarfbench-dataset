package br.com.arquivolivre.otelquarkus.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.arquivolivre.otelquarkus.model.User;
import br.com.arquivolivre.otelquarkus.repository.UserRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class UserServiceTest {

    @Autowired UserService userService;

    @MockitoBean UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("John Doe", "john@example.com", "Bio");
        testUser.id = 1L;
    }

    @Test
    void testGetAllUsers() {
        // Given
        List<User> users = Arrays.asList(testUser, new User("Jane", "jane@example.com", "Bio"));
        when(userRepository.findAll()).thenReturn(users);

        // When
        List<User> result = userService.getAllUsers();

        // Then
        assertThat(result).hasSize(2);
        verify(userRepository).findAll();
    }

    @Test
    void testGetUserByIdFound() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.getUserById(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().email).isEqualTo("john@example.com");
        verify(userRepository).findById(1L);
    }

    @Test
    void testGetUserByIdNotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.getUserById(999L);

        // Then
        assertThat(result).isEmpty();
        verify(userRepository).findById(999L);
    }

    @Test
    void testGetUserByEmailFound() {
        // Given
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.getUserByEmail("john@example.com");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().name).isEqualTo("John Doe");
        verify(userRepository).findByEmail("john@example.com");
    }

    @Test
    void testGetUserByEmailNotFound() {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.getUserByEmail("nonexistent@example.com");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void testCreateUserSuccess() {
        // Given
        User newUser = new User("New User", "new@example.com", "New bio");
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class)))
                .thenAnswer(
                        invocation -> {
                            User saved = invocation.getArgument(0);
                            saved.id = 1L; // Simulate ID assignment during persist
                            return saved;
                        });

        // When
        User result = userService.createUser(newUser);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id).isEqualTo(1L);
        verify(userRepository).existsByEmail("new@example.com");
        verify(userRepository).save(newUser);
    }

    @Test
    void testCreateUserEmailExists() {
        // Given
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> userService.createUser(testUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already exists");

        verify(userRepository).existsByEmail("john@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testUpdateUserSuccess() {
        // Given
        User updatedData = new User("Updated Name", "updated@example.com", "Updated bio");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmailAndIdNot("updated@example.com", 1L)).thenReturn(false);
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = userService.updateUser(1L, updatedData);

        // Then
        assertThat(result.name).isEqualTo("Updated Name");
        assertThat(result.email).isEqualTo("updated@example.com");
        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
    }

    @Test
    void testUpdateUserNotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> userService.updateUser(999L, testUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");

        verify(userRepository).findById(999L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testUpdateUserEmailConflict() {
        // Given
        User updatedData = new User("Updated", "conflict@example.com", "Bio");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmailAndIdNot("conflict@example.com", 1L)).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> userService.updateUser(1L, updatedData))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already exists");

        verify(userRepository).existsByEmailAndIdNot("conflict@example.com", 1L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testUpdateUserSameEmail() {
        // Given
        User updatedData = new User("Updated Name", "john@example.com", "Updated bio");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = userService.updateUser(1L, updatedData);

        // Then
        assertThat(result.name).isEqualTo("Updated Name");
        verify(userRepository, never()).existsByEmailAndIdNot(anyString(), anyLong());
    }

    @Test
    void testDeleteUserSuccess() {
        // Given
        when(userRepository.deleteUser(1L)).thenReturn(true);

        // When
        boolean result = userService.deleteUser(1L);

        // Then
        assertThat(result).isTrue();
        verify(userRepository).deleteUser(1L);
    }

    @Test
    void testDeleteUserNotFound() {
        // Given
        when(userRepository.deleteUser(999L)).thenReturn(false);

        // When
        boolean result = userService.deleteUser(999L);

        // Then
        assertThat(result).isFalse();
        verify(userRepository).deleteUser(999L);
    }

    @Test
    void testSearchUsers() {
        // Given
        List<User> users = Arrays.asList(testUser);
        when(userRepository.searchByName("John")).thenReturn(users);

        // When
        List<User> result = userService.searchUsers("John");

        // Then
        assertThat(result).hasSize(1);
        verify(userRepository).searchByName("John");
    }

    @Test
    void testGetRecentUsers() {
        // Given
        List<User> users = Arrays.asList(testUser);
        when(userRepository.findRecentUsers(7)).thenReturn(users);

        // When
        List<User> result = userService.getRecentUsers(7);

        // Then
        assertThat(result).hasSize(1);
        verify(userRepository).findRecentUsers(7);
    }

    @Test
    void testGetUserCount() {
        // Given
        when(userRepository.countUsers()).thenReturn(5L);

        // When
        long result = userService.getUserCount();

        // Then
        assertThat(result).isEqualTo(5L);
        verify(userRepository).countUsers();
    }
}
