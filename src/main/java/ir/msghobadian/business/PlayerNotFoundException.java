package ir.msghobadian.business;

public class PlayerNotFoundException extends RuntimeException {
    public PlayerNotFoundException(){
        super("Player not found");
    }
}
