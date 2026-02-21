package gr.aueb.casino.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import gr.aueb.casino.domain.Game;
import jakarta.persistence.LockModeType;

public interface GameRepository extends JpaRepository<Game, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM Game g WHERE g.id = :id")
    Optional<Game> findWithLockById(@Param("id") Long id);

    @Query("SELECT g FROM Game g WHERE g.user.id = :userId AND g.status.name = 'COMPLETED' ORDER BY g.completedAt DESC LIMIT 5")
    List<Game> findTop5CompletedByUserId(@Param("userId") Long userId);
}
