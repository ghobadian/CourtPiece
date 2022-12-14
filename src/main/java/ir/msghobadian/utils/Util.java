package ir.msghobadian.utils;

import ir.msghobadian.enums.Type;
import ir.msghobadian.models.Card;
import ir.msghobadian.models.Hand;
import ir.msghobadian.models.Player;
import lombok.SneakyThrows;

import java.io.*;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import static ir.msghobadian.constants.Color.RED;
import static ir.msghobadian.constants.Color.RESET;

public class Util {
    private static final Random RANDOM = new SecureRandom();
    private static final List<Card> allCards = generateCards();

    private static List<Card> generateCards() {
        List<Card> output = new ArrayList<>();
        for(Type type : Type.values()){
            for(int j=1;j <= 13;j++){
                output.add(Card.builder().number(j).type(type).build());
            }
        }
        return output;
    }

    public static List<Hand> generateHands(){
        List<Hand> allFourHands = new ArrayList<>();
        for(int playerNumber = 0 ; playerNumber < 4;playerNumber++){
            Hand hand = new Hand();
            IntStream.range(0,13).forEach(i -> {
                int rand = RANDOM.nextInt(allCards.size());
                Card card = allCards.remove(rand);
                hand.add(card);
            });
            allFourHands.add(hand);
        }
        return allFourHands;
    }

    public static void sendSignal(Socket socket, String signal) {
        try {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeUTF(signal);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e){
            System.err.println("socket is null");
        }
    }

    @SneakyThrows
    public static String receiveSignal(Socket socket) {
        try {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            return in.readUTF();
        } catch (IOException e) {
            System.err.println("Game not sending any response");
            Thread.sleep(1000);
        } catch (NullPointerException n){
            System.err.println("Socket is null");
        }
        return null;
    }

    public static void sendError(Socket socket, String errorMessage) {
        sendSignal(socket, RED + errorMessage + RESET);
    }

    public static void sendPlayer(Socket socket, Player player){//todo generify it
        try{
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(player);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Player receivePlayer(Socket socket){
        try{
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            return (Player) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
