package com.factory.generators.utils;

/**
 * Central constants for the IronFactory plugin.
 * Contains all magic numbers and commonly used values.
 */
public final class Constants {

    // Prevent instantiation
    private Constants() {
        throw new AssertionError("Cannot instantiate Constants class");
    }

    // Timing constants (in ticks, 20 ticks = 1 second)
    public static final class Timing {
        public static final int TICKS_PER_SECOND = 20;
        public static final int AUTO_SAVE_INTERVAL = 20 * 60 * 5; // 5 minutes
        public static final int GENERATOR_TICK_INTERVAL = 20; // 1 second
    }

    // Location offsets
    public static final class Offsets {
        public static final double DROP_OFFSET_X = 0.5;
        public static final double DROP_OFFSET_Y = 1.2;
        public static final double DROP_OFFSET_Z = 0.5;
        public static final double HOLOGRAM_DEFAULT_HEIGHT = 1.5;
    }

    // Particle effects
    public static final class Particles {
        public static final int GENERATION_PARTICLE_COUNT = 10;
        public static final int BREAK_PARTICLE_COUNT = 20;
        public static final double PARTICLE_OFFSET = 0.3;
        public static final double PARTICLE_SPEED = 0.01;
        public static final double BREAK_PARTICLE_OFFSET = 0.3;
        public static final double BREAK_PARTICLE_SPEED = 0.05;
    }

    // Sound volumes and pitches
    public static final class Sound {
        public static final float DEFAULT_VOLUME = 1.0f;
        public static final float DEFAULT_PITCH = 1.0f;
        public static final float GENERATION_VOLUME = 0.5f;
        public static final float GENERATION_PITCH = 1.5f;
        public static final float BREAK_PITCH = 0.5f;
    }

    // Title display times (in ticks)
    public static final class Title {
        public static final int FADE_IN = 10;
        public static final int STAY = 40;
        public static final int FADE_OUT = 10;
    }

    // Default configuration values
    public static final class Defaults {
        public static final int MAX_GENERATORS_PER_PLAYER = 50;
        public static final int DEFAULT_GENERATOR_DELAY = 3600; // 3 minutes
        public static final int DEFAULT_NEARBY_RADIUS = 5;
        public static final double DEFAULT_BREAK_CHANCE = 5.0;
        public static final String DEFAULT_MATERIAL = "FURNACE";
        public static final int DEFAULT_REPAIR_AMOUNT = 5;
    }

    // Chunk calculations
    public static final class Chunk {
        public static final int CHUNK_SIZE_BITS = 4; // 2^4 = 16
    }

    // Permission nodes
    public static final class Permissions {
        public static final String BYPASS = "factory.bypass";
        public static final String GIVE = "factory.give";
        public static final String RELOAD = "factory.reload";
        public static final String ADMIN = "factory.admin";
    }

    // Message prefixes
    public static final class Messages {
        public static final String PREFIX = "&6[Завод]&r ";
        public static final String ERROR_PREFIX = "&c[Завод]&r ";
        public static final String SUCCESS_PREFIX = "&a[Завод]&r ";
        public static final String INFO_PREFIX = "&e[Завод]&r ";
    }

    // File names
    public static final class Files {
        public static final String GENERATORS_FOLDER = "generators";
        public static final String MESSAGES_FILE = "messages.yml";
        public static final String DATA_FILE = "data.yml";
        public static final String MULTIBLOCK_FILE = "multiblock.yml";
    }
}
