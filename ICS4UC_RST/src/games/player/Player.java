package games.player;

import games.Game;

/**
 * Defines methods common to players of all games.
 *
 * @author Kyle Anderson
 * ICS4U RST
 */
public interface Player {
    /**
     * Called by the game in which the player is playing to notify this player that something about the
     * game has changed and may need the attention of the player.
     *
     * @param game The game object for the game which was updated.
     */
    void gameUpdated(Game game);

    /**
     * Gets the name designation for this player.
     *
     * @return The name designation of the player.
     */
    String getName();
}