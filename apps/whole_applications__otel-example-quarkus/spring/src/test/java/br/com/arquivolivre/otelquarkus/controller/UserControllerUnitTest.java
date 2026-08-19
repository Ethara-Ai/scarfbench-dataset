package br.com.arquivolivre.otelquarkus.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.arquivolivre.otelquarkus.model.User;
import br.com.arquivolivre.otelquarkus.service.UserService;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

/**
 * Pure unit tests for UserController without the Spring test framework. This ensures JaCoCo
 * properly captures code coverage.
 */
@ExtendWith(MockitoExtension.class)
class UserControllerUnitTest {

    @Mock private UserService userService;

    @InjectMocks private UserController userController;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("John Doe", "john@example.com", "Software Developer");
        testUser.id = 1L;
    }

    @Test
    void testGetAllUsersSuccess() {
        // Given
        User user2 = new User("Jane Doe", "jane@example.com", "Designer");
        user2.id = 2L;
        List<User> users = Arrays.asList(testUser, user2);
        when(userService.getAllUsers()).thenReturn(users);

        // When
        ResponseEntity<Object> response = userController.getAllUsers();

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        List<User> result = (List<User>) response.getBody();
        assertThat(result).hasSize(2);
        verify(userService).getAllUsers();
    }

    @Test
    void testGetAllUsersEmptyList() {
        // Given
        when(userService.getAllUsers()).thenReturn(Collections.emptyList());

        // When
        ResponseEntity<Object> response = userController.getAllUsers();

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        List<User> result = (List<User>) response.getBody();
        assertThat(result).isEmpty();
        verify(userService).getAllUsers();
    }

    @Test
    void testGetUserByIdFound() {
        // Given
        when(userService.getUserById(1L)).thenReturn(Optional.of(testUser));

        // When
        ResponseEntity<Object> response = userController.getUserById(1L);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        User result = (User) response.getBody();
        assertThat(result.id).isEqualTo(1L);
        assertThat(result.email).isEqualTo("john@example.com");
        verify(userService).getUserById(1L);
    }

    @Test
    void testGetUserByIdNotFound() {
        // Given
        when(userService.getUserById(999L)).thenReturn(Optional.empty());

        // When
        ResponseEntity<Object> response = userController.getUserById(999L);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        @SuppressWarnings("unchecked")
        Map<String, String> error = (Map<String, String>) response.getBody();
        assertThat(error).containsEntry("error", "User not found with id: 999");
        assertThat(error).containsKey("timestamp");
        verify(userService).getUserById(999L);
    }

    @Test
    void testGetUserByEmailFound() {
        // Given
        when(userService.getUserByEmail("john@example.com")).thenReturn(Optional.of(testUser));

        // When
        ResponseEntity<Object> response = userController.getUserByEmail("john@example.com");

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        User result = (User) response.getBody();
        assertThat(result.email).isEqualTo("john@example.com");
        verify(userService).getUserByEmail("john@example.com");
    }

    @Test
    void testGetUserByEmailNotFound() {
        // Given
        when(userService.getUserByEmail("notfound@example.com")).thenReturn(Optional.empty());

        // When
        ResponseEntity<Object> response = userController.getUserByEmail("notfound@example.com");

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        @SuppressWarnings("unchecked")
        Map<String, String> error = (Map<String, String>) response.getBody();
        assertThat(error).containsEntry("error", "User not found with email: notfound@example.com");
        verify(userService).getUserByEmail("notfound@example.com");
    }

    @Test
    void testCreateUserSuccess() {
        // Given
        User newUser = new User("Alice", "alice@example.com", "Manager");
        User createdUser = new User("Alice", "alice@example.com", "Manager");
        createdUser.id = 5L;
        when(userService.createUser(any(User.class))).thenReturn(createdUser);

        // When
        ResponseEntity<Object> response = userController.createUser(newUser);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        User result = (User) response.getBody();
        assertThat(result.id).isEqualTo(5L);
        assertThat(result.email).isEqualTo("alice@example.com");
        verify(userService).createUser(newUser);
    }

    @Test
    void testCreateUserEmailAlreadyExists() {
        // Given
        User newUser = new User("Duplicate", "john@example.com", "Bio");
        when(userService.createUser(any(User.class)))
                .thenThrow(new IllegalArgumentException("Email already exists: john@example.com"));

        // When
        ResponseEntity<Object> response = userController.createUser(newUser);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        @SuppressWarnings("unchecked")
        Map<String, String> error = (Map<String, String>) response.getBody();
        assertThat(error).containsEntry("error", "Email already exists: john@example.com");
        verify(userService).createUser(newUser);
    }

    @Test
    void testUpdateUserSuccess() {
        // Given
        User updatedData = new User("John Updated", "john.updated@example.com", "Senior Dev");
        User updatedUser = new User("John Updated", "john.updated@example.com", "Senior Dev");
        updatedUser.id = 1L;
        when(userService.updateUser(eq(1L), any(User.class))).thenReturn(updatedUser);

        // When
        ResponseEntity<Object> response = userController.updateUser(1L, updatedData);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        User result = (User) response.getBody();
        assertThat(result.name).isEqualTo("John Updated");
        verify(userService).updateUser(1L, updatedData);
    }

    @Test
    void testUpdateUserNotFound() {
        // Given
        User updatedData = new User("Test", "test@example.com", "Bio");
        when(userService.updateUser(eq(999L), any(User.class)))
                .thenThrow(new IllegalArgumentException("User not found with id: 999"));

        // When
        ResponseEntity<Object> response = userController.updateUser(999L, updatedData);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        @SuppressWarnings("unchecked")
        Map<String, String> error = (Map<String, String>) response.getBody();
        assertThat(error).containsEntry("error", "User not found with id: 999");
        verify(userService).updateUser(999L, updatedData);
    }

    @Test
    void testUpdateUserEmailConflict() {
        // Given
        User updatedData = new User("John", "taken@example.com", "Bio");
        when(userService.updateUser(eq(1L), any(User.class)))
                .thenThrow(new IllegalArgumentException("Email already exists: taken@example.com"));

        // When
        ResponseEntity<Object> response = userController.updateUser(1L, updatedData);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        @SuppressWarnings("unchecked")
        Map<String, String> error = (Map<String, String>) response.getBody();
        assertThat(error).containsEntry("error", "Email already exists: taken@example.com");
        verify(userService).updateUser(1L, updatedData);
    }

    @Test
    void testDeleteUserSuccess() {
        // Given
        when(userService.deleteUser(1L)).thenReturn(true);

        // When
        ResponseEntity<Object> response = userController.deleteUser(1L);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(204);
        assertThat(response.getBody()).isNull();
        verify(userService).deleteUser(1L);
    }

    @Test
    void testDeleteUserNotFound() {
        // Given
        when(userService.deleteUser(999L)).thenReturn(false);

        // When
        ResponseEntity<Object> response = userController.deleteUser(999L);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        @SuppressWarnings("unchecked")
        Map<String, String> error = (Map<String, String>) response.getBody();
        assertThat(error).containsEntry("error", "User not found with id: 999");
        verify(userService).deleteUser(999L);
    }

    @Test
    void testSearchUsersWithResults() {
        // Given
        User user2 = new User("Johnny", "johnny@example.com", "Bio");
        user2.id = 2L;
        List<User> users = Arrays.asList(testUser, user2);
        when(userService.searchUsers("John")).thenReturn(users);

        // When
        ResponseEntity<Object> response = userController.searchUsers("John");

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        List<User> result = (List<User>) response.getBody();
        assertThat(result).hasSize(2);
        verify(userService).searchUsers("John");
    }

    @Test
    void testSearchUsersEmptyResults() {
        // Given
        when(userService.searchUsers("Nonexistent")).thenReturn(Collections.emptyList());

        // When
        ResponseEntity<Object> response = userController.searchUsers("Nonexistent");

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        List<User> result = (List<User>) response.getBody();
        assertThat(result).isEmpty();
        verify(userService).searchUsers("Nonexistent");
    }

    @Test
    void testSearchUsersNullQuery() {
        // When
        ResponseEntity<Object> response = userController.searchUsers(null);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        @SuppressWarnings("unchecked")
        Map<String, String> error = (Map<String, String>) response.getBody();
        assertThat(error).containsEntry("error", "Search query 'name' is required");
        verify(userService, never()).searchUsers(anyString());
    }

    @Test
    void testSearchUsersEmptyQuery() {
        // When
        ResponseEntity<Object> response = userController.searchUsers("   ");

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        @SuppressWarnings("unchecked")
        Map<String, String> error = (Map<String, String>) response.getBody();
        assertThat(error).containsEntry("error", "Search query 'name' is required");
        verify(userService, never()).searchUsers(anyString());
    }

    @Test
    void testGetRecentUsersWithDefaultDays() {
        // Given
        List<User> users = Arrays.asList(testUser);
        when(userService.getRecentUsers(7)).thenReturn(users);

        // When
        ResponseEntity<Object> response = userController.getRecentUsers(7);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        List<User> result = (List<User>) response.getBody();
        assertThat(result).hasSize(1);
        verify(userService).getRecentUsers(7);
    }

    @Test
    void testGetRecentUsersCustomDays() {
        // Given
        List<User> users = Arrays.asList(testUser);
        when(userService.getRecentUsers(30)).thenReturn(users);

        // When
        ResponseEntity<Object> response = userController.getRecentUsers(30);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        List<User> result = (List<User>) response.getBody();
        assertThat(result).hasSize(1);
        verify(userService).getRecentUsers(30);
    }

    @Test
    void testGetRecentUsersInvalidDays() {
        // When
        ResponseEntity<Object> response = userController.getRecentUsers(-1);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        @SuppressWarnings("unchecked")
        Map<String, String> error = (Map<String, String>) response.getBody();
        assertThat(error).containsEntry("error", "Days must be a positive number");
        verify(userService, never()).getRecentUsers(anyInt());
    }

    @Test
    void testGetRecentUsersZeroDays() {
        // When
        ResponseEntity<Object> response = userController.getRecentUsers(0);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        @SuppressWarnings("unchecked")
        Map<String, String> error = (Map<String, String>) response.getBody();
        assertThat(error).containsEntry("error", "Days must be a positive number");
        verify(userService, never()).getRecentUsers(anyInt());
    }

    @Test
    void testGetUserCountSuccess() {
        // Given
        when(userService.getUserCount()).thenReturn(42L);

        // When
        ResponseEntity<Object> response = userController.getUserCount();

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        Map<String, Long> result = (Map<String, Long>) response.getBody();
        assertThat(result).containsEntry("count", 42L);
        verify(userService).getUserCount();
    }

    @Test
    void testGetUserCountZero() {
        // Given
        when(userService.getUserCount()).thenReturn(0L);

        // When
        ResponseEntity<Object> response = userController.getUserCount();

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        Map<String, Long> result = (Map<String, Long>) response.getBody();
        assertThat(result).containsEntry("count", 0L);
        verify(userService).getUserCount();
    }

    @Test
    void testHealthCheckSuccess() {
        // When
        ResponseEntity<Object> response = userController.healthCheck();

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        Map<String, String> health = (Map<String, String>) response.getBody();
        assertThat(health).containsEntry("status", "UP");
        assertThat(health).containsEntry("service", "UserService");
        assertThat(health).containsKey("timestamp");
    }
}
