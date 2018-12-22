package games.pong.players;

import games.player.Player;
import games.pong.pieces.Paddle;
import games.pong.pieces.Side;

import java.util.function.Consumer;

/**
 * @author Kyle Anderson
 * ICS4U RST
 */
public interface PongPlayer extends Player {
    /**
     * Sets an action to be called when the player attempts to move his/her paddle up.\
     *
     * @param action The action to be called when the event occurs.
     */
    void setOnPaddleUp(Consumer<PongPlayer> action);

    /**
     * Sets an action to be called when the player attempts to move his/her paddle down.
     *
     * @param action The action to be called when the event occurs.
     */
    void setOnPaddleDown(Consumer<PongPlayer> action);

    /**
     * Sets an action to be called when the player attempts to pause the game.
     *
     * @param action The action function to be called when the event occurs.
     */
    void setOnPause(Consumer<PongPlayer> action);

    /**
     * Gets the side on which this pong player is.
     *
     * @return {@link games.pong.Pong#LEFT_SIDE} for the left side, {@link games.pong.Pong#RIGHT_SIDE} for the right side.
     */
    Side getSide();

    /**
     * Sets the start side for this pong player.
     *
     * @param side The start side. {@link games.pong.Pong#LEFT_SIDE} for left, {@link games.pong.Pong#RIGHT_SIDE} for right.
     */
    void setSide(Side side);

    /**
     * Adds a point to this player's score.
     */
    void addPoint();

    /**
     * Gets this player's number of points.
     * @return The number of points for this player.
     */
    int getPoints();

    /**
     * Gets this player's paddle.
     * @return The player's paddle.
     */
    Paddle getPaddle();

    /**
     * Sets this player's paddle.
     */
    void setPaddle(Paddle paddle);
}
