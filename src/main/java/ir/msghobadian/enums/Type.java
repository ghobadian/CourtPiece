package ir.msghobadian.enums;

import java.io.Serializable;

public enum Type {
    SPADE("♠"),
    HEART("♥"),
    CLUB("♣"),
    DIAMOND("♦");

    private String shape;
    Type(String shape) {
        this.shape = shape;
    }

    public String getShape(){
        return shape;
    }
}