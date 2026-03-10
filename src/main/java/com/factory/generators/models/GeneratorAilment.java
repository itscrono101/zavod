package com.factory.generators.models;

/**
 * Represents a disease/ailment affecting a generator.
 */
public class GeneratorAilment {

    private GeneratorEvent eventType;
    private int ticksRemaining;
    private double speedPenalty;

    public GeneratorAilment(GeneratorEvent eventType, int durationTicks, double speedPenalty) {
        this.eventType = eventType;
        this.ticksRemaining = durationTicks;
        this.speedPenalty = speedPenalty;
    }

    public void tick() {
        if (ticksRemaining > 0) {
            ticksRemaining--;
        }
    }

    public boolean isActive() {
        return ticksRemaining > 0;
    }

    public GeneratorEvent getEventType() {
        return eventType;
    }

    public int getTicksRemaining() {
        return ticksRemaining;
    }

    public double getSpeedPenalty() {
        return speedPenalty;
    }

    public void cure(int healAmount) {
        if (healAmount < 0) return;  // Validate input
        ticksRemaining = Math.max(0, ticksRemaining - healAmount);
    }

    public int getProgressPercent() {
        return 0; // Will be calculated from duration
    }
}
