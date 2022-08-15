package ir.msghobadian.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor
public class Hand implements Serializable {
    List<Card> cards = new ArrayList<>();

    public void add(Card card) {
        cards.add(card);
    }
}
