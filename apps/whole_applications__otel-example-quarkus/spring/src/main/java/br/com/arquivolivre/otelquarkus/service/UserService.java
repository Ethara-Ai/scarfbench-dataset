package br.com.arquivolivre.otelquarkus.service;

import br.com.arquivolivre.otelquarkus.model.User;
import br.com.arquivolivre.otelquarkus.repository.UserRepository;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongGauge;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for User business logic. Includes OpenTelemetry instrumentation for distributed
 * tracing and custom metrics via the OpenTelemetry Meter API.
 */
@Service
public class UserService {

    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);
    private static final AttributeKey<String> ERROR_TYPE = AttributeKey.stringKey("error.type");

    private final UserRepository userRepository;
    private final LongCounter userCreatedCounter;
    private final LongCounter userErrorsCounter;
    private final LongHistogram userSearchDuration;
    private final ObservableLongGauge usersTotalGauge;
    private final AtomicLong currentUserCount = new AtomicLong(0);

    /**
     * Constructor injection of the UserRepository and the OpenTelemetry Meter. All custom metrics
     * are registered here so that the service is fully initialized once the container returns, and
     * so it can be unit-tested by passing a no-op Meter.
     */
    public UserService(UserRepository userRepository, Meter meter) {
        this.userRepository = userRepository;

        this.userCreatedCounter =
                meter.counterBuilder("users.created.total")
                        .setDescription("Total number of users created")
                        .setUnit("1")
                        .build();

        this.userErrorsCounter =
                meter.counterBuilder("users.errors.total")
                        .setDescription("Total number of user-related errors")
                        .setUnit("1")
                        .build();

        this.userSearchDuration =
                meter.histogramBuilder("user.search.duration")
                        .ofLongs()
                        .setDescription("Duration of user search operations")
                        .setUnit("ms")
                        .build();

        // The gauge callback runs on the OTel PeriodicMetricReader background thread,
        // which has no active transaction or request context. Instead of querying the DB
        // there, we maintain an in-memory AtomicLong that is updated on create/delete and
        // seed it from the DB count at startup.
        this.currentUserCount.set(userRepository.countUsers());

        this.usersTotalGauge =
                meter.gaugeBuilder("users.total")
                        .ofLongs()
                        .setDescription("Current total number of users")
                        .setUnit("1")
                        .buildWithCallback(m -> m.record(currentUserCount.get()));
    }

    /**
     * Get all users
     *
     * @return List of all users
     */
    @WithSpan("UserService.getAllUsers")
    public List<User> getAllUsers() {
        LOG.info("Fetching all users");
        Span span = Span.current();

        List<User> users = userRepository.findAll();
        span.setAttribute("user.count", users.size());

        LOG.info("Retrieved {} users", users.size());
        return users;
    }

    /**
     * Get user by ID
     *
     * @param id User ID
     * @return Optional containing user if found
     */
    @WithSpan("UserService.getUserById")
    public Optional<User> getUserById(@SpanAttribute("user.id") Long id) {
        LOG.info("Fetching user with id: {}", id);
        Span span = Span.current();

        Optional<User> user = userRepository.findById(id);
        span.setAttribute("user.found", user.isPresent());

        if (user.isPresent()) {
            LOG.info("Found user: {}", user.get().email);
        } else {
            LOG.warn("User not found with id: {}", id);
        }

        return user;
    }

    /**
     * Get user by email
     *
     * @param email User email
     * @return Optional containing user if found
     */
    @WithSpan("UserService.getUserByEmail")
    public Optional<User> getUserByEmail(@SpanAttribute("user.email") String email) {
        LOG.info("Fetching user with email: {}", email);
        Span span = Span.current();

        Optional<User> user = userRepository.findByEmail(email);
        span.setAttribute("user.found", user.isPresent());

        if (user.isPresent()) {
            LOG.info("Found user with email: {}", email);
        } else {
            LOG.warn("User not found with email: {}", email);
        }

        return user;
    }

    /**
     * Create a new user
     *
     * @param user User to create
     * @return Created user
     * @throws IllegalArgumentException if email already exists
     */
    @Transactional
    @WithSpan("UserService.createUser")
    public User createUser(@SpanAttribute("user.email") User user) {
        LOG.info("Creating new user with email: {}", user.email);
        Span span = Span.current();
        span.setAttribute("user.name", user.name);

        // Check if email already exists
        if (userRepository.existsByEmail(user.email)) {
            LOG.error("Email already exists: {}", user.email);
            span.setAttribute("error", true);
            span.setAttribute("error.type", "duplicate_email");
            userErrorsCounter.add(1, Attributes.of(ERROR_TYPE, "duplicate_email"));
            throw new IllegalArgumentException("Email already exists: " + user.email);
        }

        User savedUser = userRepository.save(user);
        if (savedUser.id != null) {
            span.setAttribute("user.id", savedUser.id);
        }
        span.setAttribute("user.created", true);
        userCreatedCounter.add(1);
        currentUserCount.incrementAndGet();

        LOG.info("User created successfully with id: {}", savedUser.id);
        return savedUser;
    }

    /**
     * Update an existing user
     *
     * @param id User ID
     * @param updatedUser Updated user data
     * @return Updated user
     * @throws IllegalArgumentException if user not found or email conflict
     */
    @Transactional
    @WithSpan("UserService.updateUser")
    public User updateUser(@SpanAttribute("user.id") Long id, User updatedUser) {
        LOG.info("Updating user with id: {}", id);
        Span span = Span.current();
        span.setAttribute("user.email", updatedUser.email);

        User existingUser =
                userRepository
                        .findById(id)
                        .orElseThrow(
                                () -> {
                                    LOG.error("User not found with id: {}", id);
                                    span.setAttribute("error", true);
                                    span.setAttribute("error.type", "not_found");
                                    userErrorsCounter.add(
                                            1, Attributes.of(ERROR_TYPE, "not_found"));
                                    return new IllegalArgumentException(
                                            "User not found with id: " + id);
                                });

        // Check if email is being changed and if new email already exists
        if (!existingUser.email.equals(updatedUser.email)
                && userRepository.existsByEmailAndIdNot(updatedUser.email, id)) {
            LOG.error("Email already exists: {}", updatedUser.email);
            span.setAttribute("error", true);
            span.setAttribute("error.type", "duplicate_email");
            userErrorsCounter.add(1, Attributes.of(ERROR_TYPE, "duplicate_email"));
            throw new IllegalArgumentException("Email already exists: " + updatedUser.email);
        }

        // Update fields
        existingUser.name = updatedUser.name;
        existingUser.email = updatedUser.email;
        existingUser.bio = updatedUser.bio;

        User savedUser = userRepository.save(existingUser);
        span.setAttribute("user.updated", true);

        LOG.info("User updated successfully with id: {}", id);
        return savedUser;
    }

    /**
     * Delete a user
     *
     * @param id User ID
     * @return true if deleted, false if not found
     */
    @Transactional
    @WithSpan("UserService.deleteUser")
    public boolean deleteUser(@SpanAttribute("user.id") Long id) {
        LOG.info("Deleting user with id: {}", id);
        Span span = Span.current();

        boolean deleted = userRepository.deleteUser(id);
        span.setAttribute("user.deleted", deleted);

        if (deleted) {
            currentUserCount.decrementAndGet();
            LOG.info("User deleted successfully with id: {}", id);
        } else {
            LOG.warn("User not found for deletion with id: {}", id);
            span.setAttribute("error.type", "not_found");
        }

        return deleted;
    }

    /**
     * Search users by name
     *
     * @param name Name to search for
     * @return List of matching users
     */
    @WithSpan("UserService.searchUsers")
    public List<User> searchUsers(@SpanAttribute("search.query") String name) {
        LOG.info("Searching users with name: {}", name);
        Span span = Span.current();

        long start = System.nanoTime();
        List<User> users = userRepository.searchByName(name);
        userSearchDuration.record((System.nanoTime() - start) / 1_000_000);

        span.setAttribute("search.results", users.size());

        LOG.info("Found {} users matching name: {}", users.size(), name);
        return users;
    }

    /**
     * Get recent users
     *
     * @param days Number of days to look back
     * @return List of recent users
     */
    @WithSpan("UserService.getRecentUsers")
    public List<User> getRecentUsers(@SpanAttribute("days") int days) {
        LOG.info("Fetching users from last {} days", days);
        Span span = Span.current();

        List<User> users = userRepository.findRecentUsers(days);
        span.setAttribute("user.count", users.size());

        LOG.info("Found {} users from last {} days", users.size(), days);
        return users;
    }

    /**
     * Get total user count
     *
     * @return Total number of users
     */
    @WithSpan("UserService.getUserCount")
    public long getUserCount() {
        LOG.info("Fetching user count");

        long count = userRepository.countUsers();
        Span.current().setAttribute("user.count", count);

        LOG.info("Total user count: {}", count);
        return count;
    }
}
