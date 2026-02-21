package gr.aueb.casino.service;

import lombok.Getter;
import org.springframework.stereotype.Service;

import gr.aueb.casino.domain.GameStatus;
import gr.aueb.casino.persistence.GameStatusRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Getter
@Service
@RequiredArgsConstructor
public class GameStatusCacheService {
    private final GameStatusRepository gameStatusRepository;

    private GameStatus inProgress;
    private GameStatus completed;

    @PostConstruct
    public void init() {
        inProgress = gameStatusRepository.findByName("IN_PROGRESS")
            .orElseThrow(() -> new IllegalStateException("Required game status IN_PROGRESS not found."));
        completed = gameStatusRepository.findByName("COMPLETED")
            .orElseThrow(() -> new IllegalStateException("Required game status COMPLETED not found."));
    }
}
