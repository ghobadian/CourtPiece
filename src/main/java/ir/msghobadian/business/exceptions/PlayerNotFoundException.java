package ir.msghobadian.business.exceptions;

public class PlayerNotFoundException extends RuntimeException {
    public PlayerNotFoundException(){
        super("Player not found");
    }
}
