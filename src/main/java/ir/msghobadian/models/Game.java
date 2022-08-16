package ir.msghobadian.models;

import ir.msghobadian.business.PlayerNotFoundException;
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
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.IntStream;

import static ir.msghobadian.constants.GameConstants.GAME_PORT;
import static ir.msghobadian.enums.Status.PENDING;
import static ir.msghobadian.enums.Status.STARTED;
import static ir.msghobadian.enums.Type.*;
import static ir.msghobadian.utils.Util.*;

@Data
@RequiredArgsConstructor
public class Game {
    private final List<Team> teams = new ArrayList<>();
    private final Map<Socket, Player> socketAndPlayer = new HashMap<>();
    private final List<Player> players = new ArrayList<>();
    private Map<Player, Card> currentRoundCards = new LinkedHashMap<>();//todo save sequence of changing ruler
    private final Stack<Round> playedRounds = new Stack<>();
    private Status status = PENDING;
    private Type rule;
    private int roundNumber = 1;

    public void start(){
        loadGame();
        playGameUntilATeamWins();
        broadCast(teamWonGameMessage(findTotalWinner()));
    }

    private void playGameUntilATeamWins() {
        loadRule();
        while(roundsWonBelow7()){
            playRound();
        }
        roundNumber = 0;
    }

    private void loadRule() {
        broadCast("Rule: " + rule.getShape() + "\n");
    }

    private String teamWonGameMessage(Team team) {
        return team + "won game.";
    }

    private void loadGame() {
        buildTeams();
        List<Hand> hands = generateHands();
        giveEachPlayerFiveCards(hands);
        chooseARandomRuler();//todo change for nextGames
        askRulerToChooseTheRule();
        giveEachPlayerAllOfTheCards(hands);
        startAllConnections();
        setStatus(STARTED);
        broadCast("Game started");
    }

    private void chooseARandomRuler() {
        int randomNumber = new SecureRandom().nextInt(4);
        Player randomPlayer = players.get(randomNumber);
        randomPlayer.setRuler(true);
        broadCast(randomPlayer.getName() + " is now the ruler.");
    }

    private void giveEachPlayerFiveCards(List<Hand> hands) {
        IntStream.range(0, 4).forEach(i -> {
            Hand currentFullHand = hands.get(i);
            Hand fiveCardHand = Hand.builder().cards(currentFullHand.getCards().subList(0, 5)).build();
            players.get(i).setHand(fiveCardHand);
        });
    }

    private void askRulerToChooseTheRule() {
        Socket socket = findSocketOfRuler();
        showHand(socket);
        sendRuleOptions(socket);
        rule = letRulerChooseRule(socket);
    }

    private void sendRuleOptions(Socket socket) {
        String signal = """
                1) ♠
                2) ♥
                3) ♣
                4) ♦""";
        signal += "\nChoose A Rule: ";
        sendSignal(socket, signal);
    }

    @SneakyThrows
    private Socket findSocketOfRuler() {
        return socketAndPlayer.entrySet().stream().filter(set -> set.getValue().isRuler()).findFirst()
                .orElseThrow(() -> new Exception("Ruler not found.")).getKey();
    }

    private Type letRulerChooseRule(Socket socket) {
        int chosenOption = Integer.parseInt(Objects.requireNonNull(receiveSignal(socket)));
        return switch (chosenOption){
            case 1 -> SPADE;
            case 2 -> HEART;
            case 3 -> CLUB;
            case 4 -> DIAMOND;
            default -> {
                sendError(socket, "\nWrong Number");
                yield letRulerChooseRule(socket);
            }
        };
    }

    private void giveEachPlayerAllOfTheCards(List<Hand> hands) {
        IntStream.range(0, 4).forEach(i -> players.get(i).setHand(hands.get(i)));
    }



    private void buildTeams() {
        for(int i=0 ; i<2 ; i++) {
            List<Player> currentTeamPlayers = new ArrayList<>();
            for(int j=i ; j < i + 2;j++ ) {
                currentTeamPlayers.add(players.get(i+j));
            }
            Team team = Team.builder().players(currentTeamPlayers).build();
            teams.add(team);
        }
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
            Thread.sleep(1000);/// TODO: 8/16/22 user executer
        }
        Player winner = findWinner();
        Team winnerTeam = findTeamOfPlayer(winner);
        if(isWinnerTeamChange(winnerTeam))
            changeRuler();
        broadCast(playerWonRoundMessage(winner));
        winnerTeam.wonRound();
        playedRounds.add(Round.builder().winner(winner).playerAndCard(currentRoundCards).build());
        currentRoundCards.clear();
        roundNumber++;
    }

    private void changeRuler() {
        /// TODO: 8/16/22
    }

    private boolean isWinnerTeamChange(Team winnerTeam) {
        Player ruler = findRuler();
        Team rulerTeam = findTeamOfPlayer(ruler);
        return rulerTeam != winnerTeam;
    }

    private Player findRuler() {
        return players.stream().filter(Player::isRuler).findFirst()
                .orElseThrow(PlayerNotFoundException::new);
    }

    private String playerWonRoundMessage(Player player) {
        return  player.getName() + " won this round.";
    }

    @SneakyThrows
    private Team findTeamOfPlayer(Player player) {
        return teams.stream().filter(team -> team.getPlayers().contains(player))
                .findFirst().orElseThrow(() -> new Exception("player not found"));
    }

    @SneakyThrows
    private Player findWinner() {
        return currentRoundCards.keySet().stream().max(this::cardComparator)
                .orElseThrow(() -> new Exception("winner not found"));
    }

    private int cardComparator(Player player1, Player player2) {
        Type type1 =  currentRoundCards.get(player1).getType();
        Type type2 =  currentRoundCards.get(player2).getType();
        if(type1 == type2) {
            int number1 = currentRoundCards.get(player1).getNumber();
            int number2 = currentRoundCards.get(player2).getNumber();
            if(number1 == 1)
                number1 += 13;
            if(number2 == 1)
                number2 += 13;
            return number1 - number2;
        }else {
            if(type1 == rule) {
                return 1;
            } else if(type2 == rule) {
                return -1;
            } else {
                return 1;
            }
        }
    }

    private boolean roundsWonBelow7() {
        return teams.stream().filter(team -> team.getRoundsWon() < 7).count() == 2;
    }

    @SneakyThrows
    public void pend() {
        waitForPlayersToJoin();
        start();
    }

    private void waitForPlayersToJoin() throws IOException {
        ServerSocket serverSocket = new ServerSocket(GAME_PORT);
        int numberOfPlayers = 0;
        while(numberOfPlayers <4) {
            acceptPlayers(serverSocket);
            numberOfPlayers++;
        }
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
        new Thread(() -> showDetailsToPlayer(socket)).start();
    }

    @SneakyThrows
    private void receiveCommandFromPlayer(Socket socket) {
        while(true){
            playCard(socket);
        }
    }

    @SneakyThrows
    private void showDetailsToPlayer(Socket socket) {
        while(true){
            showNameOfPlayer(socket);
            showNameOfPartner(socket);
            showRule(socket);//
            showCurrentRoundNumber(socket);//
            showRoundsWon(socket);
            showGamesWon(socket);
            showHand(socket);
            showCurrentRoundPlayedCards(socket);//
            Thread.sleep(5000);
            clearScreen(socket);
            showCardIndexPlaceHolder(socket);
        }
    }

    private void showCardIndexPlaceHolder(Socket socket) {
        sendSignal(socket, "Card Index: \n");
    }

    private void clearScreen(Socket socket) {
        String message = "\n".repeat(10) + "=====================================================";
        sendSignal(socket, message);
    }

    private void showRule(Socket socket) {
        sendSignal(socket, "Rule: " + rule.getShape());
    }

    private void showNameOfPartner(Socket socket) {
        Player player = socketAndPlayer.get(socket);
        Player partner = findPartnerOfPlayer(player);
        sendSignal(socket, "Partner: " + partner.getName());//todo feature: choosing your partner
    }

    private Player findPartnerOfPlayer(Player player) {
        Team team = findTeamOfPlayer(player);
        return team.getPlayers().stream().filter(p -> !p.equals(player)).findFirst()
                .orElseThrow(() -> new RuntimeException("player not found"));

    }

    private void showNameOfPlayer(Socket socket) {
        Player player = socketAndPlayer.get(socket);
        sendSignal(socket, "Name: " + player.getName());
    }

    private void showCurrentRoundPlayedCards(Socket socket) {
        currentRoundCards.forEach((player, card) -> sendSignal(socket, playerPlayedCardMessage(player, card)));
    }

    private void showCurrentRoundNumber(Socket socket) {
        sendSignal(socket, "Round " + roundNumber);
    }

    private void showRoundsWon(Socket socket) {
        Player player = socketAndPlayer.get(socket);
        Team team = findTeamOfPlayer(player);
        int roundsWon = team.getRoundsWon();
        sendSignal(socket, "Rounds Won: " + roundsWon);
    }

    private void showGamesWon(Socket socket) {
        Player player = socketAndPlayer.get(socket);
        Team team = findTeamOfPlayer(player);
        int gamesWon = team.getGamesWon();
        sendSignal(socket, "Games Won: " + gamesWon);
    }

    @SneakyThrows
    private void playCard(Socket socket) {
        int cardIndex = Integer.parseInt(Objects.requireNonNull(receiveSignal(socket))) - 1;
        if (foundPlayingCardError(socket, cardIndex)) return;
        Player player = socketAndPlayer.get(socket);
        List<Card> cards = player.getHand().getCards();
        Card card = cards.get(cardIndex);
        currentRoundCards.put(player, card);
        cards.remove(cardIndex);
    }

    private String playerPlayedCardMessage(Player player, Card card) {
        return player.getName() + " -> " + CardDTO.builder().card(card).build();
    }

    private void broadCast(String message) {
        socketAndPlayer.keySet().forEach(socket -> sendSignal(socket, message));
    }

    private boolean foundPlayingCardError(Socket socket, int cardIndex) {
        Player player = socketAndPlayer.get(socket);
        List<Card> cards = player.getHand().getCards();//todo clean it
        Card card = cards.get(cardIndex);
        try{
            checkGameStarted();
            checkFirstPlayerPlayingCard(player);
            checkForbiddenCardType(card.getType(), player);
            checkTwoCardsFromSinglePlayer(player);
        }catch (Exception e){
            sendError(socket, e.getMessage());
            return true;
        }
        return false;
    }

    private void checkFirstPlayerPlayingCard(Player player) throws Exception {
        if(!currentRoundCards.isEmpty()) return;
        if (isFirstRound())
            checkRulerPlaysFirst(player);
        else
            checkPreviousRoundWinnerPlayFirst(player);
    }

    private void checkPreviousRoundWinnerPlayFirst(Player player) throws Exception {
        if(!playedRounds.peek().getWinner().equals(player))
            throw new Exception("You are not the previous round winner; So you can't play first.");

    }

    private void checkRulerPlaysFirst(Player player) throws Exception {
        if(!player.isRuler()) throw new Exception("Ruler must play first");
    }

    private boolean isFirstRound() {
        return roundNumber == 1;
    }

    private void checkGameStarted() throws Exception{
        if (status != STARTED) throw new Exception("game hasn't started yet");
    }

    private void checkTwoCardsFromSinglePlayer(Player player) {
        if (currentRoundCards.containsKey(player)) throw new RuntimeException("You can't put any more cards for this game");
    }

    private void checkForbiddenCardType(Type type, Player player) {
        List<Card> cards = currentRoundCards.values().stream().toList();
        if (cards.isEmpty())
            return;
        Card firstPlayedCard = cards.get(0);
        boolean sameType = firstPlayedCard.getType() == type;
        if (!sameType && player.hasCardOfType(firstPlayedCard.getType()))
            throw new RuntimeException("Forbidden Card Type. You should play " + firstPlayedCard.getType().getShape());
    }

    private void showHand(Socket socket) {
        Player player = socketAndPlayer.get(socket);
        Hand hand = player.getHand();
        HandDTO handDTO = HandDTO.builder().hand(hand).build();
        sendSignal(socket, handDTO.toString() + "\n");
    }

    private void sendMessageToPlayer(Socket socket) {
        while(true){
            Scanner scanner = new Scanner(System.in);
            sendSignal(socket, scanner.nextLine());
        }
    }


}
