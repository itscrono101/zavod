package com.factory.generators.events;

import com.factory.generators.models.PlacedGenerator;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a generator breaks due to random chance.
 */
public class GeneratorBrokenEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final PlacedGenerator generator;
    private final String generatorTypeId;

    public GeneratorBrokenEvent(@NotNull PlacedGenerator generator, @NotNull String typeId) {
        this.generator = generator;
        this.generatorTypeId = typeId;
    }

    /**
     * Gets the broken generator.
     *
     * @return The PlacedGenerator instance
     */
    @NotNull
    public PlacedGenerator getGenerator() {
        return generator;
    }

    /**
     * Gets the generator type ID.
     *
     * @return Generator type ID
     */
    @NotNull
    public String getGeneratorTypeId() {
        return generatorTypeId;
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
