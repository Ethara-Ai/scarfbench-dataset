package br.com.arquivolivre.otelquarkus.repository;

import br.com.arquivolivre.otelquarkus.model.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repository for User entity operations. Uses Spring Data JPA for database operations. */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by email
     *
     * @param email User's email address
     * @return Optional containing user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Search users by name (case-insensitive partial match)
     *
     * @param name Name to search for
     * @return List of matching users
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<User> searchByName(@Param("name") String name);

    /**
     * Find users created on or after the given cutoff date
     *
     * @param cutoffDate Earliest creation date to include
     * @return List of recent users
     */
    List<User> findByCreatedAtGreaterThanEqual(LocalDateTime cutoffDate);

    /**
     * Find users created within the specified number of days
     *
     * @param days Number of days to look back
     * @return List of recent users
     */
    default List<User> findRecentUsers(int days) {
        return findByCreatedAtGreaterThanEqual(LocalDateTime.now().minusDays(days));
    }

    /**
     * Check if email already exists
     *
     * @param email Email to check
     * @return true if email exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Check if email exists for a different user (for update validation)
     *
     * @param email Email to check
     * @param id ID to exclude from check
     * @return true if email exists for a different user
     */
    boolean existsByEmailAndIdNot(String email, Long id);

    /**
     * Count total number of users
     *
     * @return Total user count
     */
    default long countUsers() {
        return count();
    }

    /**
     * Delete user by ID
     *
     * @param id User ID
     * @return true if deleted, false if not found
     */
    default boolean deleteUser(Long id) {
        if (!existsById(id)) {
            return false;
        }
        deleteById(id);
        return true;
    }
}
