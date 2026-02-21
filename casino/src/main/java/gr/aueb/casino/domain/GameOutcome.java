package gr.aueb.casino.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "game_outcomes")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class GameOutcome {

    @Id
    private Short id;

    @Column(nullable = false, unique = true)
    private String name;
}
