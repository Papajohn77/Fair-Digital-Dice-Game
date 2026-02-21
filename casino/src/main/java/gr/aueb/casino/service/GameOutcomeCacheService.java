package gr.aueb.casino.service;

import lombok.Getter;
import org.springframework.stereotype.Service;

import gr.aueb.casino.domain.GameOutcome;
import gr.aueb.casino.persistence.GameOutcomeRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Getter
@Service
@RequiredArgsConstructor
public class GameOutcomeCacheService {
    private final GameOutcomeRepository gameOutcomeRepository;

    private GameOutcome serverWin;
    private GameOutcome clientWin;
    private GameOutcome tie;
    private GameOutcome expired;

    @PostConstruct
    public void init() {
        serverWin = gameOutcomeRepository.findByName("SERVER_WIN")
            .orElseThrow(() -> new IllegalStateException("Required game outcome SERVER_WIN not found."));
        clientWin = gameOutcomeRepository.findByName("CLIENT_WIN")
            .orElseThrow(() -> new IllegalStateException("Required game outcome CLIENT_WIN not found."));
        tie = gameOutcomeRepository.findByName("TIE")
            .orElseThrow(() -> new IllegalStateException("Required game outcome TIE not found."));
        expired = gameOutcomeRepository.findByName("EXPIRED")
            .orElseThrow(() -> new IllegalStateException("Required game outcome EXPIRED not found."));
    }
}
