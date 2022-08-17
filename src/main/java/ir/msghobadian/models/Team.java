package ir.msghobadian.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@AllArgsConstructor
@Data
@Builder
public class Team {
    private List<Player> players;
    private int roundsWon = 0;
    private int gamesWon = 0;

    public void wonRound() {
        roundsWon++;
    }
    public void wonGame() {
        gamesWon++;
    }
}
