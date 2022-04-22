package dev.efnilite.ip.events;

import dev.efnilite.vilib.event.EventWrapper;
import dev.efnilite.ip.player.ParkourPlayer;

/**
 * This event gets called when a player falls off of the parkour.
 * This event is read-only.
 */
public class PlayerFallEvent extends EventWrapper {

    public final ParkourPlayer player;

    public PlayerFallEvent(ParkourPlayer player) {
        this.player = player;
    }
}