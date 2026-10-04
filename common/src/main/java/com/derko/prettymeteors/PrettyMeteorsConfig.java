package com.derko.prettymeteors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

public final class PrettyMeteorsConfig {
    static final String FILE_NAME = "pretty-meteors-with-trails.json";
    static final String INVALID_BACKUP_SUFFIX = ".invalid.bak";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ConfigData data = ConfigData.defaults();
    private static Path path;

    private PrettyMeteorsConfig() {
    }

    public static synchronized void load(Path configDirectory) {
        path = configDirectory.resolve(FILE_NAME);
        if (!Files.exists(path)) {
            data = ConfigData.defaults();
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            ConfigData loaded = GSON.fromJson(reader, ConfigData.class);
            if (loaded == null) {
                throw new IOException("Config contained no JSON object");
            }
            data = loaded;
            sanitize();
            save();
        } catch (Exception exception) {
            recoverInvalidConfig(exception);
        }
    }

    public static synchronized Settings snapshot() {
        return new Settings(
            data.nightEventsEnabled,
            data.nightStarCount,
            data.nightNoneChance,
            data.nightSmallChance,
            data.nightMediumChance,
            data.nightLargeChance
        );
    }

    public static synchronized void update(Settings settings) {
        Objects.requireNonNull(settings, "settings");
        validate(settings);
        data = new ConfigData(
            settings.nightEventsEnabled(),
            settings.nightStarCount(),
            settings.nightNoneChance(),
            settings.nightSmallChance(),
            settings.nightMediumChance(),
            settings.nightLargeChance()
        );
        save();
    }

    public static synchronized void save() {
        if (path == null) {
            throw new IllegalStateException("Config must be loaded before it can be saved");
        }
        sanitize();
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(temporary)) {
                GSON.toJson(data, writer);
            }
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException atomicMoveFailure) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            PrettyMeteorsMod.LOGGER.error("Could not save Pretty Meteors config {}", path, exception);
        } finally {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException exception) {
                PrettyMeteorsMod.LOGGER.warn("Could not remove temporary config {}", temporary, exception);
            }
        }
    }

    private static void recoverInvalidConfig(Exception cause) {
        Path backup = path.resolveSibling(path.getFileName() + INVALID_BACKUP_SUFFIX);
        try {
            Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            PrettyMeteorsMod.LOGGER.warn("Backed up invalid Pretty Meteors config {} to {}", path, backup);
        } catch (IOException backupException) {
            cause.addSuppressed(backupException);
            PrettyMeteorsMod.LOGGER.error("Could not back up invalid config {}", path, backupException);
        }
        PrettyMeteorsMod.LOGGER.warn("Pretty Meteors config {} was invalid; restoring defaults", path, cause);
        data = ConfigData.defaults();
        save();
    }

    private static void sanitize() {
        data.nightStarCount = clamp(data.nightStarCount, 0, 20);
        data.nightNoneChance = clamp(data.nightNoneChance, 0, 100);
        data.nightSmallChance = clamp(data.nightSmallChance, 0, 100);
        data.nightMediumChance = clamp(data.nightMediumChance, 0, 100);
        data.nightLargeChance = clamp(data.nightLargeChance, 0, 100);
        if (data.nightNoneChance + data.nightSmallChance + data.nightMediumChance + data.nightLargeChance != 100) {
            data.nightNoneChance = 70;
            data.nightSmallChance = 21;
            data.nightMediumChance = 6;
            data.nightLargeChance = 3;
        }
    }

    private static void validate(Settings settings) {
        if (settings.nightStarCount() < 0 || settings.nightStarCount() > 20) {
            throw new IllegalArgumentException("Nightly shooting stars must be between 0 and 20");
        }
        int[] chances = {
            settings.nightNoneChance(),
            settings.nightSmallChance(),
            settings.nightMediumChance(),
            settings.nightLargeChance()
        };
        for (int chance : chances) {
            if (chance < 0 || chance > 100) {
                throw new IllegalArgumentException("Meteor chances must be between 0 and 100");
            }
        }
        if (settings.chanceTotal() != 100) {
            throw new IllegalArgumentException("Meteor chances must add up to 100");
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class ConfigData {
        private boolean nightEventsEnabled;
        private int nightStarCount;
        private int nightNoneChance;
        private int nightSmallChance;
        private int nightMediumChance;
        private int nightLargeChance;

        private ConfigData() {
            this(true, 5, 70, 21, 6, 3);
        }

        private ConfigData(
            boolean nightEventsEnabled,
            int nightStarCount,
            int nightNoneChance,
            int nightSmallChance,
            int nightMediumChance,
            int nightLargeChance
        ) {
            this.nightEventsEnabled = nightEventsEnabled;
            this.nightStarCount = nightStarCount;
            this.nightNoneChance = nightNoneChance;
            this.nightSmallChance = nightSmallChance;
            this.nightMediumChance = nightMediumChance;
            this.nightLargeChance = nightLargeChance;
        }

        private static ConfigData defaults() {
            return new ConfigData();
        }
    }

    public record Settings(
        boolean nightEventsEnabled,
        int nightStarCount,
        int nightNoneChance,
        int nightSmallChance,
        int nightMediumChance,
        int nightLargeChance
    ) {
        public static Settings defaults() {
            return new Settings(true, 5, 70, 21, 6, 3);
        }

        public int chanceTotal() {
            return nightNoneChance + nightSmallChance + nightMediumChance + nightLargeChance;
        }
    }
}
