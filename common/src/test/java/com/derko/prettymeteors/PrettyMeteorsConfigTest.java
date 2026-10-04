package com.derko.prettymeteors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PrettyMeteorsConfigTest {
    @TempDir
    Path directory;

    @Test
    void createsDefaultsAndPersistsValidatedUpdates() throws Exception {
        PrettyMeteorsConfig.load(directory);
        assertEquals(PrettyMeteorsConfig.Settings.defaults(), PrettyMeteorsConfig.snapshot());

        PrettyMeteorsConfig.Settings updated = new PrettyMeteorsConfig.Settings(false, 12, 40, 30, 20, 10);
        PrettyMeteorsConfig.update(updated);

        assertEquals(updated, PrettyMeteorsConfig.snapshot());
        String saved = Files.readString(directory.resolve(PrettyMeteorsConfig.FILE_NAME));
        assertTrue(saved.contains("\"nightStarCount\": 12"));
        assertTrue(saved.contains("\"nightLargeChance\": 10"));
    }

    @Test
    void rejectsChanceTotalsThatAreNotOneHundred() {
        PrettyMeteorsConfig.load(directory);

        assertThrows(IllegalArgumentException.class, () -> PrettyMeteorsConfig.update(
            new PrettyMeteorsConfig.Settings(true, 5, 70, 20, 6, 3)
        ));
    }

    @Test
    void repairsOutOfRangeAndInconsistentFileValues() throws Exception {
        Files.writeString(directory.resolve(PrettyMeteorsConfig.FILE_NAME), """
            {
              "nightEventsEnabled": false,
              "nightStarCount": 99,
              "nightNoneChance": 1,
              "nightSmallChance": 2,
              "nightMediumChance": 3,
              "nightLargeChance": 4
            }
            """);

        PrettyMeteorsConfig.load(directory);

        assertEquals(new PrettyMeteorsConfig.Settings(false, 20, 70, 21, 6, 3), PrettyMeteorsConfig.snapshot());
    }

    @Test
    void backsUpInvalidJsonAndRestoresDefaults() throws Exception {
        Path config = directory.resolve(PrettyMeteorsConfig.FILE_NAME);
        Files.writeString(config, "{broken");

        PrettyMeteorsConfig.load(directory);

        assertEquals(PrettyMeteorsConfig.Settings.defaults(), PrettyMeteorsConfig.snapshot());
        assertTrue(Files.exists(config.resolveSibling(config.getFileName() + PrettyMeteorsConfig.INVALID_BACKUP_SUFFIX)));
    }
}
