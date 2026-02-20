package gr.aueb.casino.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import gr.aueb.casino.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
