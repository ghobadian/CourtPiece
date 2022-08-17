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
    public void connectToGame() {
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
            String response = receiveSignal(socket);
            System.out.print(response);
        }
    }

    private void sendCommandToGame() {
        Scanner scanner = new Scanner(System.in);
        while(true) {
            String command = scanner.nextLine();
            sendSignal(socket, command);
        }
    }

    public boolean hasCardOfType(Type type) {
        return hand.getCards().stream().anyMatch(hand -> hand.getType()==type);
    }
}
