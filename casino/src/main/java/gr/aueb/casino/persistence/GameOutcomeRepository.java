package gr.aueb.casino.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import gr.aueb.casino.domain.GameOutcome;

public interface GameOutcomeRepository extends JpaRepository<GameOutcome, Short> {
    Optional<GameOutcome> findByName(String name);
}
