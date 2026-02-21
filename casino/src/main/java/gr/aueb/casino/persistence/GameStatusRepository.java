package gr.aueb.casino.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import gr.aueb.casino.domain.GameStatus;

public interface GameStatusRepository extends JpaRepository<GameStatus, Short> {
    Optional<GameStatus> findByName(String name);
}
