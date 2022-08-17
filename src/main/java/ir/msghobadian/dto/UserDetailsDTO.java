package ir.msghobadian.dto;

import ir.msghobadian.enums.Type;
import ir.msghobadian.models.Card;
import ir.msghobadian.models.Player;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Builder
@RequiredArgsConstructor
public class UserDetailsDTO {
    private final String name;
    private final String partnerName;
    private final int turnNumber;
    private final Type rule;
    private final int roundNumber;
    private final int roundsWon;
    private final int gamesWon;
    private final HandDTO hand;
    private final Map<Player, Card> playedCards;
    private final boolean myTurn;

    @Override
    public String toString() {
        return roundNumber + ") " + "Name: " + name + " | Partner: " + partnerName + " | " +
                "Rule: " + rule.getShape() + " | " + "Round: " + roundNumber + " | " +
                "Rounds Won: " + roundsWon + " | " + "Games Won: " + gamesWon + "\n" +
                hand + "\n" + findPlayedCards() +
                (myTurn ? "It's your turn\n" : "") + "Card Index: ";
    }


    private String findPlayedCards() {
        StringBuilder output = new StringBuilder();
        playedCards.forEach((player, card) -> output.append(playerPlayedCardMessage(player, card)));
        return output.toString();
    }

    private String playerPlayedCardMessage(Player player, Card card) {
        return player.getName() + " -> " + CardDTO.builder().card(card).build() + "\n";
    }
}
