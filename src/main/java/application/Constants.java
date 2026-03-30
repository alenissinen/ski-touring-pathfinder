package application;

/**
 * Application-wide constants, this class can not be initialized. All variables
 * are {@code public static final} and accessed directly.
 */
public class Constants {
    private Constants() {
    }

    /** World scale in meters per grid unit */
    public static final float WORLD_SCALE = 2.0f;

    /** Size of one chunk in grid units */
    public static final int CHUNK_SIZE = 125;
}
