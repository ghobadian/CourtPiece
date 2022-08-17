package ir.msghobadian.programs;

import ir.msghobadian.models.Player;

import java.security.SecureRandom;
import java.util.UUID;

public class PlayerProgram {
    public static void main(String[] args) {
        Player player = Player.builder()
                .name("ali" + new SecureRandom().nextInt(1000))
                .id(UUID.randomUUID()).build();
        player.connectToGame();
    }
}
