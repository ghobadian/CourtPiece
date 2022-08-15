package ir.msghobadian.models;

import lombok.*;
import org.json.JSONObject;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.util.Scanner;
import java.util.UUID;

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
        return new Socket("127.0.0.1", GAME_PORT);
    }

    @SneakyThrows//todo delete all
    private JSONObject createCommand(String command, String body) {
        JSONObject output = new JSONObject();
        output.put("command", command);
        output.put("body", body);
        return output;
    }

    private void keepConnection() {
        new Thread(this::sendCommandToGame).start();
        new Thread(this::receiveMessageFromGame).start();
    }

    private void receiveMessageFromGame() {
        while (true) {
            System.out.println(receiveSignal(socket));
        }
    }

    private void sendCommandToGame() {
        Scanner scanner = new Scanner(System.in);
        while(true){
            String cardIndex = scanner.nextLine();
            if(!foundCardIndexRegexError(cardIndex)) playCard(cardIndex);
        }
    }

    private boolean foundCardIndexRegexError(String cardIndex) {
        try {
            int card = Integer.parseInt(cardIndex);
            if(card < 1 || 13 < card) throw new Exception();
        } catch (Exception e){
            System.err.println("Wrong number");
            return true;
        }
        return false;
    }

    private void playCard(String cardNumber) {
        sendSignal(socket, cardNumber);
    }
}
