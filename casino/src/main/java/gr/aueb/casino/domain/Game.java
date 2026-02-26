package gr.aueb.casino.domain;

import java.time.ZonedDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "games")
@Getter
@Setter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", nullable = false)
    private GameStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outcome_id")
    private GameOutcome outcome;

    @Column(name = "server_roll")
    private Short serverRoll;

    @Column(name = "client_roll")
    private Short clientRoll;

    @Column(name = "r_a", nullable = false, length = 64)
    private String serverNonce;

    @Column(name = "r_b", length = 64)
    private String clientNonce;

    @Column(name = "client_nonce_hash", nullable = false, length = 64)
    private String clientNonceHash;

    @Column(name = "server_nonce_hash", nullable = false, length = 64)
    private String serverNonceHash;

    @Column(name = "initiated_at", nullable = false)
    private ZonedDateTime initiatedAt;

    @Column(name = "completed_at")
    private ZonedDateTime completedAt;

    public Game(User user, GameStatus status, String serverNonce, String clientNonceHash, String serverNonceHash) {
        this.user = user;
        this.status = status;
        this.serverNonce = serverNonce;
        this.clientNonceHash = clientNonceHash;
        this.serverNonceHash = serverNonceHash;
        this.initiatedAt = ZonedDateTime.now();
    }
}
