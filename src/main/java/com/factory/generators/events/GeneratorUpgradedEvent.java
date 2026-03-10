package com.factory.generators.events;

import com.factory.generators.models.PlacedGenerator;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a generator is upgraded to the next level.
 */
public class GeneratorUpgradedEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final PlacedGenerator generator;
    private final String fromTypeId;
    private final String toTypeId;
    private boolean cancelled;

    public GeneratorUpgradedEvent(@NotNull Player player, @NotNull PlacedGenerator generator,
                                  @NotNull String fromTypeId, @NotNull String toTypeId) {
        this.player = player;
        this.generator = generator;
        this.fromTypeId = fromTypeId;
        this.toTypeId = toTypeId;
        this.cancelled = false;
    }

    /**
     * Gets the player who upgraded the generator.
     *
     * @return Player who upgraded
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /**
     * Gets the upgraded generator.
     *
     * @return The PlacedGenerator instance
     */
    @NotNull
    public PlacedGenerator getGenerator() {
        return generator;
    }

    /**
     * Gets the generator type before upgrade.
     *
     * @return Previous type ID
     */
    @NotNull
    public String getFromTypeId() {
        return fromTypeId;
    }

    /**
     * Gets the generator type after upgrade.
     *
     * @return New type ID
     */
    @NotNull
    public String getToTypeId() {
        return toTypeId;
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
