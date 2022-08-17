package ir.msghobadian.models;

import ir.msghobadian.enums.Type;
import org.junit.jupiter.api.Test;


class TempTest {
    @Test
    public void test(){
        System.out.println(Type.CLUB.ordinal());
        System.out.println(Type.HEART.ordinal());
        System.out.println(Type.DIAMOND.ordinal());
        System.out.println(Type.SPADE.ordinal());
    }
}