package br.com.arquivolivre.otelquarkus.controller;

import br.com.arquivolivre.otelquarkus.model.User;
import br.com.arquivolivre.otelquarkus.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoint for User CRUD operations. Uses Spring MVC annotations; behavior (routes, status
 * codes, payload shapes, error messages) mirrors the original JAX-RS resource.
 */
@RestController
@RequestMapping(value = "/api/users", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "User Management", description = "CRUD operations for users")
public class UserController {

    private static final Logger LOG = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Get all users", description = "Retrieve a list of all users")
    @ApiResponse(
            responseCode = "200",
            description = "Success",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = User.class)))
    public ResponseEntity<Object> getAllUsers() {
        LOG.info("GET /api/users - Fetching all users");
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieve a specific user by their ID")
    @ApiResponse(
            responseCode = "200",
            description = "User found",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = User.class)))
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<Object> getUserById(
            @Parameter(description = "User ID", required = true) @PathVariable("id") Long id) {
        LOG.info("GET /api/users/{} - Fetching user by id", id);
        return userService
                .getUserById(id)
                .<ResponseEntity<Object>>map(ResponseEntity::ok)
                .orElse(
                        ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(createErrorResponse("User not found with id: " + id)));
    }

    @GetMapping("/email/{email}")
    @Operation(
            summary = "Get user by email",
            description = "Retrieve a specific user by their email address")
    @ApiResponse(
            responseCode = "200",
            description = "User found",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = User.class)))
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<Object> getUserByEmail(
            @Parameter(description = "User email", required = true) @PathVariable("email")
                    String email) {
        LOG.info("GET /api/users/email/{} - Fetching user by email", email);
        return userService
                .getUserByEmail(email)
                .<ResponseEntity<Object>>map(ResponseEntity::ok)
                .orElse(
                        ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(createErrorResponse("User not found with email: " + email)));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create user", description = "Create a new user")
    @ApiResponse(
            responseCode = "201",
            description = "User created",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = User.class)))
    @ApiResponse(responseCode = "400", description = "Invalid input or email already exists")
    public ResponseEntity<Object> createUser(@Valid @RequestBody User user) {
        LOG.info("POST /api/users - Creating user with email: {}", user.email);
        try {
            User createdUser = userService.createUser(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (IllegalArgumentException e) {
            LOG.error("Error creating user", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update user", description = "Update an existing user")
    @ApiResponse(
            responseCode = "200",
            description = "User updated",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = User.class)))
    @ApiResponse(responseCode = "400", description = "Invalid input or email conflict")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<Object> updateUser(
            @Parameter(description = "User ID", required = true) @PathVariable("id") Long id,
            @Valid @RequestBody User user) {
        LOG.info("PUT /api/users/{} - Updating user", id);
        try {
            User updatedUser = userService.updateUser(id, user);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            LOG.error("Error updating user", e);
            HttpStatus status =
                    e.getMessage().contains("not found")
                            ? HttpStatus.NOT_FOUND
                            : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(createErrorResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Delete a user by ID")
    @ApiResponse(responseCode = "204", description = "User deleted")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<Object> deleteUser(
            @Parameter(description = "User ID", required = true) @PathVariable("id") Long id) {
        LOG.info("DELETE /api/users/{} - Deleting user", id);
        boolean deleted = userService.deleteUser(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("User not found with id: " + id));
        }
    }

    @GetMapping("/search")
    @Operation(summary = "Search users", description = "Search users by name (partial match)")
    @ApiResponse(
            responseCode = "200",
            description = "Success",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = User.class)))
    public ResponseEntity<Object> searchUsers(
            @Parameter(description = "Search query", required = true)
                    @RequestParam(value = "name", required = false)
                    String name) {
        LOG.info("GET /api/users/search?name={} - Searching users", name);
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("Search query 'name' is required"));
        }
        List<User> users = userService.searchUsers(name);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/recent")
    @Operation(
            summary = "Get recent users",
            description = "Get users created within the specified number of days")
    @ApiResponse(
            responseCode = "200",
            description = "Success",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = User.class)))
    public ResponseEntity<Object> getRecentUsers(
            @Parameter(description = "Number of days")
                    @RequestParam(value = "days", defaultValue = "7")
                    int days) {
        LOG.info("GET /api/users/recent?days={} - Fetching recent users", days);
        if (days <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("Days must be a positive number"));
        }
        List<User> users = userService.getRecentUsers(days);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/count")
    @Operation(summary = "Get user count", description = "Get the total number of users")
    @ApiResponse(responseCode = "200", description = "Success")
    public ResponseEntity<Object> getUserCount() {
        LOG.info("GET /api/users/count - Fetching user count");
        long count = userService.getUserCount();
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if the user service is healthy")
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    public ResponseEntity<Object> healthCheck() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "UserService");
        health.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return ResponseEntity.ok(health);
    }

    /** Mirrors the 400 response Quarkus returned for bean-validation failures. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationErrors(MethodArgumentNotValidException e) {
        StringBuilder message = new StringBuilder();
        e.getBindingResult()
                .getFieldErrors()
                .forEach(
                        fieldError -> {
                            if (message.length() > 0) {
                                message.append(", ");
                            }
                            message.append(fieldError.getField())
                                    .append(": ")
                                    .append(fieldError.getDefaultMessage());
                        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse(message.toString()));
    }

    /** Helper method to create error response */
    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        error.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return error;
    }
}
