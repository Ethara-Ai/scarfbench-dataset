package org.acme.user;

import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA repository for User (replaces the former PanacheUserRepository). */
public interface JpaUserRepository extends JpaRepository<User, Long> {}
