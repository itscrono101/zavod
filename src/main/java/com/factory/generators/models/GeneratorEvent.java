package com.factory.generators.models;

import org.bukkit.Particle;
import org.bukkit.Sound;

/**
 * Represents random events that can happen to generators.
 */
public enum GeneratorEvent {

    DISEASE("болезнь", "Генератор заболел", 0.008) {
        @Override
        public Particle getParticle() {
            return Particle.SMOKE_LARGE;
        }

        @Override
        public Sound getSound() {
            return Sound.ENTITY_ZOMBIE_HURT;
        }
    },

    OVERLOAD("перегруз", "Генератор перегрелся", 0.004) {
        @Override
        public Particle getParticle() {
            return Particle.FLAME;
        }

        @Override
        public Sound getSound() {
            return Sound.BLOCK_FIRE_EXTINGUISH;
        }
    },

    RUST("коррозия", "Генератор заржавел", 0.006) {
        @Override
        public Particle getParticle() {
            return Particle.DAMAGE_INDICATOR;
        }

        @Override
        public Sound getSound() {
            return Sound.BLOCK_METAL_BREAK;
        }
    },

    MISALIGNMENT("разрегулировка", "Генератор разрегулирован", 0.01) {
        @Override
        public Particle getParticle() {
            return Particle.CRIT;
        }

        @Override
        public Sound getSound() {
            return Sound.ENTITY_ITEM_BREAK;
        }
    };

    private final String id;
    private final String displayName;
    private final double probability;

    GeneratorEvent(String id, String displayName, double probability) {
        this.id = id;
        this.displayName = displayName;
        this.probability = probability;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getProbability() {
        return probability;
    }

    public abstract Particle getParticle();
    public abstract Sound getSound();
}
