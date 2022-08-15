package ir.msghobadian.dto;

import ir.msghobadian.enums.Type;
import ir.msghobadian.models.Card;
import lombok.AllArgsConstructor;
import lombok.Builder;

import static ir.msghobadian.constants.Color.*;
import static ir.msghobadian.enums.Type.*;

@Builder
@AllArgsConstructor
public class CardDTO {
    private Card card;

    @Override
    public String toString() {
        String cardNumber = sanitizeNumber(card.getNumber());
        return chooseColour(card.getType()) + cardNumber + card.getType().getShape() + RESET;
    }

    private String sanitizeNumber(int number) {
        return String.valueOf(switch (number) {
            case 1 -> "";
            case 11 -> "J";
            case 12 -> "Q";
            case 13 -> "K";
            default -> number;
        });
    }

    private String chooseColour(Type type) {
        boolean redType = type == HEART || type == DIAMOND;
        return redType ? RED : BLACK + WHITE_BACKGROUND;
    }
}
