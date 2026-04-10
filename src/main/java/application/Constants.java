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

    /**
     * Vertices along one chunk edge: {@code CHUNK_SIZE + 1} so neighboring chunks
     * share a boundary column/row and no one-cell gap appears between meshes.
     */
    public static final int CHUNK_VERTEX_DIM = CHUNK_SIZE + 1;

    /**
     * Perspective far plane (meters). Must exceed farthest loaded terrain from the
     * camera (e.g. render distance × {@link #CHUNK_SIZE} × {@link #WORLD_SCALE}
     * plus margin)
     * or distant chunks clip out while still loaded.
     */
    public static final float CAMERA_FAR_PLANE = 50_000f;
}
