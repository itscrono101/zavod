package com.factory.generators.managers;

import com.factory.generators.IronFactory;
import com.factory.generators.models.GeneratorEvent;
import com.factory.generators.models.GeneratorAilment;
import com.factory.generators.models.PlacedGenerator;
import com.factory.generators.utils.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Random;

/**
 * Manages random events for generators (diseases, overload, rust, misalignment, earthquakes).
 */
public class EventManager {

    private static final Random RANDOM = new Random();
    private final IronFactory plugin;

    // Event configuration
    private static final int DISEASE_DURATION_TICKS = 6000;  // 5 minutes
    private static final double DISEASE_SPEED_PENALTY = 2.0;  // 2x slower
    private static final int CURE_COST = 5;  // 5 ресурсов на одну единицу времени

    private static final int EARTHQUAKE_COOLDOWN_TICKS = 36000;  // 30 minutes
    private int earthquakeCooldown = 0;
    private static final double EARTHQUAKE_PROBABILITY = 0.001;  // 0.1% каждый тик

    public EventManager(IronFactory plugin) {
        this.plugin = plugin;
    }

    /**
     * Check if generator gets a disease (called every tick).
     */
    public void checkRandomEvents(PlacedGenerator generator, Location location) {
        if (generator.isBroken() || generator.hasAilment()) {
            return;  // Already has issues
        }

        // Probability of getting a disease
        for (GeneratorEvent event : GeneratorEvent.values()) {
            if (RANDOM.nextDouble() < event.getProbability() / 20.0) {  // Convert to per-second
                afflictGenerator(generator, event, location);
                break;
            }
        }
    }

    /**
     * Afflict a generator with a disease.
     */
    public void afflictGenerator(PlacedGenerator generator, GeneratorEvent event, Location location) {
        if (generator.hasAilment()) return;

        GeneratorAilment ailment = new GeneratorAilment(event, DISEASE_DURATION_TICKS, DISEASE_SPEED_PENALTY);
        generator.setAilment(ailment);

        Logger.security("Generator at " + location + " afflicted with: " + event.getDisplayName());

        // Notify owner if online
        Player owner = Bukkit.getPlayer(generator.getOwnerUUID());
        if (owner != null) {
            owner.sendMessage("§c[Завод] Генератор заболел! Болезнь: " + event.getDisplayName());
            owner.playSound(owner.getLocation(), Sound.ENTITY_ZOMBIE_HURT, 1f, 1f);
        }

        // Visual effects
        spawnEventParticles(location, event, 3);
    }

    /**
     * Cure a generator's ailment.
     */
    public boolean cureAilment(PlacedGenerator generator, int resourceAmount) {
        if (!generator.hasAilment()) {
            return false;
        }

        GeneratorAilment ailment = generator.getAilment();
        int healAmount = resourceAmount * CURE_COST;
        ailment.cure(healAmount);

        if (!ailment.isActive()) {
            Logger.info("Generator at " + generator.getLocation() + " cured from: " + ailment.getEventType().getDisplayName());
            generator.cureAilment();
            return true;
        }

        return false;
    }

    /**
     * Apply speed penalty based on ailment.
     */
    public double getAilmentSpeedMultiplier(PlacedGenerator generator) {
        if (!generator.hasAilment()) {
            return 1.0;
        }

        GeneratorAilment ailment = generator.getAilment();
        return ailment.getSpeedPenalty();  // Returns multiplier (e.g., 2.0 = 2x slower)
    }

    /**
     * Tick all active ailments.
     */
    public void tickAilments() {
        var generators = plugin.getGeneratorManager().getPlacedGenerators().values();
        if (generators.isEmpty()) return;  // Early exit if no generators

        for (PlacedGenerator generator : generators) {
            if (!generator.hasAilment()) continue;  // Skip if no ailment

            GeneratorAilment ailment = generator.getAilment();
            ailment.tick();

            // Remove if expired
            if (!ailment.isActive()) {
                Logger.info("Ailment expired for generator at " + generator.getLocation());
                generator.cureAilment();
            }
        }
    }

    /**
     * Check for earthquake event.
     */
    public void checkEarthquake() {
        if (earthquakeCooldown > 0) {
            earthquakeCooldown--;
            return;
        }

        if (RANDOM.nextDouble() < EARTHQUAKE_PROBABILITY) {
            triggerEarthquake();
            earthquakeCooldown = EARTHQUAKE_COOLDOWN_TICKS;
        }
    }

    /**
     * Trigger an earthquake - random generators get destroyed.
     */
    public void triggerEarthquake() {
        var generators = plugin.getGeneratorManager().getPlacedGenerators();
        if (generators.isEmpty()) return;

        // Create a safe copy to avoid ConcurrentModificationException
        var generatorsList = new java.util.ArrayList<>(generators.values());
        int affectedCount = Math.max(1, (int) (generatorsList.size() * 0.1));  // 10% of generators

        Logger.security("EARTHQUAKE TRIGGERED! Affecting " + affectedCount + " generators");
        Bukkit.broadcastMessage("§c§l[ЗЕМЛЕТРЯСЕНИЕ] Генераторы начинают падать!");

        int affected = 0;
        for (PlacedGenerator generator : generatorsList) {
            if (affected >= affectedCount) break;
            if (RANDOM.nextDouble() < 0.5 && !generator.isBroken()) {
                generator.setBroken(true);
                affected++;

                Location loc = generator.getLocation();
                if (loc != null && loc.getWorld() != null) {
                    loc.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, loc, 20, 0.5, 0.5, 0.5, 0.1);
                    loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.5f);
                }

                Player owner = Bukkit.getPlayer(generator.getOwnerUUID());
                if (owner != null && owner.isOnline()) {
                    owner.sendMessage("§c[Землетрясение] Твой генератор упал!");
                }
            }
        }
    }

    /**
     * Spawn visual effects for an event (synchronously to ensure thread safety).
     */
    private void spawnEventParticles(Location location, GeneratorEvent event, int times) {
        if (location == null || location.getWorld() == null) return;

        // Spawn particles immediately (sync only, no sleep)
        for (int i = 0; i < times; i++) {
            location.getWorld().spawnParticle(event.getParticle(), location.clone().add(0, 1, 0),
                    15, 0.3, 0.3, 0.3, 0.05);
        }

        // Play sound once
        location.getWorld().playSound(location, event.getSound(), 0.5f, 1f);
    }
}
