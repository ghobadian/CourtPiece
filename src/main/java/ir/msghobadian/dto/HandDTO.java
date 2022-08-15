package ir.msghobadian.dto;

import ir.msghobadian.models.Card;
import ir.msghobadian.models.Hand;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;
import java.util.stream.IntStream;


@AllArgsConstructor
@Builder
public class HandDTO {
    private Hand hand;
    @Override
    public String toString() {
        List<Card> cards = hand.getCards();
        StringBuilder output = new StringBuilder();
        IntStream.range(0, cards.size()).forEach(i -> {
            Card card = cards.get(i);
            CardDTO cardDTO = CardDTO.builder().card(card).build();
            output.append(i + 1).append(") ").append(cardDTO).append(" | ");
        });
        return output.toString();
    }
}
