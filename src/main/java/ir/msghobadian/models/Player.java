package ir.msghobadian.models;

import ir.msghobadian.enums.Type;
import lombok.*;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.util.Scanner;
import java.util.UUID;

import static ir.msghobadian.constants.GameConstants.GAME_ADDRESS;
import static ir.msghobadian.constants.GameConstants.GAME_PORT;
import static ir.msghobadian.utils.Util.*;

@AllArgsConstructor
@Builder
@Data
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
public class Player implements Serializable {
    private UUID id;
    private String name;
    private Hand hand;
    private boolean ruler;
    private transient Socket socket;

    @SneakyThrows
    public void connectToGame(){
        socket = createConnection();
        sendPlayerPropertiesToGame(this);
        keepConnection();
    }

    private void sendPlayerPropertiesToGame(Player player) {
        sendPlayer(socket, player);
    }

    private Socket createConnection() throws IOException {
        return new Socket(GAME_ADDRESS, GAME_PORT);
    }

    private void keepConnection() {
        new Thread(this::sendCommandToGame).start();
        new Thread(this::receiveMessageFromGame).start();
    }

    private void receiveMessageFromGame() {
        while (true) {
            handleCommand(receiveSignal(socket));
            System.out.println();
        }
    }

    private void handleCommand(String input) {
        System.out.print(input);
    }

    private void sendCommandToGame() {//todo update ruler after loss
        Scanner scanner = new Scanner(System.in);
        while(true){
            String cardIndex = scanner.nextLine();
            if (!foundCardIndexRegexError(cardIndex)) playCard(cardIndex);
        }
    }

    private boolean foundCardIndexRegexError(String cardIndex) {
        try {
            int card = Integer.parseInt(cardIndex);
            if (card < 1 || 13 < card) throw new Exception();
        } catch (Exception e){
            System.err.println("Wrong number");
            return true;
        }
        return false;
    }

    private void playCard(String cardNumber) {
        sendSignal(socket, cardNumber);
    }

    public boolean hasCardOfType(Type type) {
        return hand.getCards().stream().anyMatch(hand -> hand.getType()==type);
    }
}
