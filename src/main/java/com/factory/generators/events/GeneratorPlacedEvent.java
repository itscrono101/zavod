package com.factory.generators.events;

import com.factory.generators.models.PlacedGenerator;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a player places a generator.
 */
public class GeneratorPlacedEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final PlacedGenerator generator;
    private boolean cancelled;

    public GeneratorPlacedEvent(@NotNull Player player, @NotNull PlacedGenerator generator) {
        this.player = player;
        this.generator = generator;
        this.cancelled = false;
    }

    /**
     * Gets the player who placed the generator.
     *
     * @return Player who placed the generator
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /**
     * Gets the placed generator.
     *
     * @return The PlacedGenerator instance
     */
    @NotNull
    public PlacedGenerator getGenerator() {
        return generator;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
