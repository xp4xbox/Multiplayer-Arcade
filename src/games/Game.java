package games;

import games.player.NetworkPlayer;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;

import java.util.function.Consumer;

/**
 * Enforces structure for games of all sort.
 *
 * @author Kyle Anderson
 * ICS4U RST
 */
public interface Game {
    /**
     * Begins a new instance of this game.
     */
    void start();

    /**
     * Ends the game.
     */
    void end();

    /**
     * Gets the cover image for the
     *
     * @return Gets the cover art for this game.
     */
    Image getCoverArt();

    /**
     * Gets the name of the game.
     *
     * @return The game's nice english name.
     */
    String getName();

    /**
     * Gets the text object to be displayed for this game on its menu.
     *
     * @return The text object.
     */
    Text getTextDisplay();

    /**
     * Determines if this game is a network game.
     *
     * @return True if it's a network game, false otherwise.
     */
    boolean isNetworkGame();

    /**
     * Gets the displaying window for the game.
     *
     * @return The display window for the game.
     */
    Region getWindow();

    /**
     * Resets the game, readying it for another play instance.
     */
    void reset();

    /**
     * Gets the network player playing this game.
     *
     * @return The network player in this game. Null if there isn't one.
     */
    NetworkPlayer getNetworkPlayer();

    /**
     * Tells this game to be a network game, therefore making it skip any game type selection process.
     */
    void setNetworkGame();

    /**
     * Sets up players before starting the game.
     */
    void initializePlayers();

    /**
     * Sets a listener to be notified when the game ends.
     *
     * @param endListener The end game listener.
     */
    void setOnEnd(Consumer<Game> endListener);

    /**
     * Gets help text for the game.
     *
     * @return The help string for the game.
     */
    String getHelpText();
}
