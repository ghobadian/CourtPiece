package ir.msghobadian.models;

import org.junit.jupiter.api.Test;

import static ir.msghobadian.constants.Color.*;
import static ir.msghobadian.enums.Type.SPADE;
import static org.junit.jupiter.api.Assertions.assertEquals;


class TempTest {
    @Test
    public void test(){
        String BLACK_BACKGROUND = "\033[40m";  // BLACK
        String BLACK_BRIGHT = "\033[0;90m";  // BLACK

        System.out.println(BLACK + WHITE_BACKGROUND +  "fsadf");
    }
}