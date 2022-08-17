package ir.msghobadian.enums;

import static ir.msghobadian.constants.Color.*;

public enum Type {
    SPADE("♠"),
    CLUB("♣"),
    HEART("♥"),
    DIAMOND("♦");

    private final String shape;
    Type(String shape) {
        this.shape = shape;
    }

    public String getShape() {
        boolean redType = this == HEART || this == DIAMOND;
        String color = redType ? RED : BLACK + WHITE_BACKGROUND;
        return color + shape + RESET;
    }
}