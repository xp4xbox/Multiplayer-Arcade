package games.pong.ui;

import games.Game;
import games.player.PongKeyBinding;
import games.pong.EndReason;
import games.pong.Pong;
import games.pong.PongEvent;
import games.pong.pieces.Paddle;
import games.pong.pieces.PongPiece;
import games.pong.pieces.Side;
import games.pong.players.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;
import menu.MainMenu;
import network.party.PartyHandler;
import network.party.PartyRole;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * UI class for the pong game, for actually rendering the game for the user.
 *
 * @author Kyle Anderson
 * ICS4U RST
 */
public class PongUI extends Pane implements Game {

    private static final double
            FPS = 60; // Frames per second

    // Load custom blocky font
    static {
        InputStream stream = PongUI.class.getResourceAsStream("/res/pong/fonts/pong.ttf");
        Font.loadFont(stream, 10);
        try {
            stream.close();
        } catch (IOException e) {
            // Output error.
            System.err.println(String.format("Failed to close font loading stream.\n%s", Arrays.toString(e.getStackTrace())));
        }
    }


    // Font used around the UI.
    private static final Font FONT = Font.font("Bit5x3", FontWeight.BOLD, FontPosture.REGULAR, 80);
    private static final Paint BACKGROUND_COLOUR = Color.BLACK, FOREGROUND_COLOUR = Color.WHITE;
    /**
     * Key used to end the game.
     */
    private static final KeyCode END_GAME_KEYCODE = KeyCode.BACK_SPACE;
    private static final String HELP_TEXT = String.format("Press the up and down arrows to move your player (or the q and a keys for local multiplayer)\n" +
            "End the game by pressing the %s key repeatedly.", END_GAME_KEYCODE.getName());
    /**
     * The time period in which the player must hit the end key to end the game.
     */
    private static final long END_KEY_MILLISECONDS = 3000;
    private static final int END_KEY_NUMBER_PRESSES = 3;
    private Pong game;
    // How much the units in the pong game backend are scaled to make a nice looking UI.
    private double scaleFactor;

    private Rectangle ball;
    private Divider divider;

    private boolean hasInitializedPlayers;

    private final Rectangle leftPaddle;
    private final Rectangle rightPaddle;
    private final Scoreboard scoreboard;

    // Set up key bindings list.
    private ArrayList<HashMap<KeyCode, PongKeyBinding>> keyBindings;
    // Timers to be used when rendering the game to the user.
    private Timeline renderFrameTimer;

    // Used for keeping track of the keys that are being pressed down so we don't repeat calls.
    private final ArrayList<KeyCode> keysDown = new ArrayList<>();

    // List of the keyboard players in this game.
    private final ArrayList<PongKeyboardPlayer> keyboardPlayerList = new ArrayList<>();
    private final ArrayList<KeyCode> keyCodesWeCareAbout = new ArrayList<>();
    /**
     * Listener for when the game ends.
     */
    private Consumer<Game> endGameListener;

    /*
    We want to make it so that if the player presses the end key END_KEY_NUMBER_PRESSES times in END_KEY_MILLISECONDS
    milliseconds, the game quits.
     */
    private final Long[] endKeyPressTimes = new Long[END_KEY_NUMBER_PRESSES];
    private VBox selector;

    /**
     * Constructs a new PongUI with the given width and height and Game object.
     */
    public PongUI() {
        // Set the background to the proper background colour.
        setBackground(new Background(new BackgroundFill(BACKGROUND_COLOUR, CornerRadii.EMPTY, Insets.EMPTY)));

        // init paddles, ball and scoreboard
        leftPaddle = new Rectangle();
        leftPaddle.setFill(FOREGROUND_COLOUR);
        rightPaddle = new Rectangle();
        rightPaddle.setFill(FOREGROUND_COLOUR);

        divider = new Divider(FOREGROUND_COLOUR);

        // set to background color and font for scoreboard, and hide it
        scoreboard = new Scoreboard(FOREGROUND_COLOUR, FONT);
        scoreboard.setVisible(false);

        ball = new Rectangle();
        ball.setFill(FOREGROUND_COLOUR);

        getChildren().addAll(divider, leftPaddle, rightPaddle, ball, scoreboard);

        setOnKeyPressed(this::keyPressed);
        setOnKeyReleased(this::keyReleased);

        // Add a listener on the scene so we know when it's ready.
        sceneProperty().addListener((observable, oldValue, newValue) -> sceneChanged(newValue));

        // Reset and set up game.
        reset();
    }

    /**
     * Called when the scene is changed.
     *
     * @param newScene The new scene
     */
    private void sceneChanged(Scene newScene) {
        if (newScene != null) {
            // Add listeners to resize when the user resizes the screen.
            newScene.widthProperty().addListener((observable, oldValue, newValue) -> {
                if (!oldValue.equals(newValue)) {
                    recalculateScreenDimensions();
                }
            });
            newScene.heightProperty().addListener((observable, oldValue, newValue) -> {
                if (!oldValue.equals(newValue)) {
                    recalculateScreenDimensions();
                }
            });
            recalculateScreenDimensions();
        }
    }

    /**
     * Calculates the proper height and width for screen based off of the ratio set in the game.
     */
    private void recalculateScreenDimensions() {
        final Scene scene = getScene();
        if (scene != null) {
            final double sceneHeight = scene.getHeight(), sceneWidth = scene.getWidth();

            // If the height/width ratio of the screen is larger than the one on the board, the width is limiting.
            if (sceneHeight / sceneWidth > game.getBoardHeight() / game.getBoardWidth()) {
                setHeight(game.getBoardHeight() / game.getBoardWidth() * sceneWidth);
                setWidth(sceneWidth);
            }
            // Height is limiting.
            else {
                setWidth(game.getBoardWidth() / game.getBoardHeight() * sceneHeight);
                setHeight(sceneHeight);
            }

            calculateScaleFactor();
        }
    }

    /**
     * Called when a key is pressed.
     *
     * @param event The keydown event.
     */
    private void keyPressed(KeyEvent event) {
        KeyCode keyDown = event.getCode();
        if (keyDown.equals(END_GAME_KEYCODE)) {
            endGameKeyPressed();
        } else if (!keysDown.contains(keyDown)) {
            keysDown.add(keyDown);
            updatePlayerKeys();
        }
    }

    /**
     * Called when a key is released.
     *
     * @param event The keyup event.
     */
    private void keyReleased(KeyEvent event) {
        KeyCode keyDown = event.getCode();
        keysDown.remove(keyDown);
        updatePlayerKeys();
    }

    /**
     * Updates the players on which keys are being pressed down.
     */
    private void updatePlayerKeys() {
        List<KeyCode> goodKeys = keysDown.stream().filter(keyCodesWeCareAbout::contains).collect(Collectors.toList());
        if (game.getLocalPlayer() instanceof PongKeyboardPlayer) {
            ((PongKeyboardPlayer) game.getLocalPlayer()).setKeysDown(goodKeys);
        }
        if (game.getPlayer2() instanceof PongKeyboardPlayer) {
            ((PongKeyboardPlayer) game.getPlayer2()).setKeysDown(goodKeys);
        }
    }

    /**
     * Called when the end key is pressed.
     */
    private void endGameKeyPressed() {
        shiftAndAppend(endKeyPressTimes, System.currentTimeMillis());

        // If the second element is null, the player needs some encouragement.
        if (endKeyPressTimes[endKeyPressTimes.length - 2] == null) {
            showNotification(Alert.AlertType.INFORMATION, String.format("Continue pressing %s to quit the game.", END_GAME_KEYCODE.getName()));
        }

        checkEnd();
    }

    /**
     * Checks to see if the player wishes to end the game.
     */
    private void checkEnd() {
        if ((endKeyPressTimes[endKeyPressTimes.length - 1] != null)
                && (endKeyPressTimes[0] != null)
                && ((endKeyPressTimes[endKeyPressTimes.length - 1] - endKeyPressTimes[0]) <= END_KEY_MILLISECONDS)) {
            game.end(EndReason.PLAYER_END);
        }
    }

    /**
     * Re-calculates the scale factor for rendering and such.
     */
    private void calculateScaleFactor() {
        scaleFactor = getWorkingWidth() / game.getBoardWidth();

        leftPaddle.setWidth(game.getLeftPaddle().getWidth() * scaleFactor);
        leftPaddle.setHeight(game.getLeftPaddle().getHeight() * scaleFactor);
        rightPaddle.setWidth(game.getRightPaddle().getWidth() * scaleFactor);
        rightPaddle.setHeight(game.getRightPaddle().getHeight() * scaleFactor);
        ball.setWidth(game.getBall().getWidth() * scaleFactor);
        ball.setHeight(game.getBall().getHeight() * scaleFactor);
        scoreboard.changeSize(scaleFactor);
        divider.calculate(getWorkingWidth(), getWorkingHeight());
        updatePaddleLocations();
        updateBallLocation();
    }

    /**
     * Begins the game of pong.
     */
    @Override
    public void start() {
        if (hasInitializedPlayers) {
            MainMenu.getCurrentInstance().sizeToScene();
            recalculateScreenDimensions();
            requestFocus();
            scoreboard.calculate(getWorkingWidth(), getWorkingHeight());
            showScoreboard();

            if (renderFrameTimer == null) {
                renderFrameTimer = new Timeline(new KeyFrame(Duration.millis(1000.0 / FPS), event -> renderFrame()));
                renderFrameTimer.setCycleCount(Timeline.INDEFINITE);

                // Start all timelines.
                renderFrameTimer.play();
            }

            // If this isn't a network game, we can start the game right away. Otherwise, it's up to the PongNetworkPlayer.
            if (!isNetworkGame()) {
                game.begin();
            }
        }
    }

    /**
     * Gets the workable area of width for this game.
     *
     * @return The workable size of width.
     */
    private double getWorkingWidth() {
        return getWidth();
    }

    /**
     * Gets the workable area of height for this game.
     *
     * @return The workable size of height.
     */
    private double getWorkingHeight() {
        return getHeight();
    }

    /**
     * Renders a new frame on screen.
     */
    private void renderFrame() {
        updateBallLocation();
        updatePaddleLocations();
        updateScoreboard();
        game.renderTick();
    }

    /**
     * Updates the scoreboard display with the correct scores.
     */
    private void updateScoreboard() {
        scoreboard.setLeftScore(game.getLeftPlayer().getPoints());
        scoreboard.setRightScore(game.getRightPlayer().getPoints());
        scoreboard.calculate(getWorkingWidth(), getWorkingHeight());
    }

    /**
     * Updates the on-screen locations of the paddles.
     */
    private void updatePaddleLocations() {
        leftPaddle.setX(game.getLeftPaddle().getX() * scaleFactor);
        leftPaddle.setY(transformY(game.getBoardHeight(), game.getLeftPaddle(), Side.TOP) * scaleFactor);
        rightPaddle.setX(game.getRightPaddle().getX() * scaleFactor);
        rightPaddle.setY(transformY(game.getBoardHeight(), game.getRightPaddle(), Side.TOP) * scaleFactor);
    }

    /**
     * Updates hte on-screen location of the pong ball.
     */
    private void updateBallLocation() {
        ball.setX(game.getBall().getX(Side.LEFT) * scaleFactor);
        ball.setY(transformY(game.getBoardHeight(), game.getBall(), Side.TOP) * scaleFactor);
    }

    /**
     * Translates a y value from being measured from the bottom to the top to top to bottom.
     *
     * @param boardHeight The height of the pong board.
     * @param piece       The piece whose coordinates shall be transformed.
     * @param desiredSide The desired coordinate (Side.TOP, Side.CENTER, Side.BOTTOM) that needs to be calculated.
     * @return The transformed value.
     */
    private static double transformY(double boardHeight, PongPiece piece, Side desiredSide) {
        double newY;
        double topLeftY = boardHeight - piece.getY(Side.TOP);
        switch (desiredSide) {
            case TOP:
                newY = topLeftY;
                break;
            case BOTTOM:
                newY = topLeftY + piece.getHeight();
                break;
            case CENTER:
                newY = topLeftY + piece.getHeight() / 2;
                break;
            default:
                throw new IllegalArgumentException("Invalid selection for position.");
        }

        return newY;
    }

    @Override
    public void end() {
        if (renderFrameTimer != null) {
            renderFrameTimer.stop();
            renderFrameTimer = null;
        }

        // If the game ended because of player disconnect, notify the user.
        switch (game.getEndReason()) {
            case PLAYER_DISCONNECT:
                showNotification(Alert.AlertType.ERROR, "Game ended because player disconnected.");
                break;
            case PLAYER_END:
                showNotification(Alert.AlertType.INFORMATION, "Game was ended by a player.");
                break;
            case SCORE_LIMIT_REACHED:
                showNotification(Alert.AlertType.INFORMATION, String.format("%s won the game!", game.getWinner().getName()));
                break;
            default:
                showNotification(Alert.AlertType.ERROR, "Game ended for some unknown reason.");
                break;
        }

        endGameListener.accept(this);
    }

    @Override
    public Image getCoverArt() {
        //noinspection SpellCheckingInspection
        return new Image(getClass().getResource("/res/pong/images/coverart.png").toString());
    }

    @Override
    public String getName() {
        return "Pong";
    }

    @Override
    public Text getTextDisplay() {
        Text text = new Text(getName());
        text.setFont(Font.font("Bit5x3", FontWeight.BLACK, FontPosture.REGULAR, 72));
        text.setFill(Color.ORANGE);

        return text;
    }

    /**
     * Determines if this game is a network game (if one of the players is playing with this one online).
     *
     * @return True if this is a network game, false otherwise.
     */
    @Override
    public boolean isNetworkGame() {
        return game.getPlayer2() instanceof PongNetworkPlayer;
    }

    @Override
    public PongUI getWindow() {
        return this;
    }

    @Override
    public void reset() {
        hasInitializedPlayers = false;
        getChildren().remove(selector); // Make sure the selector isn't on screen anymore
        game = new Pong(); // Initialize new pong game with the correct type of players
        resetKeyBindings();
        game.addEventListener(this::gameEventHappened);
        SfxPongPlayer.init();
    }

    /**
     * Called when the game has an event that occurs.
     *
     * @param event The event that occurred.
     */
    private void gameEventHappened(PongEvent event) {
        // Switch on the event type.
        PongEvent.EventType type = event.getType();

        if (type == PongEvent.EventType.PLAYER_SCORED) {
            SfxPongPlayer.playScored();
        } else if (type == PongEvent.EventType.BALL_HIT_PADDLE) {
            SfxPongPlayer.playHitPaddle();
        } else if (type == PongEvent.EventType.BALL_HIT_BOTTOM_WALL || type == PongEvent.EventType.BALL_HIT_TOP_WALL) {
            SfxPongPlayer.playHitWall();
        } else if (type == PongEvent.EventType.GAME_ENDED) {
            // We want this to run after everything else, so set this to Platform.runLater
            Platform.runLater(this::end);
        }
    }

    /**
     * Resets the key bindings.
     */
    private void resetKeyBindings() {
        keyBindings = new ArrayList<>();

        HashMap<KeyCode, PongKeyBinding> p1Bindings = new HashMap<>();
        p1Bindings.put(KeyCode.UP, PongKeyBinding.MOVE_UP);
        p1Bindings.put(KeyCode.DOWN, PongKeyBinding.MOVE_DOWN);

        HashMap<KeyCode, PongKeyBinding> p2Bindings = new HashMap<>();
        p2Bindings.put(KeyCode.Q, PongKeyBinding.MOVE_UP);
        p2Bindings.put(KeyCode.A, PongKeyBinding.MOVE_DOWN);

        keyBindings.add(p1Bindings);
        keyBindings.add(p2Bindings);
    }

    /**
     * Called when the provided player's action has changed (i.e they should not be descending, ascending or not moving).
     *
     * @param affectedPlayer The player whose action has changed.
     * @param newAction      The new action to be performed.
     */
    private void paddleActionChanged(PongPlayer affectedPlayer, Action newAction) {
        Paddle paddle = game.getPaddle(affectedPlayer);

        switch (newAction) {
            case MOVE_DOWN:
                game.paddleDown(paddle);
                break;
            case MOVE_UP:
                game.paddleUp(paddle);
                break;
            default:
                game.stopPaddle(paddle);
                break;
        }
    }

    /**
     * Initializes the players in the game, if not already done.
     */
    @Override
    public void initializePlayers() {
        initializePlayers(false);
    }

    /**
     * Initializes players
     *
     * @param hasPrompted True if the UI has prompted the user for the game mode already, false otherwise.
     *                    If it's a network game, it will never prompt for players so this doesn't matter.
     */
    private void initializePlayers(boolean hasPrompted) {
        if (!hasPrompted && !isNetworkGame()) {
            promptForPlayers();
        } else {
            PongPlayer p1 = game.getLocalPlayer(), p2 = game.getPlayer2();
            // We can override which sides everybody is on if it's not a network game.
            boolean overrideSides = !isNetworkGame();

            if (p1 == null) {
                p1 = new PongKeyboardPlayer();
                game.setLocalPlayer(p1);
            }
            if (p2 == null) {
                p2 = new PongKeyboardPlayer();
                game.setPlayer2(p2);
            }
            if (p1 instanceof PongKeyboardPlayer) {
                setupBindings((PongKeyboardPlayer) p1);
                keyboardPlayerList.add((PongKeyboardPlayer) p1);
            }
            if (p2 instanceof PongKeyboardPlayer) {
                setupBindings((PongKeyboardPlayer) p2);
                keyboardPlayerList.add((PongKeyboardPlayer) p2);
            }
            if (overrideSides) {
                p1.setSide(Side.RIGHT);
                p2.setSide(Side.LEFT);
            }

            setupKeyCodes();

            game.initialize(); // Initialize pong game now that players are set up.

            p1.setOnActionChanged(this::paddleActionChanged);
            p2.setOnActionChanged(this::paddleActionChanged);
            hasInitializedPlayers = true;

            if (!isNetworkGame()) {
                start();
            }
        }
    }

    /**
     * Prompts the user, asking them what type of game they would like: local singleplayer, local multiplayer, etc.
     */
    private void promptForPlayers() {
        Text text = new Text("Select Game");
        text.setFont(FONT);

        selector = new VBox(15);
        selector.prefWidthProperty().bind(widthProperty());
        selector.prefHeightProperty().bind(heightProperty());
        selector.setAlignment(Pos.CENTER);
        selector.setBackground(new Background(new BackgroundFill(Color.ORANGE, CornerRadii.EMPTY, Insets.EMPTY)));

        Button localMultiplayer = new Button("Local Multiplayer");
        localMultiplayer.setOnAction(event -> playerPromptClosed(new PongKeyboardPlayer(), new PongKeyboardPlayer()));
        Button advancedBot = new Button("Advanced Bot");
        advancedBot.setOnAction(event -> playerPromptClosed(new PongKeyboardPlayer(), new PongAdvancedBot()));
        Button spectateBots = new Button("Spectate Bots");
        spectateBots.setOnAction(event -> playerPromptClosed(new PongAdvancedBot(), new PongAdvancedBot()));
        Button simpleBot = new Button("Easy Bot");
        simpleBot.setOnAction(event -> playerPromptClosed(new PongKeyboardPlayer(), new PongBeginnerBot()));

        selector.getChildren().addAll(text, localMultiplayer, advancedBot, simpleBot, spectateBots);
        getWindow().getChildren().add(selector);
    }

    /**
     * Called after prompting the player to choose how the game will be played.
     *
     * @param localPlayer The local player to be used.
     * @param player2     The second player.
     */
    private void playerPromptClosed(PongPlayer localPlayer, PongPlayer player2) {
        game.setLocalPlayer(localPlayer);
        game.setPlayer2(player2);
        getWindow().getChildren().remove(selector);
        initializePlayers(true);
    }

    /**
     * Show scoreboard
     */
    private void showScoreboard() {
        scoreboard.setVisible(true);
    }

    /**
     * Sets up the key code list.
     */
    private void setupKeyCodes() {
        for (PongKeyboardPlayer player : keyboardPlayerList) {
            keyCodesWeCareAbout.addAll(player.getKeyBindings().keySet());
        }
    }

    @Override
    public void setOnEnd(Consumer<Game> endListener) {
        this.endGameListener = endListener;
    }

    @Override
    public String getHelpText() {
        return HELP_TEXT;
    }

    /**
     * Sets up key bindings for the given player.
     *
     * @param player The player to set up.
     */
    private void setupBindings(PongKeyboardPlayer player) {
        HashMap<KeyCode, PongKeyBinding> bindings = keyBindings.get(0);
        keyBindings.remove(0);
        player.setKeyBindings(bindings);
    }

    @Override
    public void setNetworkGame() {
        PongKeyboardPlayer p1 = new PongKeyboardPlayer();
        PongNetworkPlayer p2 = new PongNetworkPlayer();

        game.setLocalPlayer(p1);
        game.setPlayer2(p2);

        // Set the side depending on who's hosting.
        if (PartyHandler.getRole() == PartyRole.SERVER) {
            game.getLocalPlayer().setSide(Side.RIGHT);
        } else {
            game.getLocalPlayer().setSide(Side.LEFT);
        }
    }

    /**
     * Gets the network player playing this game.
     *
     * @return The PongNetworkPlayer in this game, or null if there isn't one.
     */
    @Override
    public PongNetworkPlayer getNetworkPlayer() {
        PongNetworkPlayer player = null;
        if (isNetworkGame()) {
            player = (PongNetworkPlayer) game.getPlayer2();
        }
        return player;
    }

    /**
     * Shifts the elements in {@code elementsToAppend} into {@code array}.
     * Basically, this maintains the size of the array while appending the elements,
     * thereby deleting some of the first couple of items in the array.
     *
     * @param array            The array for the elements to be shifted (appended) to.
     * @param elementsToAppend The elements to append.
     * @param <T>              The type of the elements in the array.
     */
    @SafeVarargs
    private static <T> void shiftAndAppend(T[] array, T... elementsToAppend) {
        if (elementsToAppend.length >= array.length) {
            for (int i = 0; i < array.length; i++) {
                array[i] = elementsToAppend[elementsToAppend.length - i];
            }
        } else {
            // First shift.
            if (array.length - elementsToAppend.length >= 0)
                System.arraycopy(array, elementsToAppend.length, array, 0, array.length - elementsToAppend.length);
            // Then append.
            for (int i = 0; i < elementsToAppend.length; i++) {
                array[array.length - i - 1] = elementsToAppend[i];
            }
        }
    }


    /**
     * Shows a notification.
     *
     * @param contentText The text to be displayed on the notification.
     */
    private void showNotification(Alert.AlertType type, final String contentText) {
        MainMenu.showNotification(type, contentText);
    }
}
