package ir.msghobadian.models;

import ir.msghobadian.dto.CardDTO;
import ir.msghobadian.dto.HandDTO;
import ir.msghobadian.enums.Status;
import ir.msghobadian.enums.Type;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.stream.IntStream;

import static ir.msghobadian.constants.GameConstants.GAME_PORT;
import static ir.msghobadian.enums.Status.PENDING;
import static ir.msghobadian.enums.Status.STARTED;
import static ir.msghobadian.utils.Util.*;

@Data
@RequiredArgsConstructor
public class Game {
    private final List<Team> teams = generateTeams();
    private final Map<Socket, Player> socketAndPlayer = new HashMap<>();
    private final List<Player> players = new ArrayList<>();
    private Map<Player, Card> currentRoundCards = new HashMap<>();
    private Status status = PENDING;

    public void start(){
        buildTeams();
        giveHands();
        startAllConnections();
        setStatus(STARTED);
        System.out.println("gameStarted");
        while(roundsWonBelow7()){
            playRound();
        }
        broadCast(teamWonRoundMessage(findTotalWinner()));
    }

    private void buildTeams() {
        for(int i=0 ; i<2 ; i++) {
            List<Player> currentTeamPlayers = new ArrayList<>();
            for(int j=0 ; j<2;j++ ) {
                currentTeamPlayers.add(players.get(i+j));
            }
            Team team = Team.builder().players(currentTeamPlayers).build();

            teams.add(team);
        }
    }

    private void giveHands() {
        List<Hand> hands = generateHands();
        IntStream.range(0, 4).forEach(i -> players.get(i).setHand(hands.get(i)));
    }

    @SneakyThrows
    private Team findTotalWinner() {
        return teams.stream().filter(team -> team.getRoundsWon() == 7).findFirst().orElseThrow(() -> new Exception("Nobody has one this round yet"));
    }

    private void startAllConnections() {//todo sort cards by type and number;
        socketAndPlayer.keySet().forEach(this::keepConnectionToThePlayer);
    }

    @SneakyThrows
    private void playRound() {
        while(currentRoundCards.size()<4){
            Thread.sleep(1000);
        }
        Team winnerTeam = findTeamOfPlayer(findWinner());
        broadCast(teamWonRoundMessage(winnerTeam));
        winnerTeam.addPoint();
        currentRoundCards.clear();
    }

    private String teamWonRoundMessage(Team team) {
        List<Player> players =  team.getPlayers();
        String player1Name = players.get(0).getName();
        String player2Name = players.get(1).getName();
        return  player1Name + " and " + player2Name + " won this round.";
    }

    @SneakyThrows
    private Team findTeamOfPlayer(Player player) {
        return teams.stream().filter(team -> team.getPlayers().contains(player))
                .findFirst().orElseThrow(() -> new Exception("player not found"));
    }

    @SneakyThrows
    private Player findWinner() {
        return currentRoundCards.keySet().stream().max((key1,key2) -> {
            int number1 = currentRoundCards.get(key1).getNumber();
            int number2 = currentRoundCards.get(key2).getNumber();
            return number1 - number2;
        }).orElseThrow(() -> new Exception("winner not found"));
    }

    private boolean roundsWonBelow7() {
        return teams.stream().filter(team -> team.getCurrentRoundPoints() < 7).count() == 2;
    }

    @SneakyThrows
    public void pend() {
        int numberOfPlayers = 0;
        ServerSocket serverSocket = new ServerSocket(GAME_PORT);
        while(numberOfPlayers<4) {
            acceptPlayers(serverSocket);
            numberOfPlayers++;
        }
        start();
    }

    private void acceptPlayers(ServerSocket serverSocket) throws IOException {
        Socket socket = serverSocket.accept();
        Player player = receivePlayer(socket);//todo there are 2 different players: one in Game and one in Player
        savePlayerProperties(socket, player);
        broadCast("Player " + player.getName() + " joined the game.");
    }

    private void savePlayerProperties(Socket socket, Player player) {
        players.add(player);
        saveSocketAndPlayer(socket, player);
    }

    private void saveSocketAndPlayer(Socket socket, Player player) {
        socketAndPlayer.put(socket, player);
    }

    private void keepConnectionToThePlayer(Socket socket) {
        new Thread(() -> sendMessageToPlayer(socket)).start();
        new Thread(() -> receiveCommandFromPlayer(socket)).start();
    }

    @SneakyThrows
    private void receiveCommandFromPlayer(Socket socket) {
        while(true){
            showHand(socket);
            int command = Integer.parseInt(Objects.requireNonNull(receiveSignal(socket))) - 1;
            if(status == STARTED) playCard(socket, command);
        }
    }

    @SneakyThrows
    private void playCard(Socket socket, int cardIndex) {
        Player player = socketAndPlayer.get(socket);
        List<Card> cards = player.getHand().getCards();
        Card card = cards.get(cardIndex);
        if(foundPuttingCardError(player, card)) return;
        currentRoundCards.put(player, card);
        broadCast(player.getName() + " -> " + CardDTO.builder().card(card).build());
        cards.remove(cardIndex);
    }

    private void broadCast(String message) {
        socketAndPlayer.keySet().forEach(socket -> sendSignal(socket, message));
    }

    private boolean foundPuttingCardError(Player player, Card card) {
        try{
            checkForbiddenCardType(card.getType());
            checkTwoCardsFromSinglePlayer(player);
        }catch (Exception e){
            sendError(findSocketByPlayer(player), e.getMessage());
            return true;
        }
        return false;
    }

    @SneakyThrows
    private Socket findSocketByPlayer(Player player) {
        return socketAndPlayer.keySet().stream().filter(socket -> socketAndPlayer.get(socket).equals(player))
                .findFirst().orElseThrow(() -> new Exception("socket with specified player not found"));
    }

    private void checkTwoCardsFromSinglePlayer(Player player) {
        if(currentRoundCards.containsKey(player)) throw new RuntimeException("You can't put any more cards for this game");
    }

    private void checkForbiddenCardType(Type type) {
        List<Card> cards = currentRoundCards.values().stream().toList();
        if(cards.isEmpty()) return;
        Card firstPlayedCard = cards.get(0);
        boolean sameType = firstPlayedCard.getType() == type;
        if(!sameType) throw new RuntimeException("Forbidden Card Type. You should play " + firstPlayedCard.getType().getShape());
    }

    private void showHand(Socket socket) {
        Hand hand = socketAndPlayer.get(socket).getHand();
        HandDTO handDTO = HandDTO.builder().hand(hand).build();
        sendSignal(socket, handDTO.toString());
    }

    private void sendMessageToPlayer(Socket socket) {
        while(true){
            Scanner scanner = new Scanner(System.in);
            sendSignal(socket, scanner.nextLine());
        }
    }


}
