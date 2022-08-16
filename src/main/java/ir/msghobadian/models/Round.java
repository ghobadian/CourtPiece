package ir.msghobadian.models;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Builder
@Data
public class Round {
    private Map<Player, Card> playerAndCard;
    private Player winner;
}
