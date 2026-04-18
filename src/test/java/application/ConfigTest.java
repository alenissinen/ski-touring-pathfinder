package application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

public class ConfigTest {
    @Test
    void build_defaults_returnsCorrectTitle() {
        Config config = new Config.Builder().build();
        assertEquals("Example", config.getTitle());
    }

    @Test
    void build_defaults_returnsCorrectResolution() {
        Config config = new Config.Builder().build();
        assertEquals(1280, config.getWidth());
        assertEquals(720, config.getHeight());
    }

    @Test
    void build_defaults_everyFieldHasValue() {
        Config config = new Config.Builder().build();
        assertNotNull(config.getTitle());
        assertNotNull(config.getFov());
        assertNotNull(config.getHeight());
        assertNull(config.getHeightMap()); // HeightMap should be null by default
        assertNotNull(config.getMovementSpeed());
        assertNotNull(config.getOpenGlMajor());
        assertNotNull(config.getOpenGlMinor());
        assertNotNull(config.getRenderDistance());
        assertNotNull(config.getTargetFps());
        assertNotNull(config.getWidth());
    }

    @Test
    void build_customValue_returnsCorrectValue() {
        Config config = new Config.Builder().width(100).build();
        assertEquals(100, config.getWidth());
    }

    @Test
    void build_chaining_returnsSameBuilder() {
        Config.Builder builder = new Config.Builder();
        assertSame(builder, builder.fov(10f));
    }
}
