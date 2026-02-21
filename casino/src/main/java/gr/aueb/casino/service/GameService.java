package gr.aueb.casino.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gr.aueb.casino.api.schemas.response.GameHistoryResponse;
import gr.aueb.casino.api.schemas.response.GuessResponse;
import gr.aueb.casino.api.schemas.response.InitiateGameResponse;
import gr.aueb.casino.domain.Game;
import gr.aueb.casino.domain.GameOutcome;
import gr.aueb.casino.exception.custom.GameAccessDeniedException;
import gr.aueb.casino.exception.custom.GameNotFoundException;
import gr.aueb.casino.persistence.GameRepository;
import gr.aueb.casino.persistence.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GameService {
    private static final long EXPIRATION_SECONDS = 60;

    private final GameRepository gameRepository;
    private final GameStatusCacheService gameStatusCache;
    private final GameOutcomeCacheService gameOutcomeCache;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom;

    @Transactional
    public InitiateGameResponse initiateGame(Long userId, String clientNonce) {
        var user = userRepository.getReferenceById(userId);

        String serverNonce = generateNonce();
        short serverRoll = generateDiceRoll();
        String hashCommitment = computeCommitment(serverRoll, serverNonce, clientNonce);

        Game game = new Game(user, gameStatusCache.getInProgress(), serverRoll, serverNonce, clientNonce, hashCommitment);
        game = gameRepository.save(game);
        return new InitiateGameResponse(game.getId(), game.getHashCommitment());
    }

    @Transactional
    public GuessResponse submitGuess(Long gameId, short clientRoll, Long userId) {
        Game game = gameRepository.findWithLockById(gameId)
            .orElseThrow(() -> new GameNotFoundException("Game with id: " + gameId + " not found."));

        if (!isOwnedBy(game, userId)) {
            throw new GameAccessDeniedException("User with id: " + userId + " does not have access to the game with id: " + gameId);
        }

        if (isCompleted(game)) {
            return new GuessResponse(
                game.getOutcome().getName(),
                game.getServerRoll(),
                game.getServerNonce()
            );
        }

        if (isExpired(game)) {
            game.setOutcome(gameOutcomeCache.getExpired());
            game.setStatus(gameStatusCache.getCompleted());
            game.setCompletedAt(ZonedDateTime.now());
            game = gameRepository.save(game);

            return new GuessResponse(
                game.getOutcome().getName(),
                game.getServerRoll(),
                game.getServerNonce()
            );
        }

        GameOutcome outcome = determineOutcome(game.getServerRoll(), clientRoll);
        game.setOutcome(outcome);
        game.setStatus(gameStatusCache.getCompleted());
        game.setCompletedAt(ZonedDateTime.now());
        game.setClientRoll(clientRoll);
        game = gameRepository.save(game);

        return new GuessResponse(
            game.getOutcome().getName(),
            game.getServerRoll(),
            game.getServerNonce()
        );
    }

    private String generateNonce() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private short generateDiceRoll() {
        return (short) (secureRandom.nextInt(6) + 1);
    }

    private String computeCommitment(short serverRoll, String serverNonce, String clientNonce) {
        String data = serverRoll + serverNonce + clientNonce;
        MessageDigest sha256 = getSha256();
        byte[] hash = sha256.digest(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    private MessageDigest getSha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("SHA-256 must be supported by JVM spec", e);
        }
    }

    private boolean isOwnedBy(Game game, Long userId) {
        return Objects.equals(game.getUser().getId(), userId);
    }

    private boolean isCompleted(Game game) {
        return Objects.equals(game.getStatus().getName(), "COMPLETED");
    }

    private boolean isExpired(Game game) {
        ZonedDateTime now = ZonedDateTime.now();
        Duration elapsed = Duration.between(game.getInitiatedAt(), now);
        return elapsed.getSeconds() > EXPIRATION_SECONDS;
    }

    private GameOutcome determineOutcome(short serverRoll, short clientRoll) {
        if (serverRoll > clientRoll) {
            return gameOutcomeCache.getServerWin();
        } else if (serverRoll == clientRoll) {
            return gameOutcomeCache.getTie();
        } else {
            return gameOutcomeCache.getClientWin();
        }
    }

    @Transactional(readOnly = true)
    public List<GameHistoryResponse> getRecentGames(Long userId) {
        return gameRepository.findTop5CompletedByUserId(userId).stream()
            .map(game -> new GameHistoryResponse(
                game.getServerRoll(),
                game.getClientRoll(),
                game.getOutcome().getName(),
                game.getCompletedAt()
            ))
            .toList();
    }
}
