package ir.msghobadian.models;

import ir.msghobadian.business.exceptions.PlayerNotFoundException;
import ir.msghobadian.dto.HandDTO;
import ir.msghobadian.dto.UserDetailsDTO;
import ir.msghobadian.enums.Status;
import ir.msghobadian.enums.Type;
import ir.msghobadian.utils.Util;
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
    private Map<Player, Card> currentRoundCards = new LinkedHashMap<>();
    private final Stack<Round> playedRounds = new Stack<>();
    private Status status = PENDING;
    private Type rule;
    private int roundNumber = 1;

    public void start(){
        loadFirstGame();
        while(true){
            playGameUntilATeamWins();
        }
    }

    private void playGameUntilATeamWins() {
        while(roundsWonBelow7()){
            showDetailsToAllPlayers();
            playRound();
        }
        broadCast(teamWonGameMessage(findTotalWinner()));
        resetGame();
    }

    private void resetGame() {
        loadGame();

    }

    private String teamWonGameMessage(Team team) {
        List<Player> players = team.getPlayers();
        Player player1 = players.get(0);
        Player player2 = players.get(1);
        return player1.getName() + " and " + player2.getName() + " won game.";
    }

    private void loadFirstGame() {
        chooseARandomRuler();
        loadGame();
        buildTeams();
        startAllConnections();
        setStatus(STARTED);
        broadCast("Game started");
    }

    private void loadGame() {
        System.out.println(currentRoundCards);/// TODO: 8/17/22 clear
        System.out.println(roundNumber);
        System.out.println(playedRounds);
        roundNumber = 1;
        teams.forEach(team -> team.setRoundsWon(0));
        List<Hand> hands = generateHands();
        giveEachPlayerFiveCards(hands);
        askRulerToChooseTheRule();
        giveEachPlayerAllOfTheCards(hands);

    }

    private void chooseARandomRuler() {
        int randomNumber = new SecureRandom().nextInt(4);
        Player randomPlayer = players.get(randomNumber);
        randomPlayer.setRuler(true);
        broadCast(randomPlayer.getName() + " is now the ruler.\n");
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
        sendSignal(socket, "\n" + findHand(socket).toString());
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
        IntStream.range(0, 4).forEach(i -> {
            Hand hand = sortHand(hands, i);
            players.get(i).setHand(hand);
        });
    }

    private Hand sortHand(List<Hand> hands, int i) {
        Hand hand = hands.get(i);
        hand.getCards().sort(Util::cardSortingComparator);
        return hand;
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
        while(currentRoundCards.size() < 4){
            Thread.sleep(500);
            /// TODO: 8/16/22 user executer
        }
        Player winner = findWinner();
        Team winnerTeam = findTeamOfPlayer(winner);
        broadCast(playerWonRoundMessage(winner));
        winnerTeam.wonRound();
        playedRounds.add(Round.builder().winner(winner).playerAndCard(currentRoundCards).build());
        currentRoundCards.clear();
        roundNumber++;
    }

    private Player findPlayerAfter(Player player) {
        int indexOfPlayer = players.indexOf(player);
        int indexOfNextPlayer = (indexOfPlayer + 1) % 4;
        return players.get(indexOfNextPlayer);
    }

    private Player findPlayerBefore(Player player) {
        int indexOfPlayer = players.indexOf(player);
        int indexOfPreviousPlayer = (indexOfPlayer - 1);
        if(indexOfPreviousPlayer == -1)
            indexOfPreviousPlayer = 3;
        return players.get(indexOfPreviousPlayer);
    }

    private boolean isWinnerTeamChanged(Team winnerTeam) {
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
        return currentRoundCards.keySet().stream().max((player1, player2) -> {
            Card card1 = currentRoundCards.get(player1);
            Card card2 = currentRoundCards.get(player2);
            return cardComparator(card1, card2);
        })
                .orElseThrow(() -> new Exception("winner not found"));
    }

    public int cardComparator(Card card1, Card card2) {
        Type type1 =  card1.getType();
        Type type2 =  card2.getType();
        int number1 = card1.getNumber();
        int number2 = card2.getNumber();
        return type1 == type2 ? compareSameTypeCards(number1, number2) : compareDifferentTypeCards(type1, type2);
    }

    private int compareDifferentTypeCards(Type type1, Type type2) {
        if(type1 == rule) {
            return 1;
        } else if(type2 == rule) {
            return -1;
        } else {
            return 1;
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
        broadCast("Player " + player.getName() + " joined the game.\n");
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
            String response = receiveSignal(socket);
            try {
                checkRegex(response);/// TODO: 8/17/22 clean it
            } catch (RuntimeException e) {
                sendError(socket, response);
                continue;
            }
            int cardIndex = Integer.parseInt(Objects.requireNonNull(response)) - 1;
            playCard(socket, cardIndex);
        }
    }

    private void checkRegex(String response) {
        if (foundCardIndexRegexError(response))
            throw new RuntimeException("Wrong Answer");
    }

    private boolean foundCardIndexRegexError(String cardIndex) {
        try {
            int card = Integer.parseInt(cardIndex);
            if (card < 1 || 13 < card) throw new Exception();
        } catch (Exception e){
            return true;
        }
        return false;
    }

    @SneakyThrows
    private void showDetailsToPlayer(Socket socket) {
        clearScreen(socket);
        UserDetailsDTO details = findDetailsOfPlayer(socket);
        sendSignal(socket, details.toString());
    }

    private UserDetailsDTO findDetailsOfPlayer(Socket socket) {
        int turn = findTablePlace(socket);
        String name = findNameOfPlayer(socket);
        String partnerName = findNameOfPartner(socket);
        int roundsWon = findRoundsWon(socket);
        int gamesWon = findGamesWon(socket);
        HandDTO hand = findHand(socket);
        boolean playerTurn = isPlayerTurn(socket);
        return UserDetailsDTO.builder().name(name).partnerName(partnerName).turnNumber(turn)
                .roundsWon(roundsWon).rule(rule).gamesWon(gamesWon).hand(hand).myTurn(playerTurn)
                .roundNumber(roundNumber).playedCards(currentRoundCards).build();
    }

    private int findTablePlace(Socket socket) {
        Player player = socketAndPlayer.get(socket);
        int playerIndex = players.indexOf(player);
        return playerIndex + 1;
    }

    private boolean isPlayerTurn(Socket socket) {//todo clean it
        Player player = socketAndPlayer.get(socket);
        if(currentRoundCards.isEmpty() && player.equals(findRuler()) && roundNumber == 1) {
            return true;//todo fix this shit
        } else if(currentRoundCards.isEmpty() && !playedRounds.isEmpty() && player.equals(playedRounds.peek().getWinner())) {
            return true;
        } else {
            Player previousPlayer = findPlayerBefore(player);
            return currentRoundCards.containsKey(previousPlayer) && !currentRoundCards.containsKey(player);
        }
    }

    private void clearScreen(Socket socket) {
        String message = "\n".repeat(10) + "=====================================================" + "\n";
        sendSignal(socket, message);
    }

    private String findNameOfPartner(Socket socket) {
        Player player = socketAndPlayer.get(socket);
        Player partner = findPartnerOfPlayer(player);
        return partner.getName();//todo feature: choosing your partner
    }

    private Player findPartnerOfPlayer(Player player) {
        Team team = findTeamOfPlayer(player);
        return team.getPlayers().stream().filter(p -> !p.equals(player)).findFirst()
                .orElseThrow(() -> new RuntimeException("player not found"));

    }

    private String findNameOfPlayer(Socket socket) {
        Player player = socketAndPlayer.get(socket);
        return player.getName();
    }

    private int findRoundsWon(Socket socket) {
        Player player = socketAndPlayer.get(socket);
        Team team = findTeamOfPlayer(player);
        return team.getRoundsWon();
    }

    private int findGamesWon(Socket socket) {
        Player player = socketAndPlayer.get(socket);
        Team team = findTeamOfPlayer(player);
        return team.getGamesWon();
    }

    @SneakyThrows
    private void playCard(Socket socket, int cardIndex) {
        if (foundPlayingCardError(socket, cardIndex)) return;
        Player player = socketAndPlayer.get(socket);
        List<Card> cards = player.getHand().getCards();
        Card card = cards.get(cardIndex);
        currentRoundCards.put(player, card);
        cards.remove(cardIndex);
        showDetailsToAllPlayers();
    }

    private void showDetailsToAllPlayers() {
        socketAndPlayer.keySet().forEach(this::showDetailsToPlayer);
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
            checkTurnOfPlayer(player);
            checkForbiddenCardType(card.getType(), player);
            checkTwoCardsFromSinglePlayer(player);
        }catch (Exception e){
            sendError(socket, e.getMessage());
            return true;
        }
        return false;
    }

    private void checkTurnOfPlayer(Player player) {
        if(currentRoundCards.isEmpty()) {
            if(playedRounds.isEmpty() && player.isRuler()) {
                return;
            }
            Player previousRoundWinner = playedRounds.peek().getWinner();
            if(player.equals(previousRoundWinner)) {
                return;
            }
        }

        Player previousPlayer = findPlayerBefore(player);
        if(!currentRoundCards.containsKey(previousPlayer))
            throw new RuntimeException("It's not Your turn");
    }

    private void checkFirstPlayerPlayingCard(Player player) throws Exception {
        if(!currentRoundCards.isEmpty()) return;
        if (isFirstRound())
            checkRulerPlaysFirst(player);
        else
            checkPreviousRoundWinnerPlayFirst(player);
    }

    private void checkPreviousRoundWinnerPlayFirst(Player player) throws Exception {
        Player winnerOfPreviousRound = findWinnerOfPreviousRound();
        if(!winnerOfPreviousRound.equals(player))
            throw new Exception(winnerOfPreviousRound.getName() + " should play first");

    }

    private Player findWinnerOfPreviousRound() {
        return playedRounds.peek().getWinner();
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

    private HandDTO findHand(Socket socket) {
        Player player = socketAndPlayer.get(socket);
        Hand hand = player.getHand();
        return HandDTO.builder().hand(hand).build();
    }

    private void sendMessageToPlayer(Socket socket) {
        while(true){
            Scanner scanner = new Scanner(System.in);
            sendSignal(socket, scanner.nextLine());
        }
    }


}
