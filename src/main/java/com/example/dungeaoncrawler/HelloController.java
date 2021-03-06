package com.example.dungeaoncrawler;

import com.example.dungeaoncrawler.logic.Cell;
import com.example.dungeaoncrawler.logic.CellType;
import com.example.dungeaoncrawler.logic.GameMap;
import com.example.dungeaoncrawler.logic.actors.Actor;
import com.example.dungeaoncrawler.logic.actors.Enemy;
import com.example.dungeaoncrawler.logic.actors.Player;
import com.example.dungeaoncrawler.logic.items.CardRarity;
import com.example.dungeaoncrawler.logic.items.Cards;
import com.example.dungeaoncrawler.logic.items.CardsType;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.io.*;
import java.util.Objects;
import java.util.Random;

import static com.example.dungeaoncrawler.HelloApplication.player;
import static com.example.dungeaoncrawler.HelloApplication.worldMap;

public class HelloController {

    static boolean canMove = true;
    static Enemy opponent;
    static boolean isPlayerAlive = true;
    private final Image tileset = new Image("mapObjects.png", 577 * 2, 577 * 2, true, false);
    Thread independentEnemiesMoves = new Thread(new Runnable() {
        @Override
        public void run() {
            while (true) {
                try {
                    Thread.currentThread().sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                getEnemyMove();
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        printMap();
                    }
                });
            }
        }
    });

    @FXML
    private GridPane baseMap;

    @FXML
    private GridPane gridMap;

    @FXML
    private GridPane actorMap;

    @FXML
    private TableView<Statistics> TableView;

    @FXML
    private TableColumn<Statistics, String> TableStatisticName;

    @FXML
    private TableColumn<Statistics, Integer> TableStatisticPoints;

    @FXML
    private VBox playerCardDeck;

    @FXML
    private TextField deckPlayerName;

    public void closeWindow() {
        System.out.println("close window");
        independentEnemiesMoves.stop();
    }

    public void initialize() {
        deckPlayerName.setText(player.getName() + "'s deck");
        canMove = true;
        printMap();
        printMinimap();
        updateDeck();
        independentEnemiesMoves.setDaemon(true);
        independentEnemiesMoves.start();
        loadStatistics();
    }

    public void printMap() {
        gridMap.getChildren().clear();
        baseMap.getChildren().clear();
        actorMap.getChildren().clear();
        loadStatistics();

        GameMap map = worldMap.getGameMap(worldMap.getCurrentPos()[0], worldMap.getCurrentPos()[1]);
        gridMap.setHgap(0);
        gridMap.setVgap(0);

        baseMap.setHgap(0);
        baseMap.setVgap(0);

        actorMap.setHgap(0);
        actorMap.setVgap(0);

        for (int i = 0; i < gridMap.getColumnCount(); i++) {
            for (int j = 0; j < gridMap.getRowCount(); j++) {

                Cell currCell = map.getCell(i, j);

                int[] cellType = currCell.getCellTypeImageCoords();
                int[] cellDecor = currCell.getCellDecorImageCoords();
                int[] cellActor = currCell.getCellActorImageCoords();

                ImageView imageView = ImageHandler.getTile(tileset, 1, 1);
                imageView.setFitWidth(32);
                imageView.setFitHeight(32);
                baseMap.add(imageView, i, j);

                ImageView setDecor;

                if (cellType != null) {
                    setDecor = ImageHandler.getTile(tileset, cellType[0], cellType[1]);
                    setDecor.setFitWidth(32);
                    setDecor.setFitHeight(32);

                    gridMap.add(setDecor, i, j);
                }
                if (cellDecor != null && currCell.getType() == CellType.EMPTY) {
                    setDecor = ImageHandler.getTile(tileset, cellDecor[0], cellDecor[1]);
                    setDecor.setFitWidth(32);
                    setDecor.setFitHeight(32);

                    gridMap.add(setDecor, i, j);
                }

                if (cellActor != null) {
                    ImageView setActor = ImageHandler.getTile(tileset, cellActor[0], cellActor[1]);
                    setActor.setFitWidth(32);
                    setActor.setFitHeight(32);
                    actorMap.add(setActor, i, j);
                }
            }
        }
    }

    public void printMinimap() {
        System.out.println(worldMap);
    }

    public void onKeyPressed(KeyEvent keyEvent) {
        if (canMove && isPlayerAlive) {
            switch (keyEvent.getCode()) {
                case UP -> {
                    player.move(0, -1);
                    printMap();
                }
                case DOWN -> {
                    player.move(0, 1);
                    printMap();
                }
                case LEFT -> {
                    player.move(-1, 0);
                    printMap();
                }
                case RIGHT -> {
                    player.move(1, 0);
                    printMap();
                }
                case S -> {
                    saveGame();
                }
                case E -> {
                    takeItem();
                    printMap();
                }
                case M -> {
                    AlertBox.displayAlertBox("Mini Map", String.valueOf(worldMap), "minimap.png", 100);
                }
            }
            startFightWithEnemy();
        }
    }

    public void getEnemyMove() {
        if (canMove) {
            int[][] possibleMoves = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};

            for (Actor actor : worldMap.getGameMap(worldMap.getCurrMapX(), worldMap.getCurrMapY()).getEnemyList()) {

                int[] randomCoordinates = possibleMoves[new Random().nextInt(possibleMoves.length)];
                actor.move(randomCoordinates[0], randomCoordinates[1]);
            }
        }
    }

    private void startFightWithEnemy() {
        int x = player.getCell().getX();
        int y = player.getCell().getY();
        int[][] neighbourField = {{x + 1, y}, {x - 1, y}, {x, y + 1}, {x, y - 1}};

        for (int[] i : neighbourField) {
            if (worldMap.getGameMap(worldMap.getCurrMapX(), worldMap.getCurrMapY()).getCell(i[0], i[1]).getActor() != null) {
                canMove = false;
                opponent = (Enemy) worldMap.getGameMap(worldMap.getCurrMapX(), worldMap.getCurrMapY()).getCell(i[0], i[1]).getActor();
                Test test = new Test();
                test.startFight();
            }
        }
    }

    private void takeItem() {
        int x = player.getCell().getX();
        int y = player.getCell().getY();
        int[][] neighbourField = {{x + 1, y}, {x - 1, y}, {x, y + 1}, {x, y - 1}};
        Cards newCard = null;

        for (int[] i : neighbourField) {
            Cell cell = worldMap.getGameMap(worldMap.getCurrMapX(), worldMap.getCurrMapY()).getCell(i[0], i[1]);
            if (Objects.equals(cell.getInteractableStatus(), true)) {
                switch (cell.getTileName()) {
                    case "key" -> player.giveKeys(1);
                    case "health" -> player.setHealth(player.getHealth() + 2);
                    case "power" -> player.setPower(player.getPower() + 2);
                    case "armor" -> player.setArmor(player.getArmor() + 2);
                    case "card" -> newCard = collectCardAndAddToDeck();
                }

                if (Objects.equals(cell.getTileName(), "card")) {
                    getCard(newCard, cell);
                } else if (Objects.equals(cell.getTileName(), "key")) {
                    getKey(cell);
                } else if (Objects.equals(cell.getTileName(), "closedDoor")) {
                    useKey(cell);
                } else if (Objects.equals(cell.getTileName(), "trapdoor")) {
                    moveToEnd();
                } else if (Objects.equals(cell.getTileName(), "dev")) {

                    if (cell.getType() == CellType.KUBA) {
                        kubaMessage();
                    }
                    if (cell.getType() == CellType.BARTEK) {
                        bartekMessage();
                    }
                    if (cell.getType() == CellType.KRZYSIEK) {
                        krzysiekMessage();
                    }

                } else {
                    getItem(cell);
                }
                loadStatistics();
            }
        }
    }

    private void kubaMessage() {
        AlertBox.displayAlertBox("Kuba",
                "Gratulacje przybyszu! Nie s??dzi??em ??e dotrzesz do ko??ca, a jednak \n" +
                        "uda??o Ci si??, jestem pod wra??eniem! Droga do ko??ca mapy by??a naszpikowana\n" +
                        "pu??apkami, ale jak wida?? nie do???? trudna, ??eby przeszkodzi?? Ci w spotkaniu\n" +
                        "ze mn??. Twoje imi?? zostanie zapami??tane na d??ugo a bardowie b??d?? ??piewali\n" +
                        "pie??ni na Twoj?? cze????, jednak na razie nie dowiesz si?? dok??d prowadzi portal,\n" +
                        "kt??ry znajduje si?? za mn??. Nied??ugo Twoja odwaga zostanie poddana\n" +
                        "kolejnej pr??bie, zatem musisz by?? czujny. Nie znasz dnia ani godziny kiedy \n" +
                        "przyjdzie czas aby znowu wyruszy?? w podr???? i zmierzy?? si?? z nieprzyjaznym \n" +
                        "??wiatem pe??nym wrog??w. Mo??e wtedy poznasz tajemnice, kt??ra kryje si?? po \n" +
                        "drugiej stronie portalu. Zatem do zobaczenia... ", "kuba.png", 300);
    }

    private void bartekMessage() {
        AlertBox.displayAlertBox("Bartek",
                "WOW dotar??e??!!! Uczeni wyliczyli, ??e jest tylko jedna szansa na milion,\n" +
                        "by zaistnia??o co?? tak ca??kowicie absurdalnego. Jednak magowie obliczyli,\n" +
                        "??e szanse jedna na milion sprawdzaj?? si?? w dziewi??ciu przypadkach na dziesi????.\n" +
                        "\n" +
                        "Gratulacje!\n" +
                        "\n" +
                        "Co? Schody za mn???!\n" +
                        "Bior?? je skubane na siebie!\n" +
                        "Skopi?? im por??cz tak bardzo, ??e nie b??d?? wiedzia??y kt??r??dy na g??r??!",
                "bartek.png", 300);
    }

    private void krzysiekMessage() {
        String name = "";
        if (Objects.equals(player.getName(), "")) {
            name = "Poszukiwaczem przyg??d";
        } else {
            name = player.getName();
        }
        AlertBox.displayAlertBox("Krzysiek",
                "O, moi ukochani wi????niowie, musz?? wymy??li?? wam na dzi?? jakie??... tortury...\n" +
                        "Oh, kim ty jeste??? " + name + "? Pierwsze s??ysz??, ale skoro tu dotar??e??\n" +
                        "to musisz by?? ca??kiem pot????nym. Czy wiesz, ??e gdy zginiesz w tej krypcie to trafiasz\n" +
                        "do moich loch??w? Uwa??aj na siebie, je??li nie chcesz tam trafi??!",
                "krzysiek.png", 300);
    }

    private void getCard(Cards newCard, Cell cell) {
        AlertBox.displayAlertBox("Collect Item", "Great, you already collect extra card " + newCard.getName() + "!\n" +
                "Card type : " + newCard.getCardsType().name() + "\n" +
                "Card cost : " + newCard.getCardCost() + "\n" +
                "Card rarity : " + newCard.getRarity() + "\n" +
                "Description : " + newCard.getDescription() + "\n" +
                "Value : " + newCard.getValue() + "\n", newCard.getImg(), 80);
        cell.setType(CellType.EMPTY);
    }

    private void getKey(Cell cell) {
        AlertBox.displayAlertBox("Key found!", "Hey, you've found a key! Now you can go to the final boss!", "keyBig.png", 80);
        cell.setType(CellType.EMPTY);
    }

    private void useKey(Cell cell) {
        int keyCost = 1;
        if (player.getKeyCount() >= keyCost) {
            player.useKeys(keyCost);
            cell.setType(CellType.OPEN_DOOR);
        }
    }

    private void getItem(Cell cell) {
        AlertBox.displayAlertBox("Collect Item", "Great, you already collect extra + 2 to " +
                cell.getTileName() + "!", cell.getTileName() + ".png", 80);
        cell.setType(CellType.EMPTY);
    }

    private void moveToEnd() {
        player.moveToEnd();
    }

    public Cards collectCardAndAddToDeck() {
        CardRarity rarity = Player.drawRarity();
        CardsType cardsType = CardsType.getRandomeType();
        Cards card = new Cards(cardsType.getFile(), cardsType.getName(), null, cardsType, rarity);
        player.addCardToDeck(card);
        updateDeck();
        return card;
    }

    public void updateDeck() {
        playerCardDeck.getChildren().clear();
        for (Cards card : player.getDeck()) {
            try {
                playerCardDeck.getChildren().add(getCardPane(card));
                playerCardDeck.setAlignment(Pos.CENTER);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Pane getCardPane(Cards card) throws IOException {
        Pane cardPane = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("Card.fxml")));
        ImageView cardImage = (ImageView) cardPane.getChildren().get(0);
        ImageView cardBg = (ImageView) cardPane.getChildren().get(1);
        Label description = (Label) cardPane.getChildren().get(2);
        Label cardCost = (Label) cardPane.getChildren().get(3);

        cardImage.setImage(new Image(card.getImg()));
        description.setText(card.getDescription());
        cardCost.setText(String.valueOf(card.getCardCost()));

        return cardPane;
    }

    private void saveGame() {
        try {
            File saveFile = new File("SAVE.sav");
            FileOutputStream saveStream = new FileOutputStream(saveFile);
            ObjectOutputStream saveData = new ObjectOutputStream(saveStream);

            saveData.writeObject(worldMap);
            saveData.writeObject(player);

            saveData.close();
            saveStream.close();
            System.out.println(saveFile.getAbsolutePath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.out.println(": Error while saving: file not found");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadStatistics() {
        ObservableList<Statistics> playerStatistics = createPlayerStatistics();
        TableView.setItems(playerStatistics);
        TableStatisticName.setCellValueFactory(cellData -> cellData.getValue().getStatisticsName());
        TableStatisticPoints.setCellValueFactory(cellData -> cellData.getValue().getStatisticsPoints().asObject());
    }

    public ObservableList<Statistics> createPlayerStatistics() {
        ObservableList<Statistics> playerStatistics = FXCollections.observableArrayList(
                new Statistics("Health", player.getHealth()),
                new Statistics("Resistance", player.getResistance()),
                new Statistics("Armor", player.getArmor()),
                new Statistics("Power", player.getPower()),
                new Statistics("Exp", player.getExp()),
                new Statistics("Keys", player.getKeyCount()));
        return playerStatistics;
    }

    @FXML
    void setFocus(MouseEvent e) {
        gridMap.requestFocus();
    }
}
